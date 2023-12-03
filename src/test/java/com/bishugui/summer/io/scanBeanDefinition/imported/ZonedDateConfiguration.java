package com.bishugui.summer.io.scanBeanDefinition.imported;

import com.bishugui.summer.annotation.Bean;
import com.bishugui.summer.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * @author bi shugui
 * @description 时间配置
 * @date 2023/10/5 17:32
 */
@Configuration
public class ZonedDateConfiguration {
    private static final Logger log = LoggerFactory.getLogger(LocalDateConfiguration.class);

    @Bean
    public ZonedDateTime startZoneLocalDate() {
        log.atDebug().log("@Bean startZoneLocalDate");
        return ZonedDateTime.now();
    }
}
