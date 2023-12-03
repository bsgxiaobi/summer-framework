package com.bishugui.summer.context;

import com.bishugui.summer.annotation.*;
import com.bishugui.summer.exception.BeanCreationException;
import com.bishugui.summer.exception.BeanDefinitionException;
import com.bishugui.summer.exception.BeanNotOfRequiredTypeException;
import com.bishugui.summer.exception.NoUniqueBeanDefinitionException;
import com.bishugui.summer.io.PropertyResolver;
import com.bishugui.summer.io.ResourceResolver;
import com.bishugui.summer.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bi shugui
 * @description 注解配置 应用上下文
 * @date 2023/10/1 17:01
 */
public class AnnotationConfigApplicationContext {
    private static final Logger log = LoggerFactory.getLogger(AnnotationConfigApplicationContext.class);

    protected PropertyResolver propertyResolver;
    protected Map<String, BeanDefinition> beans;


    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的Class类型
        Set<String> scanForClassNameSet = scanForClassNames(configClass);

        // 扫描结果是指定包的所有Class名称，以及通过@Import导入的Class名称
        this.beans = createBeanDefinitionMap(scanForClassNameSet);
    }

    /**
     * 根据beanName查找beanDefinition
     *
     * @param beanName beanName
     * @return BeanDefinition, 不存在则返回null
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String beanName) {
        return this.beans.get(beanName);
    }

    /**
     * 根据Name和Type查找BeanDefinition，如果Name不存在，返回null，如果Name存在，但Type不匹配，抛出异常。
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(),
                    name, def.getBeanClass().getName()));
        }
        return def;
    }

    /**
     * 根据实际类型查若干个beanDefinition;
     * Bean的声明类型与实际类型不一定相符;
     *
     * @param type Class<?>
     * @return 返回0或多个
     */
    public List<BeanDefinition> findBeanDefinitionList(Class<?> type) {
        return this.beans.values().stream()
                // 判断是否是相同类类、父类、接口
                .filter(beanDefinition -> type.isAssignableFrom(beanDefinition.getBeanClass()))
                .sorted().toList();
    }

    /**
     * 根据Type查找某个BeanDefinition，如果不存在返回null，如果存在多个返回@Primary标注的一个:
     *
     * @param type
     * @return
     */
    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> beanDefinitionList = findBeanDefinitionList(type);
        if (beanDefinitionList.isEmpty()) {
            // 没有找到
            return null;
        }
        if (beanDefinitionList.size() == 1) {
            // 只有一个，直接返回
            return beanDefinitionList.get(0);
        }

        // 存在多个，返回@Primary标注的一个
        List<BeanDefinition> primaryBeanList = beanDefinitionList.stream()
                .filter(BeanDefinition::isPrimary).toList();
        if (primaryBeanList.size() == 1) {
            // 只存在一个@Primary标注的bean
            return primaryBeanList.get(0);
        }

        if (primaryBeanList.isEmpty()) {
            // 不存在@Primary
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            // 有多个，@Primary不唯一
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }

    /**
     * 扫描指定包下的所有Class，然后返回Class名字
     *
     * @param configClass
     * @return
     */
    Set<String> scanForClassNames(Class<?> configClass) {
        // 获取@ComponentScan注解
        ComponentScan componentScan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // 获取注解配置的package的名字，未配置则默认当前所在包
        String[] scanPackages = (componentScan == null || componentScan.value().length == 0) ?
                new String[]{configClass.getPackage().getName()} : componentScan.value();

        // 依次扫描所有包
        Set<String> classNameSet = new HashSet<>();
        for (String pkg : scanPackages) {
            log.atDebug().log("Scanning package: {}", pkg);
            ResourceResolver resourceResolver = new ResourceResolver(pkg);
            List<String> classNameList = resourceResolver.scan(res -> {
                // 遇到类则添加，并将其转为Class全名
                String name = res.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            classNameSet.addAll(classNameList);
        }

        // 继续查找@Import(Xyz.class)导入的Class配置
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                classNameSet.add(importConfigClass.getName());
            }
        }

        return classNameSet;
    }

    Map<String, BeanDefinition> createBeanDefinitionMap(Set<String> classNameSet) {
        Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>(classNameSet.size());
        for (String className : classNameSet) {
            Class<?> clazz = null;
            try {
                // class名称转为class对象
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }

            // 如果是注解 枚举类 接口 record则创建
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()){
                continue;
            }
            // 是否标注@Component
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                // 获取Bean的名称
                String beanName = ClassUtils.getBeanName(clazz);
                BeanDefinition beanDefinition = new BeanDefinition(
                        beanName,
                        clazz,
                        getSuitableConstructor(clazz),
                        getOrder(clazz),
                        clazz.isAnnotationPresent(Primary.class),
                        null,
                        null,
                        // 查找@PostConstruct注解的方法
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        // 查找@PreDestroy注解的方法
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class)
                );
                // 检查并添加
                addBeanDefinitions(beanDefinitionMap, beanDefinition);
                // 查找是否有@Configuration:
                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    // 查找@Bean方法:
                    scanFactoryMethods(beanName, clazz, beanDefinitionMap);
                }
            }
        }
        return beanDefinitionMap;
    }

    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * 获取合适的构造函数
     *
     * @param clazz
     * @return
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        // 返回指定参数类型public的构造器
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            // 返回指定参数类型的private和public构造器
            constructors = clazz.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (constructors.length != 1) {
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return constructors[0];
    }

    /**
     * 检查并添加BeanDefinition
     */
    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }


    /**
     * 扫描带有@Bean注释的工厂方法
     *
     * @param factoryBeanName
     * @param clazz
     * @param defs
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        // 获取这个类的所有方法
        for (Method method : clazz.getDeclaredMethods()) {
            // 判断方法上是否有@Bean注解
            Bean bean = method.getAnnotation(Bean.class);
            if(bean == null){
                continue;
            }
            //method.getModifiers() 是Java中的一个方法，用于获取一个方法的修饰符。它返回一个整数值，每个位表示一个特定的修饰符。
            //修饰符是用来描述类、方法、变量等的特性和行为的关键字。常见的修饰符包括public、private、protected、static、final等
            int modifiers = method.getModifiers();
            if (Modifier.isAbstract(modifiers)) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
            }
            if (Modifier.isFinal(modifiers)) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
            }
            if (Modifier.isPrivate(modifiers)) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
            }

            // 获取Bean的返回类型
            Class<?> beanClass = method.getReturnType();
            if (beanClass.isPrimitive()) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
            }
            if (beanClass == void.class || beanClass == Void.class) {
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
            }
            var def = new BeanDefinition(
                    ClassUtils.getBeanName(method),
                    beanClass,
                    factoryBeanName,
                    method,
                    getOrder(method),
                    method.isAnnotationPresent(Primary.class),
                    // init method:
                    bean.initMethod().isEmpty() ? null : bean.initMethod(),
                    // destroy method:
                    bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                    // @PostConstruct / @PreDestroy method:
                    null,
                    null
            );
            addBeanDefinitions(defs, def);
            log.atDebug().log("define bean: {}", def);
        }
    }

}