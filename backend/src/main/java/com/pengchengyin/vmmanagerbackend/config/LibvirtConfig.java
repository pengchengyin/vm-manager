package com.pengchengyin.vmmanagerbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Libvirt配置类
 */
@Configuration
@ConfigurationProperties(prefix = "libvirt")
@Data
public class LibvirtConfig {
    private Connection connection = new Connection();

    @Data
    public static class Connection {
        private String uri = "qemu:///system";
        private int timeout = 30;
        /**
         * QEMU 可执行文件路径（可选）
         * 如果未指定，libvirt 将使用系统默认路径
         */
        private String qemuPath;
    }
}


