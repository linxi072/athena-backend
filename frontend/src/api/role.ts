/**
 * 角色相关接口封装（对应后端 /api/mini/*）。
 *
 * 说明：默认使用浏览器/Node 的 fetch。UniApp 小程序端请将 request 实现替换为
 * uni.request（保持 Promise 形态即可），其余调用层无需改动。
 */

const BASE_URL = import.meta.env?.VITE_API_BASE ?? '/api/mini';

/** 请求封装：自动附带 token，统一解析后端 Result<T>。 */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = uniToken();
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  });
  const body = await res.json();
  // 后端统一结构：{ code, message, data }
  if (body.code !== 0) {
    const err = new Error(body.message || '请求失败') as Error & { code?: number };
    err.code = body.code;
    throw err;
  }
  return body.data as T;
}

function uniToken(): string | null {
  return localStorage.getItem('mini_token') ?? sessionStorage.getItem('mini_token');
}

export interface RoleVO {
  code: string;
  desc: string;
  level: number;
  current: boolean;
}

export interface RoleSummary {
  roles: RoleVO[];
  defaultRole: string;
  currentRole: string;
  switchEntryVisible: boolean;
}

export interface LoginResp {
  token: string;
  currentRole: string;
  defaultRole: string;
}

export interface SwitchResult {
  token: string;
  currentRole: string;
  defaultRoleSynced: boolean;
  approvalId?: string;
}

export interface ApprovalVO {
  id: string;
  fromRole: string;
  toRole: string;
  status: string;
  createdAt?: string;
  decidedAt?: string;
  reason?: string;
}

/** 演示登录：建立会话并初始化当前端角色=默认角色。 */
export function login(userId: number, terminal: string) {
  return request<LoginResp>(`/login?userId=${userId}&terminal=${terminal}`, { method: 'POST' });
}

/** 查询已授予角色 + 当前角色 + 切换入口可见性。 */
export function fetchRoles(userId: number, terminal: string) {
  return request<RoleSummary>(`/user/roles?userId=${userId}&terminal=${terminal}`);
}

/** 切换角色；升权时返回 { approvalId }（code=1005），由调用方进入审批流程。 */
export function switchRole(
  userId: number,
  terminal: string,
  targetRole: string,
  applyAsDefault = false,
) {
  return request<SwitchResult>('/user/switch-role', {
    method: 'POST',
    body: JSON.stringify({ userId, terminal, targetRole, applyAsDefault }),
  });
}

/** 当前角色按钮级权限码。 */
export function fetchButtons(userId: number, terminal: string) {
  return request<string[]>(`/perm/buttons?userId=${userId}&terminal=${terminal}`);
}

/** 当前角色菜单树。 */
export function fetchMenu(userId: number, terminal: string) {
  return request<any[]>(`/menu?userId=${userId}&terminal=${terminal}`);
}

/** 管理员通过审批（通过后立即完成切换）。 */
export function approve(approvalId: string, operatorId: number) {
  return request<SwitchResult>(`/approval/approve?approvalId=${approvalId}&operatorId=${operatorId}`, {
    method: 'POST',
  });
}

/** 管理员拒绝审批。 */
export function reject(approvalId: string, operatorId: number, reason?: string) {
  const q = `approvalId=${approvalId}&operatorId=${operatorId}${reason ? `&reason=${encodeURIComponent(reason)}` : ''}`;
  return request<ApprovalVO>(`/approval/reject?${q}`, { method: 'POST' });
}

/** 待审列表。 */
export function pendingApprovals() {
  return request<ApprovalVO[]>('/approval/pending');
}

/** 查询审批单。 */
export function queryApproval(approvalId: string) {
  return request<ApprovalVO>(`/approval/query?approvalId=${approvalId}`);
}
