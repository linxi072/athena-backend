package com.example.mini.multirole.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色切换请求（POST /api/mini/user/switch-role）。
 */
@Data
public class SwitchRoleRequest {

    /** 用户 ID */
    @NotNull(message = "userId 不能为空")
    private Long userId;

    /** 端类型编码：mini / pc / h5 / app */
    @NotBlank(message = "terminal 不能为空")
    private String terminal;

    /** 目标角色编码：visitor / member / reviewer / operator / admin */
    @NotBlank(message = "targetRole 不能为空")
    private String targetRole;

    /** 是否同时设为跨端默认角色（SW-12） */
    private boolean applyAsDefault;
}
