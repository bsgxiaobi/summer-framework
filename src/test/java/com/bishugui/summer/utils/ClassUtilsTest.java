package com.bishugui.summer.utils;

import com.bishugui.summer.annotation.Component;
import com.bishugui.summer.annotation.Configuration;
import com.bishugui.summer.exception.BeanDefinitionException;
import com.bishugui.summer.io.scanBeanDefinition.sub1.Sub1;
import com.bishugui.summer.io.scanBeanDefinition.sub1.sub2.Sub2;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author bi shugui
 * @description 类工具 测试
 * @date 2023/10/2 15:40
 */
public class ClassUtilsTest {
    @Test
    public void noComponent() {
        Component component = ClassUtils.findAnnotation(DisplayNameGenerator.Simple.class, Component.class);
        assertNull(component);
    }

    @Test
    public void hasComponent() {
        Component component = ClassUtils.findAnnotation(Sub1.class, Component.class);
        assertNotNull(component);
    }

    @Test
    public void hasComponentSuper() {
        Component component = ClassUtils.findAnnotation(Sub2.class, Component.class);
        assertNotNull(component);
    }

    @Test
    public void getBeanName(){
        assertEquals("sub1",ClassUtils.getBeanName(Sub1.class));
    }

    @Test
    public void getBeanNameWithName(){
        assertEquals("thisBeanNameWithName",ClassUtils.getBeanName(BeanNameWithName.class));

        assertEquals("testCustomComponent",ClassUtils.getBeanName(CustomWithName.class));

        assertEquals("testConfigurationWithName",ClassUtils.getBeanName(ConfigurationWithName.class));
    }

    @Test
    public void duplicateComponent(){
        assertThrows(BeanDefinitionException.class,()->ClassUtils.findAnnotation(DuplicateComponent.class, Component.class));
        assertThrows(BeanDefinitionException.class,()->ClassUtils.findAnnotation(DuplicateComponent2.class, Component.class));
    }

}

@Configuration
@Component
class DuplicateComponent {
}

@Component("thisBeanNameWithName")
class BeanNameWithName{

}

@Configuration
@CustomComponent
class DuplicateComponent2 {
}

@Configuration("testConfigurationWithName")
class ConfigurationWithName{

}

@CustomComponent("testCustomComponent")
class CustomWithName{

}