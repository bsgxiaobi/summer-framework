package com.bishugui.summer.utils;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlUtilsTest {
    static final Logger log = LoggerFactory.getLogger(YamlUtilsTest.class);

    @Test
    void loadYaml() {
        Map<String, Object> yamlMap = YamlUtils.loadYaml("application.yaml");
        yamlMap.keySet().forEach(key ->{
            Object obj = yamlMap.get(key);
            System.out.println(key + ":" + obj + ",,," + obj.getClass());
            //log.info("{}:{},({})", key, obj,obj.getClass());
        });
    }

    @Test
    void loadYamlAsPlainMap() {
        Map<String, Object> yamlToPlainMap = YamlUtils.loadYamlAsPlainMap("application.yaml");
        yamlToPlainMap.keySet().forEach(key ->{
            Object obj = yamlToPlainMap.get(key);
            System.out.println(key + ":" + obj + ",,," + obj.getClass());
            //log.info("{}:{},({})", key, obj,obj.getClass());
        });
    }

}