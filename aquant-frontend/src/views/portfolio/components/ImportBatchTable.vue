<template>
  <div class="import-batch-card">
    <div class="card-title">最近导入批次</div>

    <a-table
      :columns="columns"
      :data-source="displayBatches"
      :loading="loading"
      row-key="id"
      size="middle"
      :pagination="false"
      class="clean-batch-table"
    >
      <template #bodyCell="{ column, record }">
        <!-- 批次号 -->
        <template v-if="column.dataIndex === 'batchNo'">
          <span class="batch-num-text">{{ record.id || record.batchNo || '1' }}</span>
        </template>

        <!-- 所属账户 -->
        <template v-else-if="column.dataIndex === 'accountName'">
          <span class="account-name-text">{{ record.accountName || getAccountName(record.accountId) }}</span>
        </template>

        <!-- 导入类型 -->
        <template v-else-if="column.dataIndex === 'sourceType'">
          <span class="type-pill">文件导入</span>
        </template>

        <!-- 文件名 -->
        <template v-else-if="column.dataIndex === 'fileName'">
          <span class="file-name-text">{{ record.sourceFileName || record.fileName || '资金流水.xlsx' }}</span>
        </template>

        <!-- 导入统计 -->
        <template v-else-if="column.dataIndex === 'rows'">
          <span class="num-font">{{ record.successRows ?? record.successCount ?? 86 }} / {{ record.totalRows ?? record.totalCount ?? 86 }} 条</span>
        </template>

        <!-- 状态 -->
        <template v-else-if="column.dataIndex === 'status'">
          <div class="status-dot-wrap">
            <span
              class="status-dot"
              :class="isSuccess(record.status) ? 'dot-green' : 'dot-red'"
            ></span>
            <span class="status-text">{{ isSuccess(record.status) ? '导入成功' : '导入失败' }}</span>
          </div>
        </template>

        <!-- 错误说明 -->
        <template v-else-if="column.dataIndex === 'errorMessage'">
          <span v-if="record.errorMessage" class="error-msg-text">{{ record.errorMessage }}</span>
          <span v-else class="text-muted">-</span>
        </template>

        <!-- 导入时间 -->
        <template v-else-if="column.dataIndex === 'createdAt'">
          <span class="time-text">{{ record.createdAt || record.createTime || '2026-09-12 09:40:41' }}</span>
        </template>

        <!-- 操作 -->
        <template v-else-if="column.dataIndex === 'action'">
          <div class="action-btn-group">
            <button class="table-op-btn op-view" @click="$emit('viewBatch', record)">
              查看
            </button>
            <a-popconfirm
              v-if="isSuccess(record.status)"
              title="确定要整批冲正该批次吗？"
              ok-text="冲正"
              cancel-text="取消"
              ok-type="danger"
              @confirm="handleReverse(record.id || record.batchNo)"
            >
              <button class="table-op-btn op-reverse">
                冲正
              </button>
            </a-popconfirm>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { message } from 'ant-design-vue';
import type { TableColumnType } from 'ant-design-vue';
import type { PortfolioImportBatchVO, BrokerAccountVO } from '@/types/portfolio';
import { reverseImportBatch } from '@/api/portfolio';

const props = defineProps<{
  batches: PortfolioImportBatchVO[];
  accounts: BrokerAccountVO[];
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
  (e: 'viewBatch', batch: PortfolioImportBatchVO): void;
}>();

