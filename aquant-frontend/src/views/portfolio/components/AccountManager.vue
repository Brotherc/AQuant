<template>
  <div class="account-manager-cards">
    <!-- 1. 投资组合管理卡片 -->
    <div class="mgmt-card portfolio-mgmt-card">
      <div class="mgmt-header">
        <div class="header-titles">
          <div class="main-title">投资组合管理</div>
          <div class="sub-title">创建和管理您的投资组合，一个组合可以关联多个券商账户。</div>
        </div>
        <button class="btn-black-create" @click="openPortfolioModal()">
          <span class="plus-icon">+</span>
          <span>新建组合</span>
        </button>
      </div>

      <a-table
        :columns="portfolioColumns"
        :data-source="portfolioList"
        :loading="loading"
        row-key="id"
        :pagination="false"
        size="middle"
        class="clean-mgmt-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <div class="portfolio-name-row">
              <span class="name-text">{{ record.name }}</span>
              <span v-if="record.isDefault === 1 || record.defaultPortfolio" class="default-badge">默认</span>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'benchmark'">
            <span class="text-muted">{{ record.benchmarkCode || '--' }}</span>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            <span class="time-text">{{ record.createTime || record.createdAt || '--' }}</span>
          </template>

          <template v-else-if="column.dataIndex === 'action'">
            <div class="action-btn-group">
              <button class="table-op-btn op-edit" @click="openPortfolioModal(record)">
                编辑
              </button>
              <a-dropdown
                v-if="record.isDefault !== 1 && !record.defaultPortfolio || portfolioList.length > 1"
                :trigger="['click']"
              >
                <button class="table-op-btn op-more">···</button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item
                      v-if="record.isDefault !== 1 && !record.defaultPortfolio"
                      key="setDefault"
                      @click="handleSetDefaultPortfolio(record)"
                    >
                      设为默认组合
                    </a-menu-item>
                    <a-menu-item
                      v-if="portfolioList.length > 1"
                      key="delete"
                      danger
                      @click="handleDeletePortfolio(record.id)"
                    >
                      删除组合
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 2. 券商账户管理卡片 -->
    <div class="mgmt-card account-mgmt-card" style="margin-top: 16px;">
      <div class="mgmt-header">
        <div class="header-titles">
          <div class="main-title">券商账户管理</div>
          <div class="sub-title">添加您的券商资金账户，系统将自动关联交易数据和持仓信息。</div>
        </div>
        <button class="btn-black-create" @click="openAccountModal()">
          <span class="plus-icon">+</span>
          <span>添加券商账户</span>
        </button>
      </div>

      <a-table
        :columns="accountColumns"
        :data-source="accounts"
        :loading="loading"
        row-key="id"
        :pagination="false"
        size="middle"
        class="clean-mgmt-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'accountType'">
            <span class="account-type-tag">
              {{ ACCOUNT_TYPE_LABELS[record.accountType as AccountType] || '证券账户' }}
            </span>
          </template>

          <template v-else-if="column.dataIndex === 'accountNoMasked'">
            <span class="text-muted">{{ record.accountNoMasked || record.accountNo || '--' }}</span>
          </template>

          <template v-else-if="column.dataIndex === 'brokerCode'">
            <span class="text-muted">{{ record.brokerCode || getBrokerCode(record.brokerName) }}</span>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <div class="status-dot-wrap">
              <span class="status-dot dot-green"></span>
              <span class="status-text">正常</span>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'action'">
            <div class="action-btn-group">
              <button class="table-op-btn op-edit" @click="openAccountModal(record)">
                编辑
              </button>
              <a-popconfirm
                title="确定要删除该券商账户吗？"
                ok-text="删除"
                cancel-text="取消"
                ok-type="danger"
                @confirm="handleDeleteAccount(record.id)"
              >
                <button class="table-op-btn op-delete">
                  删除
                </button>
              </a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 组合弹窗 -->
    <a-modal
      :visible="portfolioModalVisible"
      :title="editingPortfolio ? '编辑投资组合' : '新建投资组合'"
      :confirm-loading="portfolioSubmitting"
      width="460px"
      centered
      wrap-class-name="mgmt-form-modal-wrap"
      ok-text="确认"
      cancel-text="取消"
      @ok="handleSavePortfolio"
      @cancel="portfolioModalVisible = false"
    >
      <a-form ref="portfolioFormRef" :model="portfolioForm" :rules="portfolioRules" layout="vertical">
        <a-form-item label="组合名称" name="name">
          <a-input v-model:value="portfolioForm.name" placeholder="如 默认组合" />
        </a-form-item>
        <a-form-item label="本位币" name="baseCurrency">
          <a-select v-model:value="portfolioForm.baseCurrency">
            <a-select-option value="CNY">人民币 (CNY)</a-select-option>
            <a-select-option value="USD">美元 (USD)</a-select-option>
            <a-select-option value="HKD">港币 (HKD)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="基准指数" name="benchmarkCode">
          <a-input v-model:value="portfolioForm.benchmarkCode" placeholder="如 000300 (沪深300，选填)" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 券商账户弹窗 -->
    <a-modal
      :visible="accountModalVisible"
      :title="editingAccount ? '编辑券商账户' : '添加券商账户'"
      :confirm-loading="accountSubmitting"
      width="460px"
      centered
      wrap-class-name="mgmt-form-modal-wrap"
      ok-text="确认"
      cancel-text="取消"
      @ok="handleSaveAccount"
      @cancel="accountModalVisible = false"
    >
      <a-form ref="accountFormRef" :model="accountForm" :rules="accountRules" layout="vertical">
        <a-form-item label="账户名称" name="accountName">
          <a-input v-model:value="accountForm.accountName" placeholder="如 招商证券主账户" />
        </a-form-item>
        <a-form-item label="券商机构" name="brokerName">
          <a-input v-model:value="accountForm.brokerName" placeholder="如 招商证券" />
        </a-form-item>
        <a-form-item label="账户类型" name="accountType">
          <a-select v-model:value="accountForm.accountType">
            <a-select-option value="SECURITIES">证券账户</a-select-option>
            <a-select-option value="FUND">基金账户</a-select-option>
            <a-select-option value="MARGIN">信用/两融账户</a-select-option>
            <a-select-option value="FUTURES">期货账户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="券商代码 (选填)" name="brokerCode">
          <a-input v-model:value="accountForm.brokerCode" placeholder="如 CMS" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance, TableColumnType } from 'ant-design-vue';
