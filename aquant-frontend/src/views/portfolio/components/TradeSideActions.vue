<template>
  <div class="trade-side-actions">
    <!-- 最近导入批次 -->
    <div class="side-card batch-card">
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

    <!-- 快捷操作 -->
    <div class="side-card quick-actions-card">
      <div class="card-title" style="margin-bottom: 12px;">快捷操作</div>

      <div class="quick-action-items">
        <!-- 1. 记一笔交易 -->
        <div class="action-item-card" @click="$emit('openTradeModal')">
          <div class="item-icon-wrap">
            <span class="plus-sign">+</span>
          </div>
          <div class="item-info">
            <div class="item-title">记一笔交易</div>
            <div class="item-desc">手动添加买入、卖出等交易记录</div>
          </div>
          <div class="item-arrow">›</div>
        </div>

        <!-- 2. 导入交易文件 -->
        <div class="action-item-card" @click="$emit('goToImport')">
          <div class="item-icon-wrap">
            <svg viewBox="0 0 1024 1024" width="16" height="16" fill="#475569">
              <path d="M544 128H288c-35.3 0-64 28.7-64 64v640c0 35.3 28.7 64 64 64h448c35.3 0 64-28.7 64-64V384L544 128zm192 704H288V192h224v224h224v416z" />
              <path d="M512 704l-96-96h64V480h64v128h64z" />
            </svg>
          </div>
          <div class="item-info">
            <div class="item-title">导入交易文件</div>
            <div class="item-desc">支持 CSV、XLS、XLSX 格式</div>
          </div>
          <div class="item-arrow">›</div>
        </div>

        <!-- 3. 下载模板 -->
        <div class="action-item-card" @click="$emit('downloadTemplate')">
          <div class="item-icon-wrap">
            <svg viewBox="0 0 1024 1024" width="16" height="16" fill="#475569">
              <path d="M505.7 661a8 8 0 0 0 12.6 0l112-141.7c4.1-5.2.4-12.9-6.3-12.9h-74.1V168c0-4.4-3.6-8-8-8h-60c-4.4 0-8 3.6-8 8v338.3H400c-6.7 0-10.4 7.7-6.3 12.9l112 141.8zM878 626h-60c-4.4 0-8 3.6-8 8v154H214V634c0-4.4-3.6-8-8-8h-60c-4.4 0-8 3.6-8 8v198c0 17.7 14.3 32 32 32h684c17.7 0 32-14.3 32-32V634c0-4.4-3.6-8-8-8z" />
            </svg>
          </div>
          <div class="item-info">
            <div class="item-title">下载模板</div>
            <div class="item-desc">使用标准模板整理交易数据</div>
          </div>
          <div class="item-arrow">›</div>
        </div>

        <!-- 4. 交易流水使用说明 -->
        <div class="action-item-card" @click="$emit('showHelp')">
          <div class="item-icon-wrap">
            <svg viewBox="0 0 1024 1024" width="16" height="16" fill="#475569">
              <path d="M854.6 288.7L639.4 73.4c-6-6-14.1-9.4-22.6-9.4H192c-17.7 0-32 14.3-32 32v832c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V311.3c0-8.5-3.4-16.6-9.4-22.6zM608 152.5l143.5 143.5H608V152.5zM800 896H224V128h320v200c0 17.7 14.3 32 32 32h200v504z" />
              <path d="M320 448h384v64H320zm0 128h384v64H320zm0 128h224v64H320z" />
            </svg>
          </div>
          <div class="item-info">
            <div class="item-title">交易流水使用说明</div>
            <div class="item-desc">了解支持的文件格式和导入规范</div>
          </div>
          <div class="item-arrow">›</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { PortfolioImportBatchVO } from '@/types/portfolio';

const props = defineProps<{
  batches?: PortfolioImportBatchVO[];
}>();

defineEmits<{
  (e: 'viewAllBatches'): void;
  (e: 'openTradeModal'): void;
  (e: 'goToImport'): void;
  (e: 'downloadTemplate'): void;
  (e: 'showHelp'): void;
}>();

const latestBatch = computed(() => {
  return props.batches && props.batches.length > 0 ? props.batches[0] : null;
});
</script>

<style scoped>
.trade-side-actions {
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

.batch-card {
  min-height: 104px;
  height: 104px;
  box-sizing: border-box;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.card-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
}

.view-all-link:hover {
  color: #0f172a;
}

.batch-preview-box {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
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
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 13px;
  color: #94a3b8;
}

.quick-action-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-item-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-item-card:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
  transform: translateX(2px);
}

.item-icon-wrap {
  width: 32px;
  height: 32px;
  background: #ffffff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.plus-sign {
  font-size: 18px;
  color: #475569;
  font-weight: 300;
  line-height: 1;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.item-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.item-arrow {
  font-size: 18px;
  color: #cbd5e1;
  font-family: monospace;
}
</style>
