package com.bishugui.summer.io;

import jakarta.annotation.Nullable;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * @author bi shugui
 * @description 属性解析器
 * @date 2023/9/27 21:01
 */
public class PropertyResolver {
    Map<String,String> propertyMap = new HashMap<>();

    Map<Class<?>, Function<String,Object>> converterMap = new HashMap<>();

    /**
     * 构造方法
     * @param properties Properties
     */
    public PropertyResolver(Properties properties){
        this.converterMap.put(String.class,s->s);
        this.converterMap.put(Boolean.class,Boolean::valueOf);
        this.converterMap.put(boolean.class,Boolean::valueOf);
        this.converterMap.put(Integer.class,Integer::valueOf);
        this.converterMap.put(int.class,Integer::valueOf);
        this.converterMap.put(Long.class,Long::valueOf);
        this.converterMap.put(long.class,Long::valueOf);
        this.converterMap.put(Double.class,Double::valueOf);
        this.converterMap.put(double.class,Double::valueOf);
        this.converterMap.put(Float.class,Float::valueOf);
        this.converterMap.put(float.class,Float::valueOf);
        this.converterMap.put(Short.class,Short::valueOf);
        this.converterMap.put(short.class,Short::valueOf);

        converterMap.put(LocalDate.class,LocalDate::parse);
        converterMap.put(LocalDateTime.class,LocalDateTime::parse);
        converterMap.put(LocalTime.class,LocalTime::parse);
        converterMap.put(ZonedDateTime.class,ZonedDateTime::parse);
        converterMap.put(OffsetDateTime.class,OffsetDateTime::parse);
        converterMap.put(OffsetTime.class,OffsetTime::parse);
        converterMap.put(Duration.class,Duration::parse);
        converterMap.put(ZoneId.class,ZoneId::of);

        // 存入环境变量
        this.propertyMap.putAll(System.getenv());

        // 存入Properties
        properties.stringPropertyNames().forEach(name-> this.propertyMap.put(name,properties.getProperty(name)));
    }

    public boolean containsProperty(String key){
        return this.propertyMap.containsKey(key);
    }

    /**
     * 获取属性
     * @param key 表达式/键值
     * @return 属性值
     */
    @Nullable
    public String getProperty(String key){
        // 先解析表达式
        PropertyExpr propertyExpr = parsePropertyExpr(key);
        if(propertyExpr == null){
            // 没有使用表达式${}
            String value = this.propertyMap.get(key);
            return parseValue(value);
        }

        // 使用了表达式
        if(propertyExpr.defaultValue() != null){
            // 存在默认值
            return getProperty(propertyExpr.key(),propertyExpr.defaultValue());
        }else{
            return getRequiredProperty(propertyExpr.key());
        }
    }

    @Nullable
    public String getProperty(String key,@Nullable String defaultValue){
        String value = this.propertyMap.get(key);
        return value == null ? parseValue(defaultValue) : value;
    }


    @Nullable
    public <T> T getProperty(String key,Class<T> targetClass){
        String value = this.propertyMap.get(key);
        if(value == null){
            return null;
        }
        return convert(targetClass,value);
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    private <T> T convert(Class<T> targetClass, String value) {
        Function<String, Object> stringObjectFunction = converterMap.get(targetClass);
        if(stringObjectFunction == null){
            throw new IllegalArgumentException(targetClass+" not support convert");
        }
        return (T)stringObjectFunction.apply(value);
    }

    /**
     * 解析值，目的是实现 值中嵌套表达式
     * @param value
     * @return
     */
    String parseValue(String value){
        // 先解析表达式
        PropertyExpr propertyExpr = parsePropertyExpr(value);
        if(propertyExpr == null){
            return value;
        }

        if(propertyExpr.defaultValue() != null){
            return getProperty(propertyExpr.key(),propertyExpr.defaultValue());
        }else{
            return getRequiredProperty(propertyExpr.key());
        }

    }

    /**
     * 解析属性表达式
     * @param key 键值
     * @return PropertyExpr
     */
    @Nullable
    PropertyExpr parsePropertyExpr(String key){
        if(key == null ||  key.isEmpty()){
            return null;
        }
        if(!key.startsWith("${") || !key.endsWith("}")){
            return null;
        }
        // ${}表达式
        // 是否存在默认值
        int colonIndex = key.indexOf(":");
        if(colonIndex == (-1)){
            // 没有默认值
            return new PropertyExpr(key.substring(2,key.length()-1),null);
        }
        // 有默认值,先截取出key,然后将defaultValue再次解析，以实现嵌套表达式
        String keyStr = key.substring(2,colonIndex);
        String defaultValue = key.substring(colonIndex+1,key.length()-1);
        return new PropertyExpr(keyStr,parsePropertyExpr(defaultValue).defaultValue());
    }
}