import type {
  UserPortfolioVO,
  BrokerAccountVO,
  AccountType,
  PortfolioCreateRequest,
  PortfolioUpdateRequest,
  BrokerAccountCreateRequest,
  BrokerAccountUpdateRequest
} from '@/types/portfolio';
import { ACCOUNT_TYPE_LABELS } from '@/types/portfolio';
import {
  savePortfolio,
  deletePortfolio,
  saveAccount,
  deleteAccount
} from '@/api/portfolio';

const props = defineProps<{
  portfolioList: UserPortfolioVO[];
  currentPortfolioId?: number;
  accounts: BrokerAccountVO[];
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
}>();

const portfolioColumns: TableColumnType<UserPortfolioVO>[] = [
  { title: '组合名称', dataIndex: 'name', key: 'name', width: 220 },
  { title: '本位币', dataIndex: 'baseCurrency', key: 'baseCurrency', width: 120 },
  { title: '基准指数', dataIndex: 'benchmark', key: 'benchmark', width: 140 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 200 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 130, align: 'center' }
];

const accountColumns: TableColumnType<BrokerAccountVO>[] = [
  { title: '账户名称', dataIndex: 'accountName', key: 'accountName', width: 180 },
  { title: '券商机构', dataIndex: 'brokerName', key: 'brokerName', width: 140 },
  { title: '账户类型', dataIndex: 'accountType', key: 'accountType', width: 120 },
  { title: '资金账号', dataIndex: 'accountNoMasked', key: 'accountNoMasked', width: 130 },
  { title: '券商代码', dataIndex: 'brokerCode', key: 'brokerCode', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 130, align: 'center' }
];

// 组合表单
const portfolioModalVisible = ref(false);
const portfolioSubmitting = ref(false);
const editingPortfolio = ref<UserPortfolioVO | null>(null);
const portfolioFormRef = ref<FormInstance>();
const portfolioForm = reactive<PortfolioCreateRequest & PortfolioUpdateRequest>({
  name: '',
  baseCurrency: 'CNY',
  benchmarkCode: ''
});

const portfolioRules = {
  name: [{ required: true, message: '请输入组合名称', trigger: 'blur' }]
};

const openPortfolioModal = (portfolio?: UserPortfolioVO) => {
  editingPortfolio.value = portfolio || null;
  if (portfolio) {
    portfolioForm.name = portfolio.name;
    portfolioForm.baseCurrency = portfolio.baseCurrency || 'CNY';
    portfolioForm.benchmarkCode = portfolio.benchmarkCode || '';
  } else {
    portfolioForm.name = '';
    portfolioForm.baseCurrency = 'CNY';
    portfolioForm.benchmarkCode = '';
  }
  portfolioModalVisible.value = true;
};

const handleSavePortfolio = async () => {
  try {
    await portfolioFormRef.value?.validate();
    portfolioSubmitting.value = true;
    await savePortfolio({
      id: editingPortfolio.value?.id,
      name: portfolioForm.name,
      baseCurrency: portfolioForm.baseCurrency,
      benchmarkCode: portfolioForm.benchmarkCode
    });
    message.success(editingPortfolio.value ? '组合修改成功' : '组合创建成功');
    portfolioModalVisible.value = false;
    emit('refresh');
  } catch (err: any) {
    if (err?.message) message.error(err.message);
  } finally {
    portfolioSubmitting.value = false;
  }
};

const handleSetDefaultPortfolio = async (portfolio: UserPortfolioVO) => {
  try {
    await savePortfolio({
      id: portfolio.id,
      name: portfolio.name,
      baseCurrency: portfolio.baseCurrency,
      defaultPortfolio: true
    });
    message.success('已设为默认组合');
    emit('refresh');
  } catch (err: any) {
    message.error(err?.message || '设置失败');
  }
};

