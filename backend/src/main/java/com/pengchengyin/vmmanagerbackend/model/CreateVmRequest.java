package com.pengchengyin.vmmanagerbackend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 创建虚拟机请求
 */
@Schema(description = "创建虚拟机请求参数")
@Data
public class CreateVmRequest {
    /**
     * 虚拟机名称
     */
    @Schema(description = "虚拟机名称", example = "my-vm", required = true)
    @NotBlank(message = "虚拟机名称不能为空")
    private String name;

    /**
     * 内存大小（MB）
     */
    @Schema(description = "内存大小（MB）", example = "2048", minimum = "512")
    @Min(value = 512, message = "内存大小至少为512MB")
    private int memoryMB;

    /**
     * CPU核心数
     */
    @Schema(description = "CPU核心数", example = "2", minimum = "1")
    @Min(value = 1, message = "CPU核心数至少为1")
    private int cpuCount;

    /**
     * 磁盘镜像路径
     */
    @Schema(description = "磁盘镜像路径（qcow2格式）", example = "/var/lib/libvirt/images/my-vm.qcow2", required = true)
    @NotBlank(message = "磁盘镜像路径不能为空")
    private String diskImagePath;

    /**
     * XML配置文件路径（可选，如果提供则使用XML配置）
     */
    @Schema(description = "XML配置文件路径（可选，如果提供则使用XML配置）", example = "/path/to/vm.xml")
    private String xmlConfigPath;

    /**
     * 网络类型（bridge, nat等）
     */
    @Schema(description = "网络类型（bridge, nat等）", example = "nat", defaultValue = "nat")
    private String networkType = "nat";

    /**
     * 网络名称
     */
    @Schema(description = "网络名称", example = "default", defaultValue = "default")
    private String networkName = "default";
}

