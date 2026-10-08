package com.example.mini.multirole.service;

import com.example.mini.multirole.common.BizException;
import com.example.mini.multirole.common.ErrorCode;
import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.SysUser;
import com.example.mini.multirole.domain.TerminalType;
import com.example.mini.multirole.dto.MenuNode;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link UserService} 内存实现：内置演示用户与权限矩阵 / 菜单目录。
 *
 * <p>权限矩阵（设计文档 §2.2，权限等级 visitor&lt;member&lt;reviewer&lt;operator&lt;admin）：
 * 高等级继承低等级权限，admin 额外放开 {@code sys:config:edit}（v1.1 决策：小程序端系统配置可编辑）。</p>
 *
 * <p>演示账号 u1：已绑定手机号、状态正常、授予 member/reviewer/operator/admin 四角色，
 * 默认角色 member——用于验证多角色切换与升权审批。</p>
 */
@Service
public class InMemoryUserService implements UserService {

    private final Map<Long, SysUser> users = new HashMap<>();
    /** 角色 -> 该角色拥有的权限码（含继承） */
    private final Map<RoleCode, Set<String>> permMatrix = new EnumMap<>(RoleCode.class);
    /** 菜单目录（角色过滤的基础数据） */
    private final List<MenuNode> menuCatalog = new ArrayList<>();

    @PostConstruct
    public void seed() {
        initPermMatrix();
        initMenuCatalog();

        SysUser u1 = new SysUser();
        u1.setId(1L);
        u1.setUsername("demo_user");
        u1.setPhoneBound(true);
        u1.setAccountStatus(com.example.mini.multirole.domain.AccountStatus.NORMAL);
        u1.setGrantedRoles(Set.of(RoleCode.MEMBER, RoleCode.REVIEWER, RoleCode.OPERATOR, RoleCode.ADMIN));
        u1.setDefaultRole(RoleCode.MEMBER);
        users.put(u1.getId(), u1);
    }

    @Override
    public SysUser loadUser(Long userId) {
        SysUser u = users.get(userId);
        if (u == null) {
            throw new BizException(ErrorCode.SESSION_INVALID, "用户不存在");
        }
        return u;
    }

    @Override
    public Set<String> getButtonPerms(SysUser user, RoleCode role) {
        Set<String> perms = permMatrix.getOrDefault(role, Set.of());
        // 返回副本，避免外部修改
        return new HashSet<>(perms);
    }

    @Override
    public List<MenuNode> getMenu(SysUser user, RoleCode role, TerminalType terminal) {
        Set<String> perms = getButtonPerms(user, role);
        return filterMenu(menuCatalog, perms, terminal);
    }

    // ===== 私有：权限矩阵与菜单目录 =====

    private void initPermMatrix() {
        Set<String> visitor = Set.of();
        Set<String> member = Set.of("ugc:publish", "order:view");
        Set<String> reviewer = union(member, Set.of("ugc:audit", "ugc:remove:any"));
        Set<String> operator = union(reviewer, Set.of("ugc:promote", "content:manage", "data:export"));
        Set<String> admin = union(operator, Set.of("sys:config:edit", "sys:user:manage", "sys:role:manage"));
        permMatrix.put(RoleCode.VISITOR, visitor);
        permMatrix.put(RoleCode.MEMBER, member);
        permMatrix.put(RoleCode.REVIEWER, reviewer);
        permMatrix.put(RoleCode.OPERATOR, operator);
        permMatrix.put(RoleCode.ADMIN, admin);
    }

    private void initMenuCatalog() {
        // 首页（任意登录可见）
        menuCatalog.add(node("m_home", "首页", "/home", null));
        // 内容（member 起可见）
        menuCatalog.add(node("m_ugc", "内容社区", "/ugc", "ugc:publish"));
        // 审核（reviewer 起）
        MenuNode audit = node("m_audit", "内容审核", "/audit", "ugc:audit");
        audit.setChildren(List.of(
                node("m_audit_list", "待审列表", "/audit/list", "ugc:audit"),
                node("m_audit_remove", "违规处理", "/audit/remove", "ugc:remove:any")
        ));
        menuCatalog.add(audit);
        // 运营（operator 起）
        menuCatalog.add(node("m_ops", "运营管理", "/ops", "content:manage"));
        // 系统（admin 起）
        MenuNode sys = node("m_sys", "系统管理", "/sys", "sys:user:manage");
        sys.setChildren(List.of(
                node("m_sys_user", "用户管理", "/sys/user", "sys:user:manage"),
                node("m_sys_role", "角色管理", "/sys/role", "sys:role:manage"),
                node("m_sys_cfg", "系统配置", "/sys/config", "sys:config:edit")
        ));
        menuCatalog.add(sys);
    }

    private List<MenuNode> filterMenu(List<MenuNode> catalog, Set<String> perms, TerminalType terminal) {
        List<MenuNode> result = new ArrayList<>();
        for (MenuNode n : catalog) {
            if (!visible(n, perms)) {
                continue;
            }
            MenuNode copy = node(n.getId(), n.getName(), n.getPath(), n.getRequiredPerm());
            if (n.getChildren() != null) {
                copy.setChildren(filterMenu(n.getChildren(), perms, terminal));
            }
            result.add(copy);
        }
        return result;
    }

    private boolean visible(MenuNode n, Set<String> perms) {
        return n.getRequiredPerm() == null || perms.contains(n.getRequiredPerm());
    }

    private MenuNode node(String id, String name, String path, String requiredPerm) {
        MenuNode n = new MenuNode();
        n.setId(id);
        n.setName(name);
        n.setPath(path);
        n.setRequiredPerm(requiredPerm);
        return n;
    }

    private Set<String> union(Set<String> base, Set<String> extra) {
        Set<String> s = new HashSet<>(base);
        s.addAll(extra);
        return s;
    }
}
