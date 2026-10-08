# mini-multirole · 微信小程序端多角色切换模块

将《小程序多角色切换功能设计》v1.3 中已有的伪代码落地为**可运行参考实现**，
覆盖后端（Spring Boot 3 + Java 17）与前端（Vue3 + Pinia + UniApp）两端。

> 溯源：本模块对应《系统需求说明书》v2.8 §15（多角色切换 / RS‑01~RS‑14）、
> 《架构设计说明书》v1.4 §3.2.1（多角色切换会话设计）、
> 《小程序多角色切换功能设计》v1.3（SW‑1~SW‑13）。

---

## 1. 技术栈

| 端 | 栈 | 说明 |
|----|----|------|
| 后端 | Spring Boot 3.4 / Java 17 / Sa‑Token / MyBatis‑Plus（声明） | 会话与权限以**内存实现**演示，生产替换为 Sa‑Token / Redis |
| 前端 | Vue3 + Pinia + UniApp（Vue SFC） | 角色 store、v‑perm 指令、路由守卫、切换面板 |

> **关于"可运行"的说明**：本仓库在编写环境仅配备 Python/Node，**无 JDK**，
> 因此后端代码经过严谨静态实现并配套 JUnit5 单元测试，但**未在本环境编译/启动**。
> 请在本地 JDK 17 + Maven 3.9 下 `mvn test` 验证。前端 `.vue`/`.ts` 同理需 Node 构建验证。

---

## 2. 目录结构

```
mini-multirole/
├── backend/                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/com/example/mini/multirole/
│       ├── common/          # Result / ErrorCode / BizException / GlobalExceptionHandler
│       ├── domain/          # RoleCode, TerminalType, AccountStatus, SysUser, Approval, ApprovalStatus
│       ├── service/         # UserService/SessionService/CacheService/ApprovalService/AuditService + 内存实现
│       │                    # RoleSwitchServiceImpl（核心：SW-1~SW-13）
│       ├── controller/      # UserRoleController（/api/mini/*）
│       └── dto/             # 请求/响应/视图对象
│   └── src/test/.../RoleSwitchServiceImplTest.java
└── frontend/                # Vue3 / UniApp 前端模块
    ├── src/api/role.ts       # 接口封装（fetch，UniApp 替换为 uni.request）
    ├── src/stores/role.ts    # Pinia store（切换 + 失败回滚 + 缓存失效）
    ├── src/directives/perm.ts# v-perm 按钮级指令（无权限移除元素）
    ├── src/guards/withPerm.ts# 路由守卫（页面级权限拦截）
    ├── src/pages/role-switch/# 角色切换面板组件
    └── src/main.ts           # 装配示例
```

---

## 3. 后端构建与运行

```bash
cd backend
# 需要 JDK 17 + Maven 3.9
mvn test                 # 运行 RoleSwitchServiceImplTest（7 个用例）
mvn spring-boot:run      # 启动，默认端口 8080
```

演示账号 `userId=1`（已绑定手机号、正常状态、授予 member/reviewer/operator/admin，默认角色 member）。

### 接口清单（设计文档 §4）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/mini/login?userId=&terminal=` | 演示登录，初始化当前端角色=默认角色 |
| GET  | `/api/mini/user/roles?userId=&terminal=` | 已授予角色 + 当前角色 + 切换入口可见性 |
| POST | `/api/mini/user/switch-role` | 切换角色（升权触发审批，返回 code=1005 + approvalId） |
| GET  | `/api/mini/perm/buttons?userId=&terminal=` | 当前角色按钮级权限码 |
| GET  | `/api/mini/menu?userId=&terminal=` | 当前角色菜单树 |
| POST | `/api/mini/approval/approve?approvalId=&operatorId=` | 管理员通过审批（通过后落地切换） |
| POST | `/api/mini/approval/reject?approvalId=&operatorId=&reason=` | 管理员拒绝 |
| GET  | `/api/mini/approval/pending` | 待审列表 |
| GET  | `/api/mini/approval/query?approvalId=` | 查询审批单 |

