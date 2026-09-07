package com.cloude.shop.portal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 前台商城服务启动类
 */
@Slf4j
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.cloude.shop")
public class PortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortalApplication.class, args);
        log.info("Cloude Shop 前台商城服务启动完成 -> http://localhost:8080/portal-api/doc.html");
    }
}
