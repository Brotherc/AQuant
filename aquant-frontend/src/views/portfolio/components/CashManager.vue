<template>
  <div class="cash-mgmt-card">
    <div class="card-titles">
      <div class="main-title">现金账户与余额管理</div>
      <div class="sub-title">查看和管理账户现金余额，支持手动调整或同步最新数据。</div>
    </div>

    <div v-if="cashAccounts.length" class="cash-account-list">
      <div v-for="item in cashAccounts" :key="item.id" class="cash-box-item">
        <div class="box-top-row">
          <span class="acc-name">{{ getAccountName(item.accountId) }}</span>
          <span class="currency-tag">{{ item.currency || 'CNY' }}</span>
        </div>

        <div class="box-metrics-row">
          <div class="metric-col">
            <div class="m-label">现金总余额</div>
            <div class="m-val">{{ formatMoney(item.balance ?? item.totalBalance ?? 0) }}</div>
          </div>
          <div class="metric-col">
            <div class="m-label">可用余额</div>
            <div class="m-val">{{ formatMoney(item.availableBalance ?? 0) }}</div>
          </div>
          <div class="metric-col">
            <div class="m-label">冻结金额</div>
            <div class="m-val">{{ formatMoney(item.frozenAmount ?? item.frozenBalance ?? 0) }}</div>
          </div>
        </div>

        <div class="box-bottom-row">
          <span class="time-text">最后更新时间 {{ item.updatedAt || item.updateTime || '-' }}</span>
          <button class="btn-adjust-cash" @click="openUpdateCashModal(item)">
            <svg viewBox="0 0 1024 1024" width="12" height="12" fill="currentColor">
              <path d="M759.2 383c-4.9-18.1-12.2-35.3-21.7-51.2l61.6-61.6c4.7-4.7 4.7-12.3 0-17l-67.9-67.9c-4.7-4.7-12.3-4.7-17 0l-61.6 61.6c-15.9-9.5-33.1-16.8-51.2-21.7l-15.4-85.8C585 133 578 128 570 128h-96c-8 0-15 5-16 13.4l-15.4 85.8c-18.1 4.9-35.3 12.2-51.2 21.7l-61.6-61.6c-4.7-4.7-12.3-4.7-17 0l-67.9 67.9c-4.7 4.7-4.7 12.3 0 17l61.6 61.6c-9.5 15.9-16.8 33.1-21.7 51.2L133 439c-8.4 1-13.4 8-13.4 16v96c0 8 5 15 13.4 16l85.8 15.4c4.9 18.1 12.2 35.3 21.7 51.2l-61.6 61.6c-4.7 4.7-4.7 12.3 0 17l67.9 67.9c4.7 4.7 12.3 4.7 17 0l61.6-61.6c15.9 9.5 33.1 16.8 51.2 21.7l15.4 85.8c1 8.4 8 13.4 16 13.4h96c8 0 15-5 16-13.4l15.4-85.8c18.1-4.9 35.3-12.2 51.2-21.7l61.6 61.6c4.7 4.7 12.3 4.7 17 0l67.9-67.9c4.7-4.7 4.7-12.3 0-17l-61.6-61.6c9.5-15.9 16.8-33.1 21.7-51.2l85.8-15.4c8.4-1 13.4-8 13.4-16v-96c0-8-5-15-13.4-16l-85.8-15.4zM512 640c-70.7 0-128-57.3-128-128s57.3-128 128-128 128 57.3 128 128-57.3 128-128 128z" />
            </svg>
            <span>调整现金余额</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 空数据展示 -->
    <div v-else class="cash-empty-wrap">
      <a-empty description="暂无现金记录">
        <template #extra>
          <a-button type="primary" size="small" @click="openUpdateCashModal()">
            录入现金余额
          </a-button>
        </template>
      </a-empty>
    </div>

    <!-- 弹窗 -->
    <a-modal
      :visible="modalVisible"
      title="调整现金余额"
      :confirm-loading="submitting"
      width="440px"
      centered
      wrap-class-name="mgmt-form-modal-wrap"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSaveCash"
      @cancel="modalVisible = false"
    >
      <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical" style="margin-top: 12px;">
        <a-form-item label="所属账户" name="accountId">
          <a-select v-model:value="formState.accountId" placeholder="请选择账户">
            <a-select-option v-for="acc in accounts" :key="acc.id" :value="acc.id">
              {{ acc.accountName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="币种" name="currency">
          <a-select v-model:value="formState.currency">
            <a-select-option value="CNY">人民币 (CNY)</a-select-option>
            <a-select-option value="USD">美元 (USD)</a-select-option>
            <a-select-option value="HKD">港币 (HKD)</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="最新现金余额" name="balance">
          <a-input-number
            v-model:value="formState.balance"
            :min="0"
            :step="1000"
            style="width: 100%;"
            placeholder="输入当前现金总余额"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import type {
  PortfolioCashVO,
  BrokerAccountVO,
  CashBalanceUpdateRequest
} from '@/types/portfolio';
import { updateCashBalance } from '@/api/portfolio';

const props = defineProps<{
  portfolioId?: number;
  cashList: PortfolioCashVO[];
  accounts: BrokerAccountVO[];
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
}>();

const modalVisible = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const formState = reactive<CashBalanceUpdateRequest>({
  accountId: undefined as any,
  currency: 'CNY',
  balance: undefined as any
});

const rules = {
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }],
  balance: [{ required: true, message: '请输入现金余额', trigger: 'blur' }]
};

