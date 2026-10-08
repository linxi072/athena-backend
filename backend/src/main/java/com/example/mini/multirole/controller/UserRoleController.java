package com.example.mini.multirole.controller;

import com.example.mini.multirole.common.BizException;
import com.example.mini.multirole.common.ErrorCode;
import com.example.mini.multirole.common.Result;
import com.example.mini.multirole.domain.RoleCode;
import com.example.mini.multirole.domain.SysUser;
import com.example.mini.multirole.domain.TerminalType;
import com.example.mini.multirole.dto.ApprovalVO;
import com.example.mini.multirole.dto.LoginResponse;
import com.example.mini.multirole.dto.MenuNode;
import com.example.mini.multirole.dto.RoleVO;
import com.example.mini.multirole.dto.SwitchRoleRequest;
import com.example.mini.multirole.dto.SwitchRoleResult;
import com.example.mini.multirole.service.ApprovalService;
import com.example.mini.multirole.service.RoleSwitchService;
import com.example.mini.multirole.service.SessionService;
import com.example.mini.multirole.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序端多角色切换接口（对应设计文档 §4 接口清单）。
 *
 * <p>端点：
 * <ul>
 *   <li>POST /api/mini/login          演示登录（建立会话，初始化当前端角色=默认角色）</li>
 *   <li>GET  /api/mini/user/roles     已授予角色 + 当前角色 + 是否显示切换入口</li>
 *   <li>POST /api/mini/user/switch-role 切换角色（升权触发审批）</li>
 *   <li>GET  /api/mini/perm/buttons   当前角色按钮级权限</li>
 *   <li>GET  /api/mini/menu           当前角色菜单树</li>
 *   <li>POST /api/mini/approval/approve|reject  管理员审批（SW-13）</li>
 *   <li>GET  /api/mini/approval/pending|query   审批单查询</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/mini")
public class UserRoleController {

    private final UserService userService;
    private final SessionService sessionService;
    private final RoleSwitchService roleSwitchService;
    private final ApprovalService approvalService;

    public UserRoleController(UserService userService, SessionService sessionService,
                              RoleSwitchService roleSwitchService, ApprovalService approvalService) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.roleSwitchService = roleSwitchService;
        this.approvalService = approvalService;
    }

    /** 演示登录：建立该端会话并将当前角色初始化为默认角色。 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestParam Long userId, @RequestParam String terminal) {
        SysUser user = userService.loadUser(userId);
        if (!user.isPhoneBound()) {
            throw new BizException(ErrorCode.PHONE_NOT_BOUND);
        }
        if (!user.getAccountStatus().accessible()) {
            throw new BizException(user.getAccountStatus().name().equals("FROZEN")
                    ? ErrorCode.ACCOUNT_FROZEN : ErrorCode.ACCOUNT_CLOSED);
        }
        TerminalType tt = TerminalType.of(terminal);
        String token = sessionService.login(userId, tt, user.getDefaultRole());
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setCurrentRole(user.getDefaultRole());
        resp.setDefaultRole(user.getDefaultRole() == null ? null : user.getDefaultRole().getCode());
        return Result.success(resp);
    }

    /** 已授予角色 + 当前角色 + 是否显示切换入口（授予 ≥2 角色才显示，设计文档结论）。 */
    @GetMapping("/user/roles")
    public Result<Map<String, Object>> roles(@RequestParam Long userId, @RequestParam String terminal) {
        SysUser user = userService.loadUser(userId);
        TerminalType tt = TerminalType.of(terminal);
        RoleCode current = sessionService.getCurrentRole(userId, tt);
        List<RoleVO> list = new ArrayList<>();
        for (RoleCode r : user.getGrantedRoles()) {
            list.add(RoleVO.from(r, r.equals(current)));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("roles", list);
        data.put("defaultRole", user.getDefaultRole() == null ? null : user.getDefaultRole().getCode());
        data.put("currentRole", current == null ? null : current.getCode());
        data.put("switchEntryVisible", user.getGrantedRoleCount() >= 2);
        return Result.success(data);
    }

    /** 切换角色。升权时返回待审（错误码 1005 + approvalId）。 */
    @PostMapping("/user/switch-role")
    public Result<Object> switchRole(@Valid @RequestBody SwitchRoleRequest req) {
        SwitchRoleResult res = roleSwitchService.switchRole(req);
        if (res.getOutcome() == SwitchRoleResult.Outcome.PENDING) {
            Map<String, Object> d = new HashMap<>();
            d.put("approvalId", res.getApprovalId());
            return Result.error(ErrorCode.ROLE_SWITCH_PENDING, "升权切换已提交审批，待管理员通过", d);
        }
        return Result.success(res);
    }

    /** 当前角色按钮级权限码。 */
    @GetMapping("/perm/buttons")
    public Result<List<String>> buttons(@RequestParam Long userId, @RequestParam String terminal) {
        SysUser user = userService.loadUser(userId);
        TerminalType tt = TerminalType.of(terminal);
        RoleCode current = requireCurrent(userId, tt);
        return Result.success(new ArrayList<>(userService.getButtonPerms(user, current)));
    }

    /** 当前角色菜单树（按端过滤）。 */
    @GetMapping("/menu")
    public Result<List<MenuNode>> menu(@RequestParam Long userId,
                                      @RequestParam(defaultValue = "mini") String terminal) {
        SysUser user = userService.loadUser(userId);
        TerminalType tt = TerminalType.of(terminal);
        RoleCode current = requireCurrent(userId, tt);
        return Result.success(userService.getMenu(user, current, tt));
    }

    /** 管理员通过审批（通过后立即执行实际切换）。 */
    @PostMapping("/approval/approve")
    public Result<SwitchRoleResult> approve(@RequestParam String approvalId, @RequestParam Long operatorId) {
        approvalService.approve(approvalId, operatorId);
        return Result.success(roleSwitchService.applyApprovedSwitch(approvalId));
    }

    /** 管理员拒绝审批。 */
    @PostMapping("/approval/reject")
    public Result<ApprovalVO> reject(@RequestParam String approvalId,
                                    @RequestParam Long operatorId,
                                    @RequestParam(required = false) String reason) {
        return Result.success(ApprovalVO.from(approvalService.reject(approvalId, operatorId, reason)));
    }

    /** 管理员视角：待审列表。 */
    @GetMapping("/approval/pending")
    public Result<List<ApprovalVO>> pending() {
        List<ApprovalVO> list = new ArrayList<>();
        approvalService.listPending().forEach(a -> list.add(ApprovalVO.from(a)));
        return Result.success(list);
    }

    /** 申请人查询审批单。 */
    @GetMapping("/approval/query")
    public Result<ApprovalVO> query(@RequestParam String approvalId) {
        return Result.success(ApprovalVO.from(approvalService.query(approvalId)));
    }

    private RoleCode requireCurrent(Long userId, TerminalType terminal) {
        RoleCode current = sessionService.getCurrentRole(userId, terminal);
        if (current == null) {
            throw new BizException(ErrorCode.SESSION_INVALID, "当前端未登录，请先登录");
        }
        return current;
    }
}
