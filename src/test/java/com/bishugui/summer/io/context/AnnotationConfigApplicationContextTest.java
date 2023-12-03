package com.bishugui.summer.io.context;

import com.bishugui.summer.context.AnnotationConfigApplicationContext;
import com.bishugui.summer.context.BeanDefinition;
import com.bishugui.summer.io.PropertyResolver;
import com.bishugui.summer.io.scanBeanDefinition.ScanBeanDefinitionApplication;
import com.bishugui.summer.io.scanBeanDefinition.annotation.CustomAnnotationBean;
import com.bishugui.summer.io.scanBeanDefinition.destroy.AnnotationDestroyBean;
import com.bishugui.summer.io.scanBeanDefinition.imported.LocalDateConfiguration;
import com.bishugui.summer.io.scanBeanDefinition.imported.ZonedDateConfiguration;
import com.bishugui.summer.io.scanBeanDefinition.nested.OuterBean;
import com.bishugui.summer.io.scanBeanDefinition.primary.PersonBean;
import com.bishugui.summer.io.scanBeanDefinition.primary.StudentBean;
import com.bishugui.summer.io.scanBeanDefinition.primary.TeacherBean;
import com.bishugui.summer.io.scanBeanDefinition.sub1.Sub1;
import com.bishugui.summer.io.scanBeanDefinition.sub1.sub2.Sub2;
import com.bishugui.summer.utils.YamlUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author bi shugui
 * @description 测试 AnnotationConfigApplicationContext
 * @date 2023/10/2 16:39
 */
public class AnnotationConfigApplicationContextTest {

    private PropertyResolver getPropertyResolver(){
        // 先加载基本数据
        Map<String, Object> loadYamlAsPlainMap = YamlUtils.loadYamlAsPlainMap("application.yaml");
        Properties properties = new Properties();
        properties.putAll(loadYamlAsPlainMap);
        return new PropertyResolver(properties);
    }
    /**
     * 测试 创建BeanDefinition
     */
    @Test
    public void testCreateBeanDefinition(){
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanBeanDefinitionApplication.class,getPropertyResolver(),true);

        // component scan 扫描
        assertNotNull(context.findBeanDefinition("sub1"));
        assertNotNull(context.findBeanDefinition(Sub1.class));
        assertNotNull(context.findBeanDefinition("sub2"));
        assertNotNull(context.findBeanDefinition(Sub2.class));

        // import 扫描
        assertNotNull(context.findBeanDefinition("localDateConfiguration"));
        assertNotNull(context.findBeanDefinition(LocalDateConfiguration.class));
        assertNotNull(context.findBeanDefinition("zonedDateConfiguration"));
        assertNotNull(context.findBeanDefinition(ZonedDateConfiguration.class));

        // nested 扫描 嵌套bean
        assertNotNull(context.findBeanDefinition("outerBean"));
        assertNotNull(context.findBeanDefinition("nestedBean"));
        assertNotNull(context.findBeanDefinition(OuterBean.NestedBean.class));

        // 测试继承抽象的bean
        BeanDefinition studentDef = context.findBeanDefinition(StudentBean.class);
        BeanDefinition teacherDef = context.findBeanDefinition(TeacherBean.class);
        List<BeanDefinition> personDefList = context.findBeanDefinitionList(PersonBean.class);
        // 是否指向同一内存地址
        assertSame(studentDef,personDefList.get(0));
        assertSame(teacherDef,personDefList.get(1));

        // 测试@Primary
        assertSame(teacherDef,context.findBeanDefinition(PersonBean.class));

        // 测试自定义注解
        assertNotNull(context.findBeanDefinition(CustomAnnotationBean.class));

        // 测试destroy
        assertNotNull(context.findBeanDefinition(AnnotationDestroyBean.class));

    }

    /**
     * 测试创建Bean实例
     */
    @Test
    public void testCreateBeanInstance(){
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanBeanDefinitionApplication.class,getPropertyResolver());
    }
}
