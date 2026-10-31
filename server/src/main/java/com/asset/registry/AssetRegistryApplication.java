package com.asset.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 互联网资产与信息系统管理平台 · 后端服务入口
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AssetRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetRegistryApplication.class, args);
    }
}
