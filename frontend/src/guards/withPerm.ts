/**
 * 路由守卫（对应设计文档 §5.3 withPerm / 路由守卫）。
 *
 * 规则：进入需要特定权限的路由前校验当前角色是否拥有该权限；无权限则拦截。
 * 适用于 PC（vue-router）与小程序（uni 路由拦截）的同源守卫逻辑。
 */
import { useRoleStore } from '../stores/role';

export interface RouteLike {
  path: string;
  meta?: { requiredPerm?: string };
}

export interface NextFn {
  (to?: string | Partial<RouteLike> | false | void): void;
}

/**
 * 生成权限路由守卫。
 * @param requiredPerm 访问所需权限码；也可从 route.meta.requiredPerm 读取
 */
export function withPerm(requiredPerm?: string) {
  return function guard(to: RouteLike, _from: RouteLike, next: NextFn) {
    const store = useRoleStore();
    const need = requiredPerm ?? to.meta?.requiredPerm;
    // 未初始化或角色未就绪，放行由页面自行兜底（或跳转登录）
    if (!store.currentRole) {
      next();
      return;
    }
    if (!need || store.hasPerm(need)) {
      next();
    } else {
      // 无权限：拦截到首页（可改为无权限提示页）
      next('/home');
    }
  };
}

/** 通用 beforeEach 守卫：读取 meta.requiredPerm 统一校验。 */
export function permBeforeEach(to: RouteLike, _from: RouteLike, next: NextFn) {
  withPerm()(to, _from, next);
}
