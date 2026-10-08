package com.example.mini.multirole.service;

import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.TerminalType;

/**
 * 会话与多端 Token 抽象。
 *
 * <p>生产环境以 Sa-Token（SRS §3.2 / ADD §3.2.1）实现“按 terminal 维度的独立会话”
 * 与“切换换发该端 Token”；本模块提供内存实现以便单测与本地验证，可无缝替换。</p>
 */
public interface SessionService {

    /**
     * 登录并为指定端签发 Token，同时把该端当前角色初始化为给定角色。
     *
     * @return 新签发的 Token
     */
    String login(Long userId, TerminalType terminal, RoleCode currentRole);

    /**
     * 切换指定端的当前角色（等价“换发该端 Token”）。
     *
     * @return 换发后的新 Token
     */
    String switchRoleInSession(Long userId, TerminalType terminal, RoleCode newRole);

    /** 读取指定端当前生效角色；端未登录返回 null。 */
    RoleCode getCurrentRole(Long userId, TerminalType terminal);

    /** 判断指定端是否处于登录态。 */
    boolean isLoggedIn(Long userId, TerminalType terminal);

    /**
     * 废止该用户全部端的会话 Token（账号冻结 EV-11 / 注销生效时的即时失效）。
     * 对应设计文档 ST-03：冻结态无任何业务可通达。
     */
    void invalidateAll(Long userId);
}
