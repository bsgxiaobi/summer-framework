package com.bishugui.summer.context;

import com.bishugui.summer.annotation.*;
import com.bishugui.summer.exception.*;
import com.bishugui.summer.io.PropertyResolver;
import com.bishugui.summer.io.ResourceResolver;
import com.bishugui.summer.utils.ClassUtils;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bi shugui
 * @description 注解配置 应用上下文
 * @date 2023/10/1 17:01
 */
public class AnnotationConfigApplicationContext {
    private static final Logger log = LoggerFactory.getLogger(AnnotationConfigApplicationContext.class);

    /**
     * 属性解析器
     */
    protected PropertyResolver propertyResolver;

    /**
     * 所有BeanDefinition,Map<BeanName, BeanDefinition>
     */
    protected Map<String, BeanDefinition> beans;


    /**
     * 创建Bean的名字,用于检测循环依赖
     */
    private Set<String> createBeanNameSet;

    /**
     * 扫描并创建所有Bean
     * 实现IoC容器-创建BeanDefinition
     * 实现IoC容器-创建Bean示例
     * @param configClass 被扫描的启动类
     * @param propertyResolver 属性解析器
     */
    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的Class类型
        Set<String> scanForClassNameSet = scanForClassNames(configClass);

        // 扫描结果是指定包的所有Class名称，以及通过@Import导入的Class名称
        this.beans = createBeanDefinitionMap(scanForClassNameSet);

        // 创建BeanName检测循环依赖
        this.createBeanNameSet = new HashSet<>(beans.size());

        // 创建@Configuration类型的Bean实例
        // 由于@Configuration标识的Bean实际上是工厂，它们必须先实例化，才能实例化其他普通Bean，所以我们先把@Configuration标识的Bean创建出来，再创建普通Bean
        this.beans.values().stream()
                .filter(this::isConfigurationDefinition).map(item->{
                    createBeanAsEarlySingleton(item);
                    return item.getName();
                }).toList();

        // 创建其他普通的Bean实例
        createNormalBeans();

