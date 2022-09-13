package com.atguigu.gulimall.wave;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableTransactionManagement
@MapperScan("com.atguigu.gulimall.wave.dao")
public class GuliMallWaveApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuliMallWaveApplication.class, args);
    }

}
