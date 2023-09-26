package com.bishugui.summer;

import java.net.URL;

/**
* @description 主类
* @author bi shugui
* @date 2023/9/26 23:02
*/
public class SummerFrameworkApplication {
    public static void main(String[] args) {
        URL resource = ClassLoader.getSystemResource("application.yaml");
        System.out.println("Hello World!");
    }
}
