package com.example.mini.multirole.service;

import com.example.mini.multirole.domain.TerminalType;

/**
 * 角色相关缓存抽象（按钮权限 / 菜单树 / 业务列表）。
 *
 * <p>设计文档 SW-4（安全重点）：角色切换成功后必须清除当前端的权限、菜单与
 * <b>业务列表</b>缓存，否则会因缓存残留导致越权数据可见。失败时（SW-7）则不清除。</p>
 */
public interface CacheService {

    /** 写入某端业务列表缓存（切换前/后用，演示用）。 */
    void putBusinessList(Long userId, TerminalType terminal, Object payload);

    /** 读取某端业务列表缓存。 */
    Object getBusinessList(Long userId, TerminalType terminal);

    /**
     * 清除指定端的角色相关全部缓存（权限 / 菜单 / 业务列表）。SW-4。
     */
    void clearRoleCache(Long userId, TerminalType terminal);
}
