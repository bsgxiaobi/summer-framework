package com.bishugui.summer.io.scanBeanDefinition;

import com.bishugui.summer.annotation.ComponentScan;
import com.bishugui.summer.annotation.Import;
import com.bishugui.summer.io.scanBeanDefinition.imported.LocalDateConfiguration;
import com.bishugui.summer.io.scanBeanDefinition.imported.ZonedDateConfiguration;

/**
 * @author bi shugui
 * @description 模拟主类
 * @date 2023/10/2 16:44
 */
@ComponentScan
@Import({LocalDateConfiguration.class, ZonedDateConfiguration.class})
public class ScanBeanDefinitionApplication {
}
