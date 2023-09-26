package com.bishugui.summer.io;

import com.bishugui.summer.io.ResourceResolver;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * @author bi shugui
 * @description 资源解析器测试
 * @date 2023/9/24 21:26
 */
public class ResourceResolverTest {
    Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 扫描指定目录下的class
     */
    @Test
    public void scanClass(){
        String pkgPath = "com.bishugui.summer.io.scanPackage";
        ResourceResolver resourceResolver = new ResourceResolver(pkgPath);
        List<String> classesNameList = resourceResolver.scan(resource -> {
            String name = resource.name();
            if (name.endsWith(".class")) {
                return name;
            }
            return null;
        });
        log.info("scanClass,size:{} result: {}",classesNameList.size(),classesNameList);

    }

    /**
     * 扫描jar包
     */
    @Test
    public void scanJar(){
        var packageName = PostConstruct.class.getPackageName();
        ResourceResolver resourceResolver = new ResourceResolver(packageName);
        List<String> classesNameList = resourceResolver.scan(resource -> {
            String name = resource.name();
            if (name.endsWith(".class")) {
                return name;
            }
            return null;
        });
        log.info("scanJar,size:{} result : {}",classesNameList.size(),classesNameList);
        assertTrue(classesNameList.contains("jakarta/annotation/Nullable.class"));
        assertTrue(classesNameList.contains("jakarta/annotation/Nullable1.class"));
    }

    /**
     * 扫描resources路径下的文件
     * 运行时resources下的文件未打包进target?????
     */
    @Test
    public void scanTxt(){
        var packageName = "scanTxt";
        ResourceResolver resourceResolver = new ResourceResolver(packageName);
        List<String> classesNameList = resourceResolver.scan(resource -> {
            String name = resource.name();
            if (name.endsWith(".txt")) {
                return name;
            }
            return null;
        });
        log.info("scanTxt,size:{} result : {}",classesNameList.size(),classesNameList);
    }
}
