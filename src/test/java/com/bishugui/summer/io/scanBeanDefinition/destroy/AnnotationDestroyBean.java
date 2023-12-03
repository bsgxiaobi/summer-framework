package com.bishugui.summer.io.scanBeanDefinition.destroy;

import com.bishugui.summer.annotation.Component;
import com.bishugui.summer.annotation.Value;
import jakarta.annotation.PreDestroy;

/**
 * @author bi shugui
 * @description 测试使用@PreDestroy注解
 * @date 2023/10/5 18:44
 */
@Component
public class AnnotationDestroyBean {

    @Value("${app.title}")
    private String title;

    @PreDestroy
    public void destroy() {
        System.out.println("destroy title : " + title);
    }
}
