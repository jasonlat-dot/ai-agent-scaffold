package com.jasonlat.ai.infrastructure.generator;

/**
 * @Author: jia-qiang ljq1024.cc
 * @desc:
 * @Date: 2024-04-10-21:54
 */
public class Test {
    public static void main(String[] args) {
        String path = System.getProperty("user.dir") + "\\" + "ai-agent-scaffold-code-infrastructure\\src\\main\\java";
        System.out.println(path);

        String mapperXmlPath =
                System.getProperty("user.dir") + "\\src\\main\\resources\\mybatis\\mapper";

        System.out.println(mapperXmlPath);
    }
}
