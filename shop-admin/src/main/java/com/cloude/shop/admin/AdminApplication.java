package com.cloude.shop.admin;

import com.cloude.shop.service.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后台管理服务启动类
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.cloude.shop")
@RequiredArgsConstructor
public class AdminApplication implements ApplicationRunner {

    private final AdminService adminService;

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
        log.info("Cloude Shop 后台管理服务启动完成 -> http://localhost:8081/admin-api/doc.html");
    }

    @Override
    public void run(ApplicationArguments args) {
        adminService.initDefaultAdmin();
    }
}
