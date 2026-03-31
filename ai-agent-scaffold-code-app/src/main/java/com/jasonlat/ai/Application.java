
package com.jasonlat.ai;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configurable
@EnableScheduling
@SpringBootApplication
@MapperScan("com.jasonlat.ai.infrastructure.dao")
@ComponentScan(basePackages = {"com.jasonlat", "cc.jq1024.middleware"})
public class Application {

    public static void main(String[] args){
        SpringApplication.run(Application.class);
        log.info("项目开始启动...");
    }

}
