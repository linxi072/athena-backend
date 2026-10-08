/**
 * v-perm 按钮级权限指令（对应设计文档 §5.2 / SW-? 权限控制）。
 *
 * 规则：无权限时直接从 DOM 移除元素（而非 display:none），避免残留可被绕过。
 * 用法：<button v-perm="'ugc:audit'">审核</button>
 */
import type { App, Directive, DirectiveBinding } from 'vue';
import { useRoleStore } from '../stores/role';

export const permDirective: Directive<HTMLElement, string> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
    const store = useRoleStore();
    if (!store.hasPerm(binding.value)) {
      // SW: 无权限移除，而非隐藏（防止越权元素残留在 DOM）
      const comment = document.createComment(`v-perm denied: ${binding.value}`);
      el.parentNode?.replaceChild(comment, el);
    }
  },
};

export function registerPermDirective(app: App) {
  app.directive('perm', permDirective);
}
