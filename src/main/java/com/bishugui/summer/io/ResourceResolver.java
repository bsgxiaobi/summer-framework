package com.bishugui.summer.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author bi shugui
 * @description 资源解析器，一个简单的类路径扫描在目录和jar中都有效
 * @date 2023/9/17 23:30
 */
public class ResourceResolver {
    Logger log = LoggerFactory.getLogger(getClass());

    String basePackage;

    public ResourceResolver() {}

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    public <R> List<R> scan(Function<Resource,R> mapper){
        String basePackagePath = this.basePackage.replace(".", "/");
        String path = basePackagePath;
        try {
            List<R> collector = new ArrayList<>();
            scan(basePackagePath,path,collector,mapper);
            return collector;
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }catch (URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    <R> void scan(String basePackagePath,String path,List<R> collector,Function<Resource,R> mapper) throws IOException, URISyntaxException {
        log.atDebug().log("扫描路径： {}",path);
        // 通过ClassLoader获取URL列表:
        Enumeration<URL> en = getContextClassLoader().getResources(path);
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uri.toString());
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if (uri.toString().startsWith("file:")) {
                // 如果是文件，则先去除“file:”
                uriBaseStr = uriBaseStr.substring(5);
                // 在目录中搜索
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
                continue;
            }
            if (uri.toString().startsWith("jar:")) {
                // 在Jar包中搜索
                scanFile(true,uriBaseStr,jarUriToPath(basePackagePath, uri),collector,mapper);
            }
        }
    }

    /**
     * jar包的Uri转为Path
     * @param basePackagePath
     * @param jarUri
     * @return
     * @throws IOException
     */
    Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        return FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
    }


    /**
     * 获取上下文类加载器
     * @return
     */
    ClassLoader getContextClassLoader() {
        //首先从Thread.getContextClassLoader()获取，如果获取不到，再从当前Class获取，因为Web应用的ClassLoader不是JVM提供的基于Classpath的
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if(contextClassLoader == null){
            contextClassLoader = getClass().getClassLoader();
        }
        return contextClassLoader;
    }

    <R> void scanFile(boolean jsJar, String basePackagePath, Path root, List<R> collector, Function<Resource,R> mapper) throws IOException {
        //basePackagePath = removeTrailingSlash(basePackagePath);
        // 遍历目录下所有标准文件
        Files.walk(root).filter(Files::isRegularFile).forEach(file->{
            Resource resource = null;
            if (jsJar) {
                // 如果是jar包，则直接创建资源，basePackagePath=jar:xxx
                resource = new Resource(basePackagePath,removeLeadingSlash(file.toString()));
            }else{
                // 如果是文件，则
                String filePath = file.toString();
                // 提取出文件名
                String name = removeLeadingSlash(filePath.substring(basePackagePath.length()));
                resource = new Resource("file:" + filePath,name);
                log.atDebug().log("找到资源: {}", resource);
            }
            R r = mapper.apply(resource);
            if(r != null){
                collector.add(r);
            }
        });
    }


    /**
     * 删除尾部斜线
     * @param str
     * @return
     */
    private String removeTrailingSlash(String str) {
        if (str.endsWith("/") || str.endsWith("\\")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * 删除头部斜线
     * @param str
     * @return
     */
    String removeLeadingSlash(String str) {
        if (str.startsWith("/") || str.startsWith("\\")) {
            str = str.substring(1);
        }
        return str;
    }
}
