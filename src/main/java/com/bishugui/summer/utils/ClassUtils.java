package com.bishugui.summer.utils;

import com.bishugui.summer.annotation.Bean;
import com.bishugui.summer.annotation.Component;
import com.bishugui.summer.exception.BeanDefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author bi shugui
 * @description 类 工具
 * @date 2023/10/1 21:16
 */
public class ClassUtils {

    /**
     * 递归查找Annotation
     *
     * 示例：Annotation A可以直接标注在Class定义:
     *
     * <code>
     * @A
     * public class Hello {}
     * </code>
     *
     * 或者Annotation B标注了A，Class标注了B:
     *
     * <code>
     * &A
     * public @interface B {}
     *
     * @B
     * public class Hello {}
     * </code>
     */
    public static <T extends Annotation> T findAnnotation(Class<?> target, Class<T> annotationClass) {
        T annotation = target.getAnnotation(annotationClass);
        for (Annotation targetAnnotation : target.getAnnotations()) {
            Class<? extends Annotation> targetAnnotationType = targetAnnotation.annotationType();
            // 非java.lang.annotation包下的注解,java自带的注解不扫描
            if(!targetAnnotationType.getPackageName().equals("java.lang.annotation")){
                T sonAnnotation = findAnnotation(targetAnnotationType, annotationClass);
                if(sonAnnotation != null){
                    if(annotation != null){
                        // 情况1：如果注解@A包含@Component,且注解@B也包含@Component；当@A与@B同时标注在类上时
                        // 此时先扫描到了@A中@Component，并将其赋值给了T annotation，又继续扫描@B的@Component，就会抛异常重复添加
                        // 情况2：如果注解@A包含@Component,当@A与@Component同时标注在类上时,会抛异常重复添加
                        // 重复添加注解
                        throw new BeanDefinitionException("Duplicate @" + annotationClass.getSimpleName() + " found on class " + target.getSimpleName());
                    }
                    annotation = sonAnnotation;
                }
            }
        }
        return annotation;
    }

    /**
     * <code>
     * @Bean
     * Hello createHello() {}
     * </code>
     * 获取@Bean注解的对象名称
     * @param method Method
     * @return 名称
     */
    public static String getBeanName(Method method){
        Bean bean = method.getAnnotation(Bean.class);
        String beanBeanName = bean.value();
        if(beanBeanName.isBlank()){
            beanBeanName = method.getName();
        }
        return beanBeanName;
    }

    /**
     * 获取Bean的名称
     *
     * <code>
     * @Component
     * public class Hello {}
     * </code>
     */
    public static String getBeanName(Class<?> clazz){
        String name = "";
        // 查找@Component
        Component component = clazz.getAnnotation(Component.class);
        if(component != null){
            name = component.value();
        }else {
            // 未找到@Component，继续在其他注解中查找@Component:
            for (Annotation anno : clazz.getAnnotations()) {
                if(findAnnotation(anno.annotationType(), Component.class) != null){
                    try {
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        throw new BeanDefinitionException("Cannot get annotation value.", e);
                    }
                }
            }
        }
        if(name.isBlank()){
            // 默认名称: "HelloWorld" => "helloWorld"
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

        }
        return name;
    }

    /**
     * 查找指定注解所在的方法
     * 通过@PostConstruct或@PreDestroy获取非arg方法。不在父类中搜索
     * @param clazz
     * @param annotationType
     * @return
     */
    public static Method findAnnotationMethod(Class<?> clazz,Class<? extends Annotation> annotationType){
        List<Method> methodList = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType))
                .map(method -> {
                    if (method.getParameterCount() != 0) {
                        throw new BeanDefinitionException(
                                String.format("Method '%s' with @%s must not have argument: %s", method.getName(), annotationType.getSimpleName(), clazz.getName()));
                    }
                    return method;
                }).toList();

        if(methodList.isEmpty()){
            return null;
        }

        if(methodList.size() > 1){
            throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s", annotationType.getSimpleName(), clazz.getName()));
        }

        return methodList.get(0);
    }
}
