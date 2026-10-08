package com.example.mini.multirole.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单树节点（GET /api/mini/menu 响应）。
 * 菜单可见性由角色权限过滤（设计文档 §2.2 权限矩阵）。
 */
@Data
public class MenuNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String path;
    /** 访问该菜单所需权限码；为空表示任意登录用户可见 */
    private String requiredPerm;
    private List<MenuNode> children;
}
