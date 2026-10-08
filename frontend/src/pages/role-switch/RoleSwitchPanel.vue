<script setup lang="ts">
/**
 * 角色切换面板（对应设计文档 §5.5 / 原型 2.7）。
 * 功能：角色选择、切换、设为默认角色（跨端同步 SW-12）、升权审批待审态（SW-13）。
 */
import { ref } from 'vue';
import { useRoleStore } from '../../stores/role';
import { queryApproval, type ApprovalVO } from '../../api/role';

const store = useRoleStore();

const applyAsDefault = ref(false); // 设为默认角色（跨端同步 SW-12）
const switching = ref(false);
const toast = ref('');
const pending = ref<ApprovalVO | null>(null); // 升权待审态（SW-13）

function showMsg(msg: string) {
  toast.value = msg;
  setTimeout(() => (toast.value = ''), 2500);
}

async function onSwitch(targetRole: string) {
  if (switching.value) return;
  switching.value = true;
  try {
    const res = await store.switchRole(targetRole, applyAsDefault.value);
    if (res.approvalId) {
      // 升权触发审批：进入待审态
      pending.value = await queryApproval(res.approvalId);
      showMsg('升权切换已提交审批，待管理员通过');
    } else {
      pending.value = null;
      showMsg(`已切换至 ${res.currentRole}`);
    }
  } catch (e: any) {
    // SW-7：失败已由 store 回滚，这里仅提示
    showMsg(e?.message || '切换失败');
  } finally {
    switching.value = false;
  }
}

/** 申请人主动查询审批结果（待审态轮询）。 */
async function refreshPending() {
  if (!pending.value) return;
  pending.value = await queryApproval(pending.value.id);
}
</script>

<template>
  <view class="role-panel">
    <view class="title">角色切换</view>

    <!-- 当前角色 -->
    <view class="current">当前角色：<text class="strong">{{ store.currentRole }}</text></view>

    <!-- 跨端默认角色同步开关（SW-12） -->
    <label class="default-switch">
      <input type="checkbox" v-model="applyAsDefault" />
      设为默认角色（跨端同步）
    </label>

    <!-- 角色列表 -->
    <view v-if="store.switchEntryVisible" class="role-list">
      <view
        v-for="r in store.roles"
        :key="r.code"
        class="role-item"
        :class="{ active: r.current }"
      >
        <text>{{ r.desc }}</text>
        <button
          v-if="!r.current"
          size="mini"
          @click="onSwitch(r.code)"
          :disabled="switching"
        >切换</button>
        <text v-else class="tag">使用中</text>
      </view>
    </view>
    <view v-else class="hint">当前账号仅单一角色，无需切换</view>

    <!-- 升权审批待审态（SW-13） -->
    <view v-if="pending" class="approval-pending">
      <view>升权审批待审：{{ pending.toRole }}</view>
      <view class="approval-id">单号：{{ pending.id }}</view>
      <view>状态：{{ pending.status }}</view>
      <button size="mini" @click="refreshPending">查询审批结果</button>
    </view>

    <!-- 提示 -->
    <view v-if="toast" class="toast">{{ toast }}</view>
  </view>
</template>

<style scoped>
.role-panel { padding: 24rpx; }
.title { font-size: 32rpx; font-weight: 600; margin-bottom: 16rpx; }
.current { margin-bottom: 12rpx; }
.strong { color: #07c160; }
.default-switch { display: flex; align-items: center; gap: 8rpx; margin-bottom: 16rpx; }
.role-list { display: flex; flex-direction: column; gap: 12rpx; }
.role-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16rpx; border: 1rpx solid #eee; border-radius: 8rpx;
}
.role-item.active { border-color: #07c160; }
.tag { color: #07c160; font-size: 24rpx; }
.hint { color: #999; font-size: 24rpx; }
.approval-pending {
  margin-top: 20rpx; padding: 16rpx; background: #fff7e6;
  border: 1rpx solid #ffd591; border-radius: 8rpx;
}
.approval-id { color: #999; font-size: 22rpx; word-break: break-all; }
.toast {
  margin-top: 20rpx; padding: 12rpx 16rpx; background: rgba(0,0,0,0.7);
  color: #fff; border-radius: 8rpx; font-size: 24rpx;
}
</style>
