package com.chareslm.shopping.merchant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * 商家模块文件存储配置；上传目录在存储服务初始化时转为规范化绝对路径。
 */
@ConfigurationProperties(prefix = "app.merchant")
public record MerchantProperties(Path uploadDir) {
}
