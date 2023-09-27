package com.bishugui.summer.utils;

import com.bishugui.summer.io.InputStreamCallback;
import com.bishugui.summer.io.ResourceResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * @author bi shugui
 * @description 类路径工具
 * @date 2023/9/27 21:53
 */
public class ClassPathUtils {
    public static <T> T readInputStream(String path, InputStreamCallback<T> inputStreamCallback) {
        // 如果是”/“开头，则去掉
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try(InputStream input = getContextClassLoader().getResourceAsStream(path)) {
            if(input == null) {
                throw new FileNotFoundException("File not found in classpath: " + path);
            }
            return inputStreamCallback.doWithInputStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 获取上下文类加载器
     * @return
     */
    static ClassLoader getContextClassLoader() {
        ClassLoader contextClassLoader = null;
        //首先从Thread.getContextClassLoader()获取，如果获取不到，再从当前Class获取，因为Web应用的ClassLoader不是JVM提供的基于Classpath的
        contextClassLoader = Thread.currentThread().getContextClassLoader();
        if(contextClassLoader == null){
            contextClassLoader = ClassPathUtils.class.getClassLoader();
        }
        return contextClassLoader;
    }
}
