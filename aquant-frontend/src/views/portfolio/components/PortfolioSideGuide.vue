<template>
  <div class="portfolio-side-guide">
    <!-- 从何开始 卡片 -->
    <div class="guide-card start-guide-card">
      <div class="guide-header">
        <div class="guide-title">从何开始</div>
        <div class="guide-subtitle">四个简单步骤，快速开始使用 AQuant</div>
      </div>

      <div class="vertical-steps-list">
        <div
          v-for="(item, idx) in guideSteps"
          :key="item.step"
          class="v-step-item"
          @click="$emit('stepClick', item.step)"
        >
          <div class="v-step-icon">
            <div v-if="item.completed" class="icon-circle check-circle">
              <svg viewBox="0 0 1024 1024" width="12" height="12">
                <path
                  d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.8c4.1-5.1.4-12.9-6.3-12.9z"
                  fill="#ffffff"
                />
              </svg>
            </div>
            <div v-else class="icon-circle current-circle">
              {{ item.step }}
            </div>
            <div v-if="idx < guideSteps.length - 1" class="v-step-line" :class="{ 'is-active': item.completed }"></div>
          </div>

          <div class="v-step-content">
            <div class="v-step-title">{{ item.title }}</div>
            <div class="v-step-desc">{{ item.desc }}</div>
          </div>

          <div class="v-step-badge">
            <span v-if="item.completed" class="badge-tag tag-success">已完成</span>
            <span v-else class="badge-tag tag-running">进行中</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近导入批次 卡片 -->
    <div class="guide-card recent-batch-card">
      <div class="card-top-header">
        <span class="card-title">最近导入批次</span>
        <button class="view-all-link" @click="$emit('viewAllBatches')">
          <span>查看全部</span>
          <span class="arrow">→</span>
        </button>
      </div>

      <div v-if="latestBatch" class="batch-preview-box">
        <div class="batch-left">
          <div class="file-icon-wrap">
            <svg viewBox="0 0 1024 1024" width="18" height="18" fill="#64748b">
              <path d="M854.6 288.7L639.4 73.4c-6-6-14.1-9.4-22.6-9.4H192c-17.7 0-32 14.3-32 32v832c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V311.3c0-8.5-3.4-16.6-9.4-22.6zM608 152.5l143.5 143.5H608V152.5zM800 896H224V128h320v200c0 17.7 14.3 32 32 32h200v504z" />
            </svg>
          </div>
          <div class="file-info">
            <div class="file-name">{{ latestBatch.sourceFileName || latestBatch.batchNo || '-' }}</div>
            <div class="file-time">{{ latestBatch.createdAt || latestBatch.createTime || '-' }}</div>
          </div>
        </div>

        <div class="batch-right">
          <div class="status-pill success-pill">
            <svg viewBox="0 0 1024 1024" width="12" height="12" class="check-svg">
              <path
                d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm193.5 301.7l-210.6 292a31.8 31.8 0 0 1-51.7 0L318.5 514a8 8 0 0 1 1.3-11.2l34.8-26.6c4-3.1 9.8-2.6 13.2 1.3l83.6 96.8 174.5-242.1c3.6-5 10.6-6 15.6-2.4l37.2 26.7c5 3.6 6 10.6 2.4 15.6z"
                fill="#10b981"
              />
            </svg>
            <span>{{ latestBatch.successRows ?? latestBatch.successCount ?? 0 }} / {{ latestBatch.totalRows ?? latestBatch.totalCount ?? 0 }} 成功</span>
          </div>
        </div>
      </div>

      <div v-else class="empty-batch-box">
        <span class="empty-text">暂无导入记录</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { PortfolioImportBatchVO } from '@/types/portfolio';

const props = defineProps<{
  batches?: PortfolioImportBatchVO[];
  hasPortfolio?: boolean;
  hasAccount?: boolean;
  hasTrade?: boolean;
}>();

defineEmits<{
  (e: 'stepClick', step: number): void;
  (e: 'viewAllBatches'): void;
}>();

const latestBatch = computed(() => {
  return props.batches && props.batches.length > 0 ? props.batches[0] : null;
});

const guideSteps = computed(() => [
  {
    step: 1,
    title: '创建组合',
    desc: '新建一个投资组合',
    completed: props.hasPortfolio ?? true
  },
  {
    step: 2,
    title: '添加券商账户',
    desc: '关联您的券商资金账户',
    completed: props.hasAccount ?? true
  },
  {
    step: 3,
    title: '导入交易流水',
    desc: '导入历史交易数据',
    completed: props.hasTrade ?? true
  },
  {
    step: 4,
    title: '查看持仓',
    desc: '查看持仓与收益情况',
    completed: false
  }
]);
</script>

<style scoped>
.portfolio-side-guide {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.guide-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 18px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.guide-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.guide-subtitle {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 3px;
  margin-bottom: 16px;
}

.vertical-steps-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.v-step-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
  padding: 4px 0;
  transition: opacity 0.2s;
}

.v-step-item:hover {
  opacity: 0.85;
}

.v-step-icon {
  flex-shrink: 0;
  position: relative;
  width: 24px;
  display: flex;
  justify-content: center;
}

.v-step-line {
  position: absolute;
  top: 24px;
  bottom: -16px;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  background: #e2e8f0;
  z-index: 1;
}

.icon-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  position: relative;
  z-index: 2;
}

.check-circle {
  background: #0f172a;
  color: #ffffff;
}

.current-circle {
  background: #0f172a;
  color: #ffffff;
}

.v-step-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.v-step-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.v-step-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
}

.badge-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.tag-success {
  background: #f0fdf4;
  color: #16a34a;
}

.tag-running {
  background: #f1f5f9;
  color: #64748b;
}

.card-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.view-all-link {
  background: transparent;
  border: none;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  transition: color 0.15s;
}

.view-all-link:hover {
  color: #0f172a;
}

.batch-preview-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  padding: 8px 4px;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-icon-wrap {
  width: 32px;
  height: 32px;
  background: #f8fafc;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
}

.file-info {
  display: flex;
  flex-direction: column;
}

.file-name {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
}

.file-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.status-pill {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: #10b981;
}

.empty-batch-box {
  padding: 16px 0;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}
</style>