const cashAccounts = computed(() => {
  return props.cashList || [];
});

const defaultAccountName = computed(() => {
  return props.accounts.length > 0 && props.accounts[0] ? props.accounts[0].accountName : '-';
});

const getAccountName = (accountId: number): string => {
  const acc = props.accounts.find((a) => a.id === accountId);
  return acc ? acc.accountName : `账户 #${accountId}`;
};

const formatMoney = (val?: number | null): string => {
  if (val === undefined || val === null || isNaN(val)) return '0.00';
  return Number(val).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const openUpdateCashModal = (cash?: PortfolioCashVO) => {
  if (cash) {
    formState.accountId = cash.accountId;
    formState.currency = cash.currency || 'CNY';
    formState.balance = Number(cash.balance ?? cash.totalBalance);
  } else {
    formState.accountId = props.accounts.length > 0 && props.accounts[0] ? props.accounts[0].id : (undefined as any);
    formState.currency = 'CNY';
    formState.balance = undefined as any;
  }
  modalVisible.value = true;
};

const handleSaveCash = async () => {
  if (!props.portfolioId) {
    message.warning('请先选择投资组合');
    return;
  }
  try {
    await formRef.value?.validate();
    submitting.value = true;
    await updateCashBalance(props.portfolioId, formState);
    message.success('现金余额已更新');
    modalVisible.value = false;
    emit('refresh');
  } catch (err: any) {
    if (err?.message) message.error(err.message);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.cash-mgmt-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 18px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  height: 100%;
}

.card-titles {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
}

.main-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.sub-title {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 3px;
}

.cash-account-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cash-box-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
}

.box-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.acc-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.currency-tag {
  font-size: 10px;
  background: #ffffff;
  color: #64748b;
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.box-metrics-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.metric-col {
  display: flex;
  flex-direction: column;
}

.m-label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 2px;
}

.m-val {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.box-bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f5f9;
  padding-top: 10px;
}

.time-text {
  font-size: 11px;
  color: #94a3b8;
}

.btn-adjust-cash {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  padding: 3px 10px;
  font-size: 11px;
  color: #0f172a;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s ease;
}

.btn-adjust-cash:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

:global(.mgmt-form-modal-wrap .ant-modal-footer) {
  border-top: none !important;
  padding: 10px 24px 20px 24px !important;
}
</style>