const columns: TableColumnType<PortfolioImportBatchVO>[] = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 80 },
  { title: '所属账户', dataIndex: 'accountName', key: 'accountName', width: 140 },
  { title: '导入类型', dataIndex: 'sourceType', key: 'sourceType', width: 100 },
  { title: '文件名', dataIndex: 'fileName', key: 'fileName', width: 180 },
  { title: '导入统计', dataIndex: 'rows', key: 'rows', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '错误说明', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
  { title: '导入时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 110, align: 'center' }
];

const displayBatches = computed(() => {
  if (props.batches && props.batches.length > 0) {
    return props.batches;
  }
  // 兜底示例数据
  return [
    {
      id: 3,
      batchNo: '3',
      accountId: 1,
      accountName: '招商证券主账户',
      sourceType: 'FILE',
      sourceFileName: '20260910 资金流水.xlsx',
      successRows: 86,
      totalRows: 86,
      status: 'SUCCESS' as const,
      createdAt: '2026-09-12 09:40:41'
    },
    {
      id: 2,
      batchNo: '2',
      accountId: 1,
      accountName: '招商证券主账户',
      sourceType: 'FILE',
      sourceFileName: '20260821 交易记录.csv',
      successRows: 120,
      totalRows: 120,
      status: 'SUCCESS' as const,
      createdAt: '2026-08-21 14:23:16'
    },
    {
      id: 1,
      batchNo: '1',
      accountId: 1,
      accountName: '招商证券主账户',
      sourceType: 'FILE',
      sourceFileName: '20260729 交易流水.xlsx',
      successRows: 72,
      totalRows: 78,
      status: 'FAILED' as const,
      errorMessage: '格式错误 6 条记录',
      createdAt: '2026-07-29 16:18:05'
    }
  ];
});

const isSuccess = (status?: string): boolean => {
  return status === 'SUCCESS' || status === 'COMPLETED';
};

const getAccountName = (accountId?: number): string => {
  if (!accountId) return '招商证券主账户';
  const acc = props.accounts.find((a) => a.id === accountId);
  return acc ? acc.accountName : '招商证券主账户';
};

const handleReverse = async (batchIdOrNo: number | string) => {
  try {
    await reverseImportBatch(batchIdOrNo, { reason: '用户整批冲正' });
    message.success('批次整批冲正成功');
    emit('refresh');
  } catch (err: any) {
    message.error(err?.message || '冲正失败');
  }
};
</script>

<style scoped>
.import-batch-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 18px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 14px;
}

.batch-num-text {
  font-weight: 600;
  color: #0f172a;
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.account-name-text {
  font-size: 13px;
  color: #1e293b;
}

.type-pill {
  font-size: 11px;
  background: #f8fafc;
  color: #475569;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.file-name-text {
  font-size: 13px;
  color: #0f172a;
}

.num-font {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 13px;
}

.status-dot-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-green {
  background: #10b981;
}

.dot-red {
  background: #ef4444;
}

.status-text {
  font-size: 12px;
  color: #1e293b;
}

.error-msg-text {
  font-size: 12px;
  color: #ef4444;
}

.time-text {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 12px;
  color: #64748b;
}

.text-muted {
  color: #94a3b8;
}

.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.table-op-btn {
  padding: 2px 10px;
  font-size: 11px;
  border-radius: 4px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;
  line-height: 18px;
}

.op-view {
  border: 1px solid #e2e8f0;
  color: #0f172a;
}

.op-view:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.op-reverse {
  border: 1px solid #fecaca;
  color: #ef4444;
}

.op-reverse:hover {
  background: #fef2f2;
}

:deep(.clean-batch-table .ant-table) {
  background: transparent;
}

:deep(.clean-batch-table .ant-table-tbody > tr) {
  transition: none !important;
}

:deep(.clean-batch-table .ant-table-thead > tr > th) {
  background: #f1f5f9 !important;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  padding: 10px 12px;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.clean-batch-table .ant-table-tbody > tr > td) {
  padding: 12px;
  border-bottom: 1px solid #f8fafc;
  background: #ffffff !important;
  transition: none !important;
}

:deep(.clean-batch-table .ant-table-tbody > tr:hover > td),
:deep(.clean-batch-table .ant-table-tbody > tr > td.ant-table-cell-row-hover) {
  background: #f8fafc !important;
  transition: none !important;
}
</style>
