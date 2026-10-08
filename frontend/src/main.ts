/**
 * 前端入口示例：演示如何将多角色切换模块装配进 Vue3 / UniApp 应用。
 *
 * 说明：
 * - 注册 Pinia 与 v-perm 指令；
 * - 登录后调用 store.init(...) 初始化角色与 Token；
 * - 路由守卫 withPerm 用于页面级权限拦截。
 * UniApp 项目可将该逻辑放入 App.vue 的 onLaunch；此处以 Vite + Vue3 为例。
 */
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { registerPermDirective } from './directives/perm';
import { useRoleStore } from './stores/role';
import RoleSwitchPanel from './pages/role-switch/RoleSwitchPanel.vue';

const app = createApp(RoleSwitchPanel); // 实际项目中替换为根组件
app.use(createPinia());
registerPermDirective(app);

// —— 登录后初始化（示例）——
async function bootstrap(userId: number, terminal: string, token: string) {
  const store = useRoleStore();
  await store.init(userId, terminal, token);
}

// 在真实工程里，登录成功后调用 bootstrap(...)
// bootstrap(1, 'mini', 'token-xxx');

app.mount('#app');
