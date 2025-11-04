package com.pengchengyin.vmmanagerbackend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 虚拟机信息
 */
@Schema(description = "虚拟机详细信息")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmInfo {
    /**
     * 虚拟机名称
     */
    @Schema(description = "虚拟机名称", example = "my-vm")
    private String name;

    /**
     * 虚拟机UUID
     */
    @Schema(description = "虚拟机UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String uuid;

    /**
     * 状态
     */
    @Schema(description = "虚拟机状态", example = "RUNNING")
    private VmStatus status;

    /**
     * 状态描述
     */
    @Schema(description = "状态描述", example = "运行中")
    private String statusDescription;

    /**
     * 最大内存（字节）
     */
    @Schema(description = "最大内存（字节）", example = "2147483648")
    private long maxMemory;

    /**
     * 当前内存（字节）
     */
    @Schema(description = "当前内存（字节）", example = "2147483648")
    private long currentMemory;

    /**
     * CPU数量
     */
    @Schema(description = "CPU核心数", example = "2")
    private int cpuCount;

    /**
     * CPU使用率（百分比）
     */
    @Schema(description = "CPU使用率（百分比）", example = "25.5")
    private double cpuUsage;

    /**
     * 运行时间（秒）
     */
    @Schema(description = "运行时间（秒）", example = "3600")
    private long runTime;

    /**
     * 是否持久化
     */
    @Schema(description = "是否持久化", example = "true")
    private boolean persistent;

    /**
     * 网络接口列表
     */
    @Schema(description = "网络接口列表")
    private List<String> networkInterfaces;

    /**
     * 磁盘列表
     */
    @Schema(description = "磁盘列表")
    private List<String> disks;

    /**
     * VNC 监听主机
     */
    @Schema(description = "VNC 监听主机", example = "0.0.0.0")
    private String vncHost;

    /**
     * VNC 端口（-1 表示自动分配/未知）
     */
    @Schema(description = "VNC端口（-1表示未知）", example = "5901")
    private Integer vncPort;
}