const handleDeletePortfolio = async (portfolioId: number) => {
  try {
    await deletePortfolio(portfolioId);
    message.success('组合已删除');
    emit('refresh');
  } catch (err: any) {
    message.error(err?.message || '删除失败');
  }
};

// 账户表单
const accountModalVisible = ref(false);
const accountSubmitting = ref(false);
const editingAccount = ref<BrokerAccountVO | null>(null);
const accountFormRef = ref<FormInstance>();
const accountForm = reactive<BrokerAccountCreateRequest & BrokerAccountUpdateRequest>({
  portfolioId: 0,
  accountName: '',
  brokerName: '',
  accountType: 'SECURITIES',
  brokerCode: ''
});

const accountRules = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }]
};

const openAccountModal = (acc?: BrokerAccountVO) => {
  editingAccount.value = acc || null;
  if (acc) {
    accountForm.portfolioId = acc.portfolioId;
    accountForm.accountName = acc.accountName;
    accountForm.brokerName = acc.brokerName || '';
    accountForm.accountType = acc.accountType || 'SECURITIES';
    accountForm.brokerCode = acc.brokerCode || '';
  } else {
    accountForm.portfolioId = props.currentPortfolioId || 0;
    accountForm.accountName = '';
    accountForm.brokerName = '';
    accountForm.accountType = 'SECURITIES';
    accountForm.brokerCode = '';
  }
  accountModalVisible.value = true;
};

const handleSaveAccount = async () => {
  try {
    await accountFormRef.value?.validate();
    accountSubmitting.value = true;
    await saveAccount({
      id: editingAccount.value?.id,
      portfolioId: props.currentPortfolioId || 1,
      accountName: accountForm.accountName,
      brokerName: accountForm.brokerName,
      accountType: accountForm.accountType,
      brokerCode: accountForm.brokerCode || getBrokerCode(accountForm.brokerName)
    });
    message.success(editingAccount.value ? '账户修改成功' : '券商账户创建成功');
    accountModalVisible.value = false;
    emit('refresh');
  } catch (err: any) {
    if (err?.message) message.error(err.message);
  } finally {
    accountSubmitting.value = false;
  }
};

const handleDeleteAccount = async (accountId: number) => {
  try {
    await deleteAccount(accountId);
    message.success('券商账户已删除');
    emit('refresh');
  } catch (err: any) {
    message.error(err?.message || '删除失败');
  }
};

const getBrokerCode = (name?: string): string => {
  if (!name) return 'CMS';
  if (name.includes('招商')) return 'CMS';
  if (name.includes('华泰')) return 'HTSC';
  if (name.includes('中信')) return 'CITICS';
  if (name.includes('广发')) return 'GF';
  return 'BROKER';
};
</script>

<style scoped>
.account-manager-cards {
  display: flex;
  flex-direction: column;
}

.mgmt-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 18px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.mgmt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-titles {
  display: flex;
  flex-direction: column;
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

.btn-black-create {
  background: #0f172a;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: background 0.15s;
}

.btn-black-create:hover {
  background: #1e293b;
}

.plus-icon {
  font-size: 14px;
  font-weight: bold;
}

.portfolio-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-text {
  font-weight: 600;
  color: #0f172a;
  font-size: 13px;
}

.default-badge {
  font-size: 11px;
  background: #f1f5f9;
  color: #64748b;
  padding: 1px 6px;
  border-radius: 4px;
}

.time-text {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 12px;
  color: #64748b;
}

.account-type-tag {
  font-size: 11px;
  background: #f8fafc;
  color: #475569;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
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

.status-text {
  font-size: 12px;
  color: #1e293b;
}

.text-muted {
  color: #94a3b8;
  font-size: 12px;
}

.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.table-op-btn {
  padding: 2px 10px;
  font-size: 12px;
  border-radius: 4px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;
  line-height: 18px;
}

.op-edit {
  border: 1px solid #e2e8f0;
  color: #0f172a;
}

.op-edit:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.op-more {
  border: 1px solid #e2e8f0;
  color: #64748b;
  padding: 2px 6px;
}

.op-delete {
  border: 1px solid #fecaca;
  color: #ef4444;
}

.op-delete:hover {
  background: #fef2f2;
}

:deep(.clean-mgmt-table .ant-table) {
  background: transparent;
}

:deep(.clean-mgmt-table .ant-table-tbody > tr) {
  transition: none !important;
}

:deep(.clean-mgmt-table .ant-table-thead > tr > th) {
  background: #f1f5f9 !important;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  padding: 10px 12px;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.clean-mgmt-table .ant-table-tbody > tr > td) {
  padding: 10px 12px;
  border-bottom: 1px solid #f8fafc;
  background: #ffffff !important;
  transition: none !important;
}

:deep(.clean-mgmt-table .ant-table-tbody > tr:hover > td),
:deep(.clean-mgmt-table .ant-table-tbody > tr > td.ant-table-cell-row-hover) {
  background: #f8fafc !important;
  transition: none !important;
}

:global(.mgmt-form-modal-wrap .ant-modal-footer) {
  border-top: none !important;
  padding: 10px 24px 20px 24px !important;
}
</style>
