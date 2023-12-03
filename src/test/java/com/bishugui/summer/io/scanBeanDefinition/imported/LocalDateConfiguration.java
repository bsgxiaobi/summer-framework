package com.bishugui.summer.io.scanBeanDefinition.imported;

import com.bishugui.summer.annotation.Bean;
import com.bishugui.summer.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author bi shugui
 * @description 本地时间配置
 * @date 2023/10/5 17:29
 */
@Configuration
public class LocalDateConfiguration {
    private static final Logger log = LoggerFactory.getLogger(LocalDateConfiguration.class);

    @Bean
    public LocalDate startLocalDate() {
        log.atDebug().log("@Bean startLocalDate");
        return LocalDate.now();
    }

    @Bean
    public LocalDateTime startLocalDateTime() {
        log.atDebug().log("@Bean startLocalDateTime");
        return LocalDateTime.now();
    }
}
