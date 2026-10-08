package com.example.mini.multirole.service;

import com.example.mini.multirole.common.BizException;
import com.example.mini.multirole.common.ErrorCode;
import com.example.mini.multirole.domain.AccountStatus;
import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.SysUser;
import com.example.mini.multirole.domain.TerminalType;
import com.example.mini.multirole.dto.MenuNode;
import com.example.mini.multirole.dto.SwitchRoleRequest;
import com.example.mini.multirole.dto.SwitchRoleResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RoleSwitchServiceImpl} 单元测试（不依赖 Spring，直接装配内存实现）。
 * 覆盖：即时切换、升权审批、目标未授予、频率限制、角色回收回退、跨端默认角色同步、强制绑定手机号。
 */
class RoleSwitchServiceImplTest {

    /** 构造被测服务及内存依赖，user 由调用方提供（便于不同场景）。 */
    private static class Harness {
        final InMemorySessionService session = new InMemorySessionService();
        final InMemoryCacheService cache = new InMemoryCacheService();
        final InMemoryApprovalService approval = new InMemoryApprovalService();
        final InMemoryAuditService audit = new InMemoryAuditService();
        final StubUserService userSvc;
        final RoleSwitchServiceImpl svc;

        Harness(SysUser user) {
            this.userSvc = new StubUserService(user);
            this.svc = new RoleSwitchServiceImpl(userSvc, session, cache, approval, audit);
        }
    }

    /** 仅返回固定用户的 UserService 桩（权限/菜单返回空，足够驱动切换逻辑）。 */
    static class StubUserService implements UserService {
        private final SysUser user;
        StubUserService(SysUser user) { this.user = user; }
        @Override public SysUser loadUser(Long userId) { return user; }
        @Override public Set<String> getButtonPerms(SysUser u, RoleCode r) { return new HashSet<>(); }
        @Override public List<MenuNode> getMenu(SysUser u, RoleCode r, TerminalType t) { return new ArrayList<>(); }
    }

