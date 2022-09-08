package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 整合mybatis-plus
 * 1.导入依赖
 * 2.配置
 *  2.1 配置数据源
 *      2.1.1导入数据库驱动
 *      2.1.2在application.yaml中配置数据源信息
 *  2.2 配置mubatis-plus
 *      2.2.1 配置mapperScan
 *      2.2.2 在application.yaml配置文件中指定sql映射文件位置
 */

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign")
@MapperScan("com.atguigu.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
