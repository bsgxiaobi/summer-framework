package com.bishugui.summer.io.scanBeanDefinition.primary;

import com.bishugui.summer.annotation.Bean;
import com.bishugui.summer.annotation.Configuration;
import com.bishugui.summer.annotation.Primary;

/**
 * @author bi shugui
 * @description 测试primary配置
 * @date 2023/10/5 17:48
 */
@Configuration
public class PrimaryConfiguration {

    @Primary
    @Bean
    DogBean husky() {
        return new DogBean("Husky");
    }

    @Bean
    DogBean teddy() {
        return new DogBean("Teddy");
    }
}
