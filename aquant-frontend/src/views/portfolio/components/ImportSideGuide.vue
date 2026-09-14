<template>
  <div class="import-side-guide">
    <!-- 1. 导入说明 -->
    <div class="side-card help-guide-card">
      <div class="card-top-header">
        <div class="card-title-with-icon">
          <svg viewBox="0 0 1024 1024" width="16" height="16" fill="#475569">
            <path d="M854.6 288.7L639.4 73.4c-6-6-14.1-9.4-22.6-9.4H192c-17.7 0-32 14.3-32 32v832c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V311.3c0-8.5-3.4-16.6-9.4-22.6zM608 152.5l143.5 143.5H608V152.5zM800 896H224V128h320v200c0 17.7 14.3 32 32 32h200v504z" />
            <path d="M320 448h384v64H320zm0 128h384v64H320zm0 128h224v64H320z" />
          </svg>
          <span>导入说明</span>
        </div>
        <button class="view-help-link" @click="$emit('showHelp')">
          <span>查看帮助</span>
          <span class="arrow">→</span>
        </button>
      </div>

      <div class="help-steps-list">
        <div class="h-step-row">
          <span class="h-step-num">1</span>
          <span class="h-step-text">选择要导入数据的券商账户</span>
        </div>
        <div class="h-step-row">
          <span class="h-step-num">2</span>
          <span class="h-step-text">上传该账户的历史交易流水文件</span>
        </div>
        <div class="h-step-row">
          <span class="h-step-num">3</span>
          <span class="h-step-text">系统自动解析交易记录并导入</span>
        </div>
        <div class="h-step-row">
          <span class="h-step-num">4</span>
          <span class="h-step-text">导入完成后可在「持仓总览」查看最新数据</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
defineEmits<{
  (e: 'showHelp'): void;
}>();
</script>

<style scoped>
.import-side-guide {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 18px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.card-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.card-title-with-icon {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.view-help-link {
  background: transparent;
  border: none;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
}

.view-help-link:hover {
  color: #0f172a;
}

.help-steps-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.h-step-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.h-step-num {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.h-step-text {
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
}
</style>
