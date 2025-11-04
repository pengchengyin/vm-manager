package com.pengchengyin.vmmanagerbackend.service;

import com.pengchengyin.vmmanagerbackend.config.LibvirtConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
 * Libvirt连接管理服务
 */
@Slf4j
@Service
public class LibvirtConnectionService {

    @Autowired
    private LibvirtConfig libvirtConfig;

    private Connect connect;

    /**
     * 获取配置的QEMU路径
     */
    public String getQemuPath() {
        return libvirtConfig.getConnection().getQemuPath();
    }

    /**
     * 初始化连接（延迟初始化，不在启动时连接）
     * 注意：libvirt Java API 需要本地库支持，即使使用SSH连接也需要本地libvirt客户端库
     */
    @PostConstruct
    public void init() {
        // 延迟初始化，不在启动时连接
        // 首次调用 getConnection() 时才会建立连接
        log.info("Libvirt连接服务已初始化，URI: {}", libvirtConfig.getConnection().getUri());
        log.warn("注意：libvirt Java API 需要本地libvirt客户端库支持。");
        log.warn("如果使用SSH远程连接（qemu+ssh://），建议在Linux服务器上运行此应用。");
    }

    /**
     * 获取连接（延迟初始化）
     */
    public Connect getConnection() {
        try {
            if (connect == null || !connect.isAlive()) {
                try {
                    log.info("正在连接到libvirt: {}", libvirtConfig.getConnection().getUri());
                    connect = new Connect(libvirtConfig.getConnection().getUri(), false);
                    log.info("成功连接到libvirt");
                    log.info("Hypervisor类型: {}", connect.getType());
                    log.info("Libvirt版本: {}", connect.getLibVersion());
                    log.info("主机名: {}", connect.getHostName());
                } catch (LibvirtException e) {
                    log.error("连接libvirt失败: {}", e.getMessage(), e);
                    throw new RuntimeException("无法连接到libvirt: " + e.getMessage(), e);
                } catch (UnsatisfiedLinkError e) {
                    log.error("无法加载libvirt本地库: {}", e.getMessage(), e);
                    throw new RuntimeException(
                        "无法加载libvirt本地库。libvirt Java API 需要本地libvirt客户端库支持。\n" +
                        "即使使用SSH远程连接（qemu+ssh://），也需要在本地安装libvirt客户端库。\n" +
                        "解决方案：\n" +
                        "1. 在Linux服务器上运行此应用（推荐）\n" +
                        "2. 或在Windows上安装libvirt客户端库（较复杂）\n" +
                        "错误详情: " + e.getMessage(), e);
                }
            }
        } catch (LibvirtException e) {
            throw new RuntimeException("无法连接到libvirt: " + e.getMessage(), e);
        }
        return connect;
    }

    /**
     * 关闭连接
     */
    @PreDestroy
    public void close() {
        if (connect != null) {
            try {
                connect.close();
                log.info("已关闭libvirt连接");
            } catch (LibvirtException e) {
                log.error("关闭libvirt连接失败: {}", e.getMessage(), e);
            }
        }
    }
}