### 错误码（设计文档 §4）

| code | 含义 |
|------|------|
| 1001 | 目标角色未授予（ROLE_NOT_GRANTED） |
| 1002 | 当前角色已被回收（ROLE_REVOKED） |
| 1003 | 切换过于频繁（ROLE_SWITCH_RATE_LIMIT，10 次/分钟） |
| 1004 | 会话失效（SESSION_INVALID） |
| 1005 | 升权切换已提交审批（ROLE_SWITCH_PENDING） |
| 1006 | 审批被拒绝（ROLE_SWITCH_REJECTED） |
| 1007 | 审批已过期（APPROVAL_EXPIRED） |
| 2001 | 未绑定手机号（PHONE_NOT_BOUND，受限浏览态） |
| 2002/2003 | 账号冻结 / 注销或冷静期 |

---

## 4. 多角色切换规则闭环（SW‑1~SW‑13）

| 编号 | 规则 | 实现位置 |
|------|------|----------|
| SW‑1 | 切换换发该端 Token | `SessionService.switchRoleInSession` |
| SW‑4 | 成功后清空权限/菜单/业务列表缓存，防越权残留 | `CacheService.clearRoleCache` + 前端 store 置空 |
| SW‑6 | 草稿自动保存 | 前端 `switchRole` 暂存 `businessListCache` 草稿 |
| SW‑7 | 失败保持原角色、不清除缓存（回滚） | `RoleSwitchServiceImpl.doSwitch` catch 回滚 + 前端 catch |
| SW‑8 | 当前角色被回收强制回退默认角色 | `RoleSwitchServiceImpl.switchRole` 回收检测 |
| SW‑10 | 切换频率 ≤10 次/分钟 | `enforceRateLimit` 滑动窗口 |
| SW‑11 | 审计留痕 | `AuditService.recordSwitch` |
| SW‑12 | 设为默认角色 → 跨端同步（defaultRole 全局） | `RoleSwitchServiceImpl.doSwitch` applyAsDefault |
| SW‑13 | 升权（目标等级 > 当前）触发审批，通过后落地 | `RoleSwitchServiceImpl.switchRole` + `applyApprovedSwitch` |

---

## 5. 与需求文档的一致性

- **EARS**：SRS §15.4 的 RS‑01~RS‑14 在后端 `RoleCode` 权限等级、审批流、跨端同步中落地。
- **账号生命周期**：`SysUser` 字段（`phoneBound` / `accountStatus` / `defaultRole`）对齐 ADD v1.4 §5.1。
- **多端会话**：`TerminalType` + `SessionService` 按端隔离，对齐 SRS §3.2 / ADD §3.2.1。
- **安全**：无权限元素**移除**而非隐藏（`v-perm`），对齐设计文档权限控制结论。

---

## 6. 单元测试覆盖

`RoleSwitchServiceImplTest`（不依赖 Spring，直接装配内存实现）：

1. 即时切换（operator→member）
2. 升权触发审批 → 通过后落地（operator→admin）
3. 目标角色未授予 → 1001
4. 频率限制（第 11 次拒绝）
5. 当前角色被回收 → 强制回退默认（SW‑8）
6. 设为默认角色 → 跨端同步（SW‑12）
7. 未绑定手机号 → 2001

---

## 7. 后续接入生产

- `SessionService` / `CacheService` 替换为 Sa‑Token（`StpUtil.login(userId, terminal)`）+ Redis（key：`perm:{userId}:{terminal}`、`menu:tree:{userId}:{terminal}:{v}`、`bizlist:{userId}:{terminal}`，见 ADD §5.3）。
- `UserService` 内存仓储替换为 MyBatis‑Plus（实体对应 `sys_user`，含 `default_role` / `phone_bound` / `account_status` / `freeze_reason` / `close_cooldown_until`）。
- 审批服务 `ApprovalService` 持久化 + 消息通知。
- 审计 `AuditService` 落地到 `sys_audit_log`（SRS §19.4）。
