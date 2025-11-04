package com.pengchengyin.vmmanagerbackend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "修改虚拟机内用户密码请求，依赖QEMU Guest Agent")
public class ChangePasswordRequest {
    @NotBlank
    @Schema(description = "用户名", example = "root")
    private String username;

    @NotBlank
    @Schema(description = "新密码（当encrypted=false时为明文）", example = "Passw0rd!")
    private String password;

    @Schema(description = "密码是否已加密（默认false）", example = "false")
    private boolean encrypted = false;
}