    private static SysUser baseUser(Long id, Set<RoleCode> granted, RoleCode def, boolean phoneBound, AccountStatus st) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername("u" + id);
        u.setGrantedRoles(granted);
        u.setDefaultRole(def);
        u.setPhoneBound(phoneBound);
        u.setAccountStatus(st);
        return u;
    }

    private static SwitchRoleRequest req(Long userId, String terminal, String target, boolean asDefault) {
        SwitchRoleRequest r = new SwitchRoleRequest();
        r.setUserId(userId);
        r.setTerminal(terminal);
        r.setTargetRole(target);
        r.setApplyAsDefault(asDefault);
        return r;
    }

    // —— A. 正常即时切换（非升权：operator -> member）——
    @Test
    void testImmediateSwitchDowngrade() {
        SysUser u = baseUser(1L,
                Set.of(RoleCode.MEMBER, RoleCode.REVIEWER, RoleCode.OPERATOR, RoleCode.ADMIN),
                RoleCode.OPERATOR, true, AccountStatus.NORMAL);
        Harness h = new Harness(u);
        h.session.login(1L, TerminalType.MINI, RoleCode.OPERATOR); // 初始当前角色 = operator

        SwitchRoleResult res = h.svc.switchRole(req(1L, "mini", "member", false));

        assertEquals(SwitchRoleResult.Outcome.SWITCHED, res.getOutcome());
        assertEquals(RoleCode.MEMBER, res.getCurrentRole());
        assertNotNull(res.getToken());
        // SW-4：切换后业务列表缓存被清空
        h.cache.putBusinessList(1L, TerminalType.MINI, "some-data");
        h.svc.switchRole(req(1L, "mini", "reviewer", false)); // 再次切换触发清缓存
        assertEquals(null, h.cache.getBusinessList(1L, TerminalType.MINI));
    }

    // —— B. 升权触发审批，审批通过后落地 ——
    @Test
    void testEscalationApprovalFlow() {
        SysUser u = baseUser(2L,
                Set.of(RoleCode.MEMBER, RoleCode.REVIEWER, RoleCode.OPERATOR, RoleCode.ADMIN),
                RoleCode.OPERATOR, true, AccountStatus.NORMAL);
        Harness h = new Harness(u);
        h.session.login(2L, TerminalType.MINI, RoleCode.OPERATOR);

        SwitchRoleResult pending = h.svc.switchRole(req(2L, "mini", "admin", false));
        assertEquals(SwitchRoleResult.Outcome.PENDING, pending.getOutcome());
        String approvalId = pending.getApprovalId();
        assertNotNull(approvalId);

        h.approval.approve(approvalId, 999L);
        SwitchRoleResult done = h.svc.applyApprovedSwitch(approvalId);
        assertEquals(SwitchRoleResult.Outcome.SWITCHED, done.getOutcome());
        assertEquals(RoleCode.ADMIN, done.getCurrentRole());
    }

    // —— C. 目标角色未授予 ——
    @Test
    void testRoleNotGranted() {
        SysUser u = baseUser(3L,
                Set.of(RoleCode.MEMBER, RoleCode.REVIEWER, RoleCode.OPERATOR, RoleCode.ADMIN),
                RoleCode.OPERATOR, true, AccountStatus.NORMAL);
        Harness h = new Harness(u);
        h.session.login(3L, TerminalType.MINI, RoleCode.OPERATOR);

        BizException ex = assertThrows(BizException.class,
                () -> h.svc.switchRole(req(3L, "mini", "visitor", false)));
        assertEquals(ErrorCode.ROLE_NOT_GRANTED, ex.getErrorCode());
    }

    // —— D. 频率限制（60s 内 ≥10 次切换后第 11 次拒绝）——
    @Test
    void testRateLimit() {
        SysUser u = baseUser(4L,
                Set.of(RoleCode.MEMBER, RoleCode.REVIEWER, RoleCode.OPERATOR, RoleCode.ADMIN),
                RoleCode.OPERATOR, true, AccountStatus.NORMAL);
        Harness h = new Harness(u);
        h.session.login(4L, TerminalType.MINI, RoleCode.OPERATOR);

        for (int i = 0; i < 10; i++) {
            SwitchRoleResult r = h.svc.switchRole(req(4L, "mini", "member", false));
            assertEquals(SwitchRoleResult.Outcome.SWITCHED, r.getOutcome());
        }
        BizException ex = assertThrows(BizException.class,
                () -> h.svc.switchRole(req(4L, "mini", "member", false)));
        assertEquals(ErrorCode.ROLE_SWITCH_RATE_LIMIT, ex.getErrorCode());
    }

    // —— E. 当前角色被回收：强制回退默认角色（SW-8）——
    @Test
    void testRevokedRoleFallback() {
        // 仅授予 member；但会话中残留 operator（模拟旧角色被回收）
        SysUser u = baseUser(5L, Set.of(RoleCode.MEMBER), RoleCode.MEMBER, true, AccountStatus.NORMAL);
        Harness h = new Harness(u);
        h.session.login(5L, TerminalType.MINI, RoleCode.OPERATOR); // 残留高权限角色

        // 切到 member：发现 operator 已被回收 -> 回退默认 member 后正常完成
        SwitchRoleResult res = h.svc.switchRole(req(5L, "mini", "member", false));
        assertEquals(SwitchRoleResult.Outcome.SWITCHED, res.getOutcome());
        assertEquals(RoleCode.MEMBER, h.session.getCurrentRole(5L, TerminalType.MINI));
    }

    // —— F. 设为默认角色 -> 跨端同步（SW-12）——
    @Test
    void testApplyAsDefaultSync() {
        SysUser u = baseUser(6L,
                Set.of(RoleCode.MEMBER, RoleCode.REVIEWER, RoleCode.OPERATOR, RoleCode.ADMIN),
                RoleCode.OPERATOR, true, AccountStatus.NORMAL);
        Harness h = new Harness(u);
        h.session.login(6L, TerminalType.MINI, RoleCode.OPERATOR);

        SwitchRoleResult res = h.svc.switchRole(req(6L, "mini", "member", true));
        assertTrue(res.isDefaultRoleSynced());
        assertEquals(RoleCode.MEMBER, h.userSvc.loadUser(6L).getDefaultRole());
    }

    // —— G. 未绑定手机号：受限浏览态（PHONE_NOT_BOUND）——
    @Test
    void testPhoneNotBound() {
        SysUser u = baseUser(7L,
                Set.of(RoleCode.MEMBER, RoleCode.REVIEWER, RoleCode.OPERATOR, RoleCode.ADMIN),
                RoleCode.OPERATOR, false, AccountStatus.NORMAL);
        Harness h = new Harness(u);
        h.session.login(7L, TerminalType.MINI, RoleCode.OPERATOR);

        BizException ex = assertThrows(BizException.class,
                () -> h.svc.switchRole(req(7L, "mini", "admin", false)));
        assertEquals(ErrorCode.PHONE_NOT_BOUND, ex.getErrorCode());
    }
}
