package com.example.mini.multirole.dto;

import com.example.mini.multirole.domain.RoleCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应（演示用，建立会话并初始化当前端角色为默认角色）。
 */
@Data
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private RoleCode currentRole;
    private String defaultRole;
}
