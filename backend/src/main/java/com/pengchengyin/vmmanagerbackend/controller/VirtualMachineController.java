package com.pengchengyin.vmmanagerbackend.controller;

import com.pengchengyin.vmmanagerbackend.model.CreateVmRequest;
import com.pengchengyin.vmmanagerbackend.model.VmInfo;
import com.pengchengyin.vmmanagerbackend.model.ChangePasswordRequest;
import com.pengchengyin.vmmanagerbackend.model.VmStatus;
import com.pengchengyin.vmmanagerbackend.service.VirtualMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.libvirt.LibvirtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 虚拟机管理REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/vms")
@Tag(name = "虚拟机管理", description = "KVM虚拟机管理API，包括创建、销毁、启动、关闭、状态监控等操作")
public class VirtualMachineController {

    @Autowired
    private VirtualMachineService vmService;

    /**
     * 获取所有虚拟机列表
     */
    @Operation(summary = "获取所有虚拟机列表", description = "返回系统中所有虚拟机的列表，包括运行中和已关闭的虚拟机")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取虚拟机列表"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAllVms() {
        try {
            List<VmInfo> vms = vmService.listAllVms();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", vms);
            response.put("count", vms.size());
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("获取虚拟机列表失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取虚拟机列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据名称获取虚拟机信息
     */
    @Operation(summary = "根据名称获取虚拟机信息", description = "根据虚拟机名称获取详细的虚拟机信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取虚拟机信息"),
            @ApiResponse(responseCode = "404", description = "虚拟机不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{name}")
    public ResponseEntity<Map<String, Object>> getVmByName(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            VmInfo vm = vmService.getVmByName(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", vm);
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("获取虚拟机信息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取虚拟机信息失败: " + e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 创建虚拟机
     */
    @Operation(summary = "创建虚拟机", description = "创建新的KVM虚拟机，需要提供虚拟机名称、内存、CPU、磁盘镜像等配置信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "虚拟机创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数验证失败"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createVm(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "创建虚拟机请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateVmRequest.class))
            )
            @Valid @RequestBody CreateVmRequest request) {
        try {
            VmInfo vm = vmService.createVm(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "虚拟机创建成功");
            response.put("data", vm);
            return ResponseEntity.status(201).body(response);
        } catch (LibvirtException e) {
            log.error("创建虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建虚拟机失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除虚拟机
     */
    @Operation(summary = "删除虚拟机", description = "删除指定的虚拟机，如果虚拟机正在运行，会先强制关闭再删除")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "虚拟机删除成功"),
            @ApiResponse(responseCode = "404", description = "虚拟机不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, Object>> destroyVm(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            vmService.destroyVm(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "虚拟机删除成功");
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("删除虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除虚拟机失败: " + e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 启动虚拟机
     */
    @Operation(summary = "启动虚拟机", description = "启动指定的虚拟机，虚拟机必须已定义且处于关闭状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "虚拟机启动成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{name}/start")
    public ResponseEntity<Map<String, Object>> startVm(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            vmService.startVm(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "虚拟机启动成功");
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("启动虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "启动虚拟机失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 关闭虚拟机（优雅关闭）
     */
    @Operation(summary = "关闭虚拟机（优雅关闭）", description = "向虚拟机发送关闭信号，允许操作系统正常关闭")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "关闭信号已发送"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{name}/shutdown")
    public ResponseEntity<Map<String, Object>> shutdownVm(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            vmService.shutdownVm(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "虚拟机关闭信号已发送");
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("关闭虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "关闭虚拟机失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 强制关闭虚拟机
     */
    @Operation(summary = "强制关闭虚拟机", description = "立即强制关闭虚拟机，不会等待操作系统正常关闭")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "虚拟机已强制关闭"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{name}/force-shutdown")
    public ResponseEntity<Map<String, Object>> forceShutdownVm(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            vmService.forceShutdownVm(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "虚拟机已强制关闭");
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("强制关闭虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "强制关闭虚拟机失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 重启虚拟机
     */
    @Operation(summary = "重启虚拟机", description = "向虚拟机发送重启信号，允许操作系统正常重启")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "重启信号已发送"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{name}/reboot")
    public ResponseEntity<Map<String, Object>> rebootVm(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            vmService.rebootVm(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "虚拟机重启信号已发送");
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("重启虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "重启虚拟机失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 暂停虚拟机
     */
    @Operation(summary = "暂停虚拟机", description = "暂停正在运行的虚拟机，虚拟机状态会被保存，可以通过恢复操作继续运行")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "虚拟机已暂停"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{name}/suspend")
    public ResponseEntity<Map<String, Object>> suspendVm(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            vmService.suspendVm(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "虚拟机已暂停");
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("暂停虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "暂停虚拟机失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 恢复虚拟机
     */
    @Operation(summary = "恢复虚拟机", description = "恢复被暂停的虚拟机，从暂停时的状态继续运行")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "虚拟机已恢复"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{name}/resume")
    public ResponseEntity<Map<String, Object>> resumeVm(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            vmService.resumeVm(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "虚拟机已恢复");
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("恢复虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "恢复虚拟机失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取虚拟机状态
     */
    @Operation(summary = "获取虚拟机状态", description = "获取指定虚拟机的当前状态信息（运行中、已关闭、暂停等）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取虚拟机状态"),
            @ApiResponse(responseCode = "404", description = "虚拟机不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{name}/status")
    public ResponseEntity<Map<String, Object>> getVmStatus(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            VmStatus status = vmService.getVmStatus(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "name", name,
                    "status", status.name(),
                    "statusCode", status.getCode(),
                    "description", status.getDescription()
            ));
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("获取虚拟机状态失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取虚拟机状态失败: " + e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 监控虚拟机（获取详细信息）
     */
    @Operation(summary = "监控虚拟机", description = "获取虚拟机的详细监控信息，包括CPU、内存、运行时间、网络接口等")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取虚拟机监控信息"),
            @ApiResponse(responseCode = "404", description = "虚拟机不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{name}/monitor")
    public ResponseEntity<Map<String, Object>> monitorVm(
            @Parameter(description = "虚拟机名称", required = true, example = "my-vm")
            @PathVariable String name) {
        try {
            VmInfo vm = vmService.monitorVm(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", vm);
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("监控虚拟机失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "监控虚拟机失败: " + e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(404).body(response);
            }
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 修改虚拟机内用户密码（依赖QEMU Guest Agent）
     */
    @Operation(summary = "修改虚拟机密码", description = "通过QEMU Guest Agent修改来宾系统用户密码。需要来宾系统安装并运行qemu-guest-agent")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码修改成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{name}/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Parameter(description = "虚拟机名称", required = true) @PathVariable String name,
            @Valid @RequestBody ChangePasswordRequest req) {
        try {
            vmService.changeGuestPassword(name, req.getUsername(), req.getPassword(), req.isEncrypted());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "密码修改指令已发送");
            return ResponseEntity.ok(response);
        } catch (LibvirtException e) {
            log.error("修改虚拟机密码失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "修改虚拟机密码失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