        if (log.isDebugEnabled()) {
            this.beans.values().stream().sorted().forEach(def -> {
                log.debug("bean初始化; bean initialized: {}", def);
            });
        }
    }
    /**
     * 扫描并创建所有Bean
     * 实现IoC容器-创建BeanDefinition
     * @param configClass 被扫描的启动类
     * @param propertyResolver 属性解析器
     */
    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver,boolean beanDefinition) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的Class类型
        Set<String> scanForClassNameSet = scanForClassNames(configClass);

        // 扫描结果是指定包的所有Class名称，以及通过@Import导入的Class名称
        this.beans = createBeanDefinitionMap(scanForClassNameSet);
    }

    void createNormalBeans(){
        // 获取还没有实例的BeanDefinition列表
        List<BeanDefinition> beanDefinitionList = this.beans.values().stream()
                .filter(beanDefinition -> beanDefinition.getInstance() == null).sorted().toList();
        beanDefinitionList.forEach(item->{
            // 如果Bean未被创建(可能在其他Bean的构造方法注入前被创建)
            if(item.getInstance() == null){
                createBeanAsEarlySingleton(item);
            }
        });
    }

    /**
     * 创建一个Bean，但不进行字段和方法级别的注入。如果创建的Bean不是Configuration，则在构造方法中注入的依赖Bean会自动创建。
     * @param beanDefinition BeanDefinition
     * @return
     */
    public Object createBeanAsEarlySingleton(BeanDefinition beanDefinition){
        log.atDebug().log("Try create bean '{}' as early singleton: {}", beanDefinition.getName(), beanDefinition.getBeanClass().getName());
        // 如果名字已经存在，则认为触发了循环依赖
        if(!this.createBeanNameSet.add(beanDefinition.getName())){
            throw new UnsatisfiedDependencyException(String.format("触发了循环依赖; Circular dependency detected when create bean '%s'", beanDefinition.getName()));
        }

        //创建方式：构造方法或工厂方法
        Executable createFun = null;
        if(beanDefinition.getFactoryName() != null){
            // 使用工厂方法创建
            createFun = beanDefinition.getFactoryMethod();
        }else{
            // 工厂方法名字为空，则用构造方法创建
            createFun = beanDefinition.getConstructor();
        }

        // 创建参数
        final Parameter[] parameters = createFun.getParameters();
        final Annotation[][] parameterAnnotations = createFun.getParameterAnnotations();
        Object[] args =  new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            // 从参数获取@Value和@Autowired
            final Parameter parameter = parameters[i];
            final Annotation[] parameterAnnotation = parameterAnnotations[i];
            final Value value = ClassUtils.getAnnotation(parameterAnnotation, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(parameterAnnotation, Autowired.class);

            // @Configuration是工厂方法，不允许使用@Autowired创建
            boolean isConfiguration = isConfigurationDefinition(beanDefinition);
            if(isConfiguration && autowired != null){
                throw new BeanCreationException(
                        String.format("@Configuration是工厂方法，不允许使用@Autowired创建; Cannot specify @Autowired when create @Configuration bean '%s': %s.", beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }

            // 参数只能是@Value、@Autowired其中之一
            if(value == null && autowired == null){
                throw new BeanCreationException(
                        String.format("参数只能是@Value、@Autowired其中之一; Must specify @Autowired or @Value when create bean '%s': %s.", beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }
            if(value != null && autowired != null){
                throw new BeanCreationException(
                        String.format("参数只能是@Value、@Autowired其中之一; Cannot specify both @Autowired and @Value when create bean '%s': %s.", beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }

            // 参数类型
            final Class<?> type = parameter.getType();
            if(type == null){
                throw new BeanCreationException("参数类型为空，但不是; Parameter type is null");
            }
            if(value != null){
                // 参数是@Value
                args[i] = this.propertyResolver.getRequiredProperty(value.value(),type);
            }else{
                // 参数是@Autowired
                String name = autowired.name();
                boolean required = autowired.value();
                // 依赖的BeanDefinition
                BeanDefinition dependBeanDefinition = name.isBlank() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                // 如果是必须的，但依赖的BeanDefinition是空
                if(required && dependBeanDefinition == null){
                    throw new BeanCreationException(String.format("@Autowired是必须的但没找到依赖的Bean; Missing autowired bean with type '%s' when create bean '%s': %s.",
                            type.getName(), beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
                }
                if(dependBeanDefinition != null){
                    // 获取到了依赖bean的实例化对象
                    Object autowiredBeanInstance = dependBeanDefinition.getInstance();
                    if(autowiredBeanInstance == null && !isConfiguration){
                        // 当前依赖Bean尚未初始化，递归调用初始化该依赖Bean
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependBeanDefinition);
                    }
                    args[i] = autowiredBeanInstance;
                }else{
                    // @Autowired依赖bean不是必须，且未获取到bean
                    args[i] = null;
                }
            }
        }

        // 创建Bean示例
        Object instance = null;
        if(beanDefinition.getFactoryName() != null){
            // 有工厂方法时，用@Bean方法创建
            Object configInstance = getBean(beanDefinition.getFactoryName());
            try {
                instance = beanDefinition.getFactoryMethod().invoke(configInstance,args);
            }catch (Exception e){
                throw new BeanCreationException(String.format("用@Bean方法创建Bean是发生异常; Exception when create bean '%s': %s",
                        beanDefinition.getName(), beanDefinition.getBeanClass().getName()), e);
            }
        }else{
            // 无工厂方法时，用构造方法创建
            try {
                instance = beanDefinition.getConstructor().newInstance(args);
            }catch (Exception e){
                throw new BeanCreationException(String.format("用构造方法创建Bean是发生异常; Exception when create bean '%s': %s",
                        beanDefinition.getName(), beanDefinition.getBeanClass().getName()), e);
            }
        }
        beanDefinition.setInstance(instance);
        return beanDefinition.getInstance();
    }
    /**
     * 判断是否是@Configuration标识的BeanDefinition
     * @param beanDefinition BeanDefinition
     * @return 是：true
     */
    public boolean isConfigurationDefinition(BeanDefinition beanDefinition) {
        return ClassUtils.findAnnotation(beanDefinition.getBeanClass(),Configuration.class) != null;
    }


    /**
     * 通过Name查找Bean，不存在时抛出NoSuchBeanDefinitionException
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过Name和Type查找Bean，不存在抛出NoSuchBeanDefinitionException，存在但与Type不匹配抛出BeanNotOfRequiredTypeException
     */
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if (t == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType));
        }
        return t;
    }

    /**
     * 通过Type查找Beans
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> defs = findBeanDefinitionList(requiredType);
        if (defs.isEmpty()) {
            return List.of();
        }
        List<T> list = new ArrayList<>(defs.size());
        for (var def : defs) {
            list.add((T) def.getRequiredInstance());
        }
        return list;
    }

    /**
     * 通过Type查找Bean，不存在抛出NoSuchBeanDefinitionException，存在多个但缺少唯一@Primary标注抛出NoUniqueBeanDefinitionException
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }


    // findXxx与getXxx类似，但不存在时返回null

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefinitionList(requiredType).stream().map(def -> (T) def.getRequiredInstance()).collect(Collectors.toList());
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
            throw new BeanNotOfRequiredTypeException(String.format("Name存在，但Type不匹配; Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(),
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
            throw new NoUniqueBeanDefinitionException(String.format("不存在@Primary; Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            // 有多个，@Primary不唯一
            throw new NoUniqueBeanDefinitionException(String.format("@Primary有多个,不唯一; Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
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
                throw new BeanDefinitionException("构造函数不唯一; More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (constructors.length != 1) {
            throw new BeanDefinitionException("构造函数不唯一; More than one public constructor found in class " + clazz.getName() + ".");
        }
        return constructors[0];
    }

    /**
     * 检查并添加BeanDefinition
     */
    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("beanName重复; Duplicate bean name: " + def.getName());
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
                throw new BeanDefinitionException("@Bean不能作用在abstract方法; @Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
            }
            if (Modifier.isFinal(modifiers)) {
                throw new BeanDefinitionException("@Bean不能作用在final方法; @Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
            }
            if (Modifier.isPrivate(modifiers)) {
                throw new BeanDefinitionException("@Bean不能作用在private方法; @Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
            }

            // 获取Bean的返回类型
            Class<?> beanClass = method.getReturnType();
            if (beanClass.isPrimitive()) {
                throw new BeanDefinitionException("@Bean的方法返回值不能是基本类型; @Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
            }
            if (beanClass == void.class || beanClass == Void.class) {
                throw new BeanDefinitionException("@Bean的方法返回值不能是空; @Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
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