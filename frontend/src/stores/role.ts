/**
 * 多角色切换 Pinia store（对应设计文档 §5.1 switchRole / §3 状态机）。
 *
 * 关键点：
 * - 切换成功：更新 token / 当前角色，并清空权限、菜单、业务列表缓存（SW-4，防越权残留）。
 * - 切换失败：保持原角色与缓存不变（SW-7，失败回滚，不清除缓存）。
 * - 草稿自动保存（SW-6）：切换前暂存当前业务草稿，失败时恢复。
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import {
  fetchRoles,
  switchRole as apiSwitchRole,
  approve as apiApprove,
  fetchButtons,
  fetchMenu,
  type RoleSummary,
  type SwitchResult,
} from '../api/role';

export const useRoleStore = defineStore('role', () => {
  // —— 状态 ——
  const userId = ref<number | null>(null);
  const terminal = ref<string>('mini');
  const roles = ref<RoleSummary['roles']>([]);
  const currentRole = ref<string>(''); // 当前生效角色（SW-1 切换换发 Token 后更新）
  const defaultRole = ref<string>(''); // 跨端默认角色（SW-12）
  const token = ref<string>(''); // 当前端 Token（SW-1 换发后刷新）
  const switchEntryVisible = ref<boolean>(false); // 授予 ≥2 角色才显示入口
  const pendingApprovalId = ref<string>(''); // 升权待审单号（SW-13）

  // 客户端缓存（切换成功即失效，SW-4）
  const permCache = ref<Record<string, string[]>>({});
  const menuCache = ref<any[]>([]);
  const businessListCache = ref<any>(null); // 业务列表（越权残留重点，SW-4）

  // —— 计算 ——
  const isMultiRole = computed(() => roles.value.length >= 2);

  /** 是否拥有某按钮权限（v-perm 指令使用）。 */
  function hasPerm(code: string): boolean {
    if (!currentRole.value) return false;
    return (permCache.value[currentRole.value] ?? []).includes(code);
  }

  // —— 动作 ——
  /** 初始化：登录后拉取角色并缓存权限/菜单。 */
  async function init(uid: number, term: string, tk: string) {
    userId.value = uid;
    terminal.value = term;
    token.value = tk;
    await refreshRoles();
  }

  async function refreshRoles() {
    if (userId.value == null) return;
    const summary = await fetchRoles(userId.value, terminal.value);
    roles.value = summary.roles;
    currentRole.value = summary.currentRole;
    defaultRole.value = summary.defaultRole;
    switchEntryVisible.value = summary.switchEntryVisible;
    await reloadCaches();
  }

  /** 切换成功/初始化后重载权限与菜单缓存（SW-4 的“写入”侧）。 */
  async function reloadCaches() {
    if (userId.value == null || !currentRole.value) return;
    const [perms, menu] = await Promise.all([
      fetchButtons(userId.value, terminal.value),
      fetchMenu(userId.value, terminal.value),
    ]);
    permCache.value[currentRole.value] = perms;
    menuCache.value = menu;
  }

  /** 切换角色；支持升权审批与跨端默认角色同步。 */
  async function switchRole(targetRole: string, applyAsDefault = false): Promise<SwitchResult> {
    if (userId.value == null) throw new Error('未初始化');
    // SW-6：暂存业务草稿与当前缓存快照，便于失败恢复
    const draftBusiness = businessListCache.value;
    const prevRole = currentRole.value;
    const prevToken = token.value;

    try {
      const res = await apiSwitchRole(userId.value, terminal.value, targetRole, applyAsDefault);
      if (res.approvalId) {
        // SW-13：升权触发审批，等待管理员通过
        pendingApprovalId.value = res.approvalId;
        return res;
      }
      // 切换成功：刷新 Token 与当前角色（SW-1）
      token.value = res.token;
      currentRole.value = res.currentRole;
      if (res.defaultRoleSynced) defaultRole.value = res.currentRole;
      // SW-4：清空业务列表等缓存，防越权残留（不依赖旧缓存）
      businessListCache.value = null;
      permCache.value = {};
      menuCache.value = [];
      await reloadCaches(); // 重新拉取新角色的权限/菜单
      return res;
    } catch (e) {
      // SW-7：失败保持原角色与缓存，恢复草稿，不清除
      currentRole.value = prevRole;
      token.value = prevToken;
      businessListCache.value = draftBusiness;
      throw e;
    }
  }

  /** 管理员通过后完成升权切换（SW-13 落地）。 */
  async function completeApproval(operatorId: number): Promise<SwitchResult> {
    if (!pendingApprovalId.value) throw new Error('无待审审批单');
    const res = await apiApprove(pendingApprovalId.value, operatorId);
    token.value = res.token;
    currentRole.value = res.currentRole;
    businessListCache.value = null;
    permCache.value = {};
    menuCache.value = [];
    pendingApprovalId.value = '';
    await reloadCaches();
    return res;
  }

  return {
    userId, terminal, roles, currentRole, defaultRole, token,
    switchEntryVisible, pendingApprovalId,
    permCache, menuCache, businessListCache,
    isMultiRole, hasPerm,
    init, refreshRoles, switchRole, completeApproval,
  };
});
