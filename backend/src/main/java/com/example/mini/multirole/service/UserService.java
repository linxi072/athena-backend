package com.example.mini.multirole.service;

import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.SysUser;
import com.example.mini.multirole.domain.TerminalType;
import com.example.mini.multirole.dto.MenuNode;

import java.util.List;
import java.util.Set;

/**
 * 用户与权限供应服务。
 */
public interface UserService {

    /** 加载用户（演示用内存仓储；生产替换为 MyBatis-Plus）。 */
    SysUser loadUser(Long userId);

    /** 某角色下的按钮级权限码集合（设计文档 §2.2 权限矩阵）。 */
    Set<String> getButtonPerms(SysUser user, RoleCode role);

    /** 按角色过滤后的菜单树（设计文档 §2.2）。 */
    List<MenuNode> getMenu(SysUser user, RoleCode role, TerminalType terminal);
}
