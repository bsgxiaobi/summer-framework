package com.bishugui.summer.utils;

import jdk.jshell.spi.ExecutionControl;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bi shugui
 * @description yaml工具
 * @date 2023/9/27 21:46
 */
public class YamlUtils {
    /**
     * 加载yaml文件为map
     * @param path 路径
     * @return Map<String,Object>，key为yaml中首字段，value为yaml中首字段的值(可能是嵌套)
     */
    public static Map<String,Object> loadYaml(String path){
        var loaderOptions = new LoaderOptions();
        var dumperOptions = new DumperOptions();
        var rePresenter = new Representer(dumperOptions);
        var resolver = new NoImplicitResolver();
        Yaml yaml = new Yaml(new Constructor(loaderOptions), rePresenter, dumperOptions, loaderOptions, resolver);
        return ClassPathUtils.readInputStream(path,(input)->{
            return (Map<String,Object>)yaml.load(input);
        });
    }

    /**
     * 将yaml转为plain map,与properties文件类似
     * @param path
     * @return
     */
    public static Map<String,Object> loadYamlAsPlainMap(String path){
        Map<String, Object> dataMap = loadYaml(path);
        Map<String, Object> plainMap = new LinkedHashMap<>(dataMap.size());
        convertTo(dataMap, "", plainMap);
        return plainMap;
    }

    /**
     * 将yaml的树形map,转为plain map,与properties文件类似
     * @param sourceMap
     * @param prefix
     * @param plainMap
     */
    static void convertTo(Map<String, Object> sourceMap,String prefix,Map<String, Object> plainMap){
        for (String key : sourceMap.keySet()) {
            Object value = sourceMap.get(key);
            if (value instanceof Map) {
                // 如果是map，则递归解析出子级
                Map<String, Object> subMap = (Map<String, Object>) value;
                convertTo(subMap, prefix + key + ".", plainMap);
            } else if (value instanceof List) {
                plainMap.put(prefix + key, value);
            } else{
                plainMap.put(prefix + key, value.toString());
            }
        }
    }

}

/**
 * Disable ALL implicit convert and treat all values as string.
 */
class NoImplicitResolver extends Resolver {

    public NoImplicitResolver() {
        super();
        super.yamlImplicitResolvers.clear();
    }
}