package com.smart.sso.server.config;

import com.smart.sso.server.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
@Slf4j
public class MyCommandLineRunner implements CommandLineRunner {

//    @Autowired
    private ConfigService configService;
    @Override
    public void run(String... args) {
        log.error("客户配置初始化");
//        configService.refreshCustomerConfig();
    }
}
