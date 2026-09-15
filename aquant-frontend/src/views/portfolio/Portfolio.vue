<template>
  <div class="portfolio-page-container">
    <!-- 1. 顶部步骤向导横幅 (4 步流程卡片置顶) -->
    <PortfolioStepBanner
      :has-portfolio="portfolioList.length > 0"
      :has-account="accounts.length > 0"
      :has-trade="trades.length > 0 || !!tradeTotal"
      @step-click="handleStepClick"
      @cta-click="handleCtaClick"
    />

    <!-- 2. 二级导航 Tab 与筛选选择器栏 -->
    <div class="portfolio-nav-tabs-bar">
      <div class="portfolio-nav-tabs">
        <button
          v-for="tab in navTabs"
          :key="tab.key"
          class="nav-tab-item"
          :class="{ 'is-active': activeTabKey === tab.key }"
          @click="activeTabKey = tab.key"
        >
          <span class="tab-label">{{ tab.label }}</span>
          <span v-if="activeTabKey === tab.key" class="tab-underline"></span>
        </button>
      </div>

      <div class="header-right-filters">
        <div class="filter-select-group">
          <span class="select-label">当前组合</span>
          <a-select
            v-model:value="currentPortfolioId"
            placeholder="请选择组合"
            class="header-select portfolio-select"
            :loading="portfolioLoading"
            @change="handlePortfolioChange"
          >
            <a-select-option
              v-for="p in portfolioList"
              :key="p.id"
              :value="p.id"
            >
              {{ p.name }}{{ p.isDefault === 1 || p.defaultPortfolio ? ' (默认)' : '' }}
            </a-select-option>
          </a-select>
        </div>

        <div class="filter-select-group">
          <span class="select-label">券商账户</span>
          <a-select
            v-model:value="selectedAccountId"
            placeholder="全部券商账户"
            allow-clear
            class="header-select account-select"
            @change="handleAccountChange"
          >
            <a-select-option
              v-for="acc in accounts"
              :key="acc.id"
              :value="acc.id"
            >
              {{ acc.accountName }}
            </a-select-option>
          </a-select>
        </div>
      </div>
    </div>

    <!-- 4. Tab 视图内容 -->
    <div class="tab-content-container">
      <!-- Tab 1: 总览 -->
      <div v-show="activeTabKey === 'overview'" class="tab-pane-overview">
        <a-row :gutter="[16, 16]">
          <!-- 左侧主区域 (17/24) -->
          <a-col :xs="24" :lg="17">
            <!-- 6 张指标卡片 -->
            <PortfolioSummary
              :summary="summary"
              :loading="summaryLoading"
            />

            <!-- 持仓明细表格 -->
            <PositionTable
              :positions="positions"
              :accounts="accounts"
              :loading="positionLoading"
              @quick-trade="handleQuickTrade"
            />
          </a-col>

          <!-- 右侧侧边栏 (7/24) -->
          <a-col :xs="24" :lg="7">
            <div class="overview-side-charts">
              <!-- 资产配置 -->
              <AssetAllocationPie
                :summary="summary"
                :loading="summaryLoading"
              />

              <!-- 资产走势 -->
              <AssetHistoryChart
                :snapshots="snapshots"
                :loading="snapshotLoading"
                :selected-account-id="selectedAccountId"
              />
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- Tab 2: 交易流水 -->
      <div v-show="activeTabKey === 'trades'" class="tab-pane-trades">
        <a-row :gutter="[16, 16]">
          <!-- 左侧主区域 (17/24) -->
          <a-col :xs="24" :lg="17">
            <!-- 5 张流水统计卡片 -->
            <TradeSummaryCards
              :trades="trades"
              :total-trades-count="tradeTotal"
              :last-import-time="latestBatchTime"
            />

            <!-- 流水表格卡片 -->
            <TradeTable
              :trades="trades"
              :accounts="accounts"
              :loading="tradeLoading"
              :total="tradeTotal"
              :current="tradePage"
              :page-size="tradePageSize"
              @change-page="handleTradePageChange"
              @open-trade-modal="openTradeModal"
              @edit-trade="handleEditTrade"
              @refresh="handleTradeRefresh"
            />
          </a-col>

          <!-- 右侧侧边栏 (7/24) -->
          <a-col :xs="24" :lg="7">
            <TradeSideActions
              :batches="importBatches"
              @view-all-batches="activeTabKey = 'import'"
              @open-trade-modal="openTradeModal"
              @go-to-import="activeTabKey = 'import'"
              @download-template="handleDownloadTemplate"
              @show-help="activeTabKey = 'import'"
            />
          </a-col>
        </a-row>
      </div>

      <!-- Tab 3: 账户与资金 -->
      <div v-show="activeTabKey === 'account'" class="tab-pane-account">
        <!-- 顶部绑定横幅 -->
        <AccountFundBanner
          :has-portfolio="portfolioList.length > 0"
          :has-account="accounts.length > 0"
        />

        <a-row :gutter="[16, 16]">
          <!-- 左侧投资组合与券商账户 (14/24) -->
          <a-col :xs="24" :lg="14">
            <AccountManager
              :portfolio-list="portfolioList"
              :current-portfolio-id="currentPortfolioId"
              :accounts="accounts"
              :loading="accountLoading"
              @refresh="handleAccountManagerRefresh"
            />
          </a-col>

          <!-- 右侧现金管理 (10/24) -->
          <a-col :xs="24" :lg="10">
            <CashManager
              :portfolio-id="currentPortfolioId"
              :cash-list="cashList"
              :accounts="accounts"
              :loading="cashLoading"
              @refresh="loadCashData"
            />
          </a-col>
        </a-row>
      </div>

      <!-- Tab 4: 数据导入 -->
      <div v-show="activeTabKey === 'import'" class="tab-pane-import">
        <a-row :gutter="[16, 16]">
          <!-- 左侧导入批次记录表格 (17/24) -->
          <a-col :xs="24" :lg="17">
            <ImportBatchTable
              :batches="importBatches"
              :accounts="accounts"
              :loading="importBatchLoading"
              @refresh="loadImportBatches"
              @view-batch="handleViewBatch"
            />
          </a-col>

          <!-- 右侧上传卡片 (7/24) -->
          <a-col :xs="24" :lg="7">
            <ImportUploadSection
              :accounts="accounts"
              :selected-account-id="selectedAccountId"
              @file-selected="handleFileUpload"
              @download-template="handleDownloadTemplate"
            />
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- 期初建仓弹窗 -->
    <PositionInitModal
      v-model:visible="initModalVisible"
      :portfolio-id="currentPortfolioId"
      :accounts="accounts"
      @success="handleInitSuccess"
    />

    <!-- 手工流水录入弹窗 -->
    <TradeFormModal
      v-model:visible="tradeModalVisible"
      :portfolio-id="currentPortfolioId"
      :accounts="accounts"
      :initial-data="quickTradeInitialData"
      @success="handleTradeSuccess"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, watch } from 'vue';
import { message } from 'ant-design-vue';

import type {
  UserPortfolioVO,
  BrokerAccountVO,
  PortfolioSummaryVO,
  PortfolioPositionVO,
  PortfolioSnapshotVO,
  PortfolioTradeVO,
  PortfolioCashVO,
  PortfolioImportBatchVO,
  TradePageQuery,
  TradeType,
  AssetType
} from '@/types/portfolio';

import {
  getPortfolioList,
  getAccountList,
  getPortfolioSummary,
  getPositionList,
  getSnapshotList,
  getTradePage,
  getCashList,
  getImportBatches,
  downloadTradeImportTemplate
} from '@/api/portfolio';

import PortfolioStepBanner from './components/PortfolioStepBanner.vue';
import PortfolioSummary from './components/PortfolioSummary.vue';
import PositionTable from './components/PositionTable.vue';
import AssetHistoryChart from './components/AssetHistoryChart.vue';
import AssetAllocationPie from './components/AssetAllocationPie.vue';

import TradeSummaryCards from './components/TradeSummaryCards.vue';
import TradeTable from './components/TradeTable.vue';
import TradeSideActions from './components/TradeSideActions.vue';

import AccountFundBanner from './components/AccountFundBanner.vue';
import AccountManager from './components/AccountManager.vue';
import CashManager from './components/CashManager.vue';

import ImportUploadSection from './components/ImportUploadSection.vue';
import ImportBatchTable from './components/ImportBatchTable.vue';

import PositionInitModal from './components/PositionInitModal.vue';
import TradeFormModal from './components/TradeFormModal.vue';

// 页面状态
const activeTabKey = ref('overview');
const currentPortfolioId = ref<number | undefined>(undefined);
const selectedAccountId = ref<number | undefined>(undefined);

const navTabs = [
  { key: 'overview', label: '总览' },
  { key: 'trades', label: '交易流水' },
  { key: 'account', label: '账户与资金' },
  { key: 'import', label: '数据导入' }
];

// 数据状态
const portfolioList = ref<UserPortfolioVO[]>([]);
const accounts = ref<BrokerAccountVO[]>([]);
const portfolioLoading = ref(false);
const accountLoading = ref(false);

const summary = ref<PortfolioSummaryVO | null>(null);
const summaryLoading = ref(false);
const positions = ref<PortfolioPositionVO[]>([]);
const positionLoading = ref(false);
const snapshots = ref<PortfolioSnapshotVO[]>([]);
const snapshotLoading = ref(false);

const trades = ref<PortfolioTradeVO[]>([]);
const tradeLoading = ref(false);
const tradeTotal = ref(0);
const tradePage = ref(1);
const tradePageSize = ref(10);
const tradeQueryParams = ref<TradePageQuery>({});

const cashList = ref<PortfolioCashVO[]>([]);
const cashLoading = ref(false);
const importBatches = ref<PortfolioImportBatchVO[]>([]);
const importBatchLoading = ref(false);

// 弹窗
const initModalVisible = ref(false);
const tradeModalVisible = ref(false);
const quickTradeInitialData = ref<{
  accountId?: number;
  symbol?: string;
  symbolName?: string;
  assetType?: AssetType;
  tradeType?: TradeType;
} | undefined>(undefined);

const latestBatchTime = computed(() => {
  if (importBatches.value && importBatches.value.length > 0 && importBatches.value[0]) {
    const first = importBatches.value[0];
    return first.createdAt || first.createTime || '-';
  }
  return '-';
});

// 加载投资组合列表
const loadPortfolios = async () => {
  portfolioLoading.value = true;
  try {
    const list = await getPortfolioList();
    portfolioList.value = list || [];
    if (list && list.length > 0) {
      if (!currentPortfolioId.value || !list.some((item) => item.id === currentPortfolioId.value)) {
        const defaultPortfolio = list.find((item) => item.isDefault === 1 || item.defaultPortfolio) || list[0];
        if (defaultPortfolio) {
          currentPortfolioId.value = defaultPortfolio.id;
        }
      }
      await loadAccounts();
    }
  } catch (err: any) {
    message.error(err?.message || '获取投资组合失败');
  } finally {
    portfolioLoading.value = false;
  }
};

// 加载券商账户列表
const loadAccounts = async () => {
  if (!currentPortfolioId.value) return;
  accountLoading.value = true;
  try {
    const accList = await getAccountList(currentPortfolioId.value);
    accounts.value = accList || [];
    if (selectedAccountId.value && !accounts.value.some((a) => a.id === selectedAccountId.value)) {
      selectedAccountId.value = undefined;
    }
  } catch (err: any) {
    message.error(err?.message || '获取券商账户失败');
  } finally {
    accountLoading.value = false;
  }
};

// 加载持仓总览数据
const loadOverviewData = async () => {
  if (!currentPortfolioId.value) return;
  summaryLoading.value = true;
  positionLoading.value = true;
  snapshotLoading.value = true;

  try {
    const [sumRes, posRes, snapRes] = await Promise.all([
      getPortfolioSummary(currentPortfolioId.value, selectedAccountId.value),
      getPositionList(currentPortfolioId.value, selectedAccountId.value),
      getSnapshotList(currentPortfolioId.value, selectedAccountId.value)
    ]);
    summary.value = sumRes || null;
    positions.value = posRes || [];
    snapshots.value = snapRes || [];
  } catch (err: any) {
    message.error(err?.message || '获取概览数据失败');
  } finally {
    summaryLoading.value = false;
    positionLoading.value = false;
    snapshotLoading.value = false;
  }
};

// 加载交易流水数据
const loadTradeData = async () => {
  if (!currentPortfolioId.value) return;
  tradeLoading.value = true;
  try {
    const query: TradePageQuery = {
      ...tradeQueryParams.value,
      accountId: selectedAccountId.value || tradeQueryParams.value.accountId,
      page: tradePage.value - 1,
      size: tradePageSize.value
    };
    const res = await getTradePage(currentPortfolioId.value, query);
    trades.value = res?.content || [];
    tradeTotal.value = res?.totalElements || 0;
  } catch (err: any) {
    message.error(err?.message || '获取交易流水失败');
  } finally {
    tradeLoading.value = false;
  }
};

// 加载现金
const loadCashData = async () => {
  if (!currentPortfolioId.value) return;
  cashLoading.value = true;
  try {
    const list = await getCashList(currentPortfolioId.value, selectedAccountId.value);
    cashList.value = list || [];
  } catch (err: any) {
    message.error(err?.message || '获取现金失败');
  } finally {
    cashLoading.value = false;
  }
};

// 加载导入批次
const loadImportBatches = async () => {
  if (!currentPortfolioId.value) return;
  importBatchLoading.value = true;
  try {
    const list = await getImportBatches(currentPortfolioId.value, selectedAccountId.value);
    importBatches.value = list || [];
  } catch (err: any) {
    message.error(err?.message || '获取批次失败');
  } finally {
    importBatchLoading.value = false;
  }
};

const handlePortfolioChange = async () => {
  selectedAccountId.value = undefined;
  await loadAccounts();
  await loadActiveTabData();
};

const handleAccountChange = () => {
  void loadActiveTabData();
};

// 每个页签只加载自身可见区域依赖的数据，避免进入页面时请求所有接口。
const loadActiveTabData = async () => {
  if (activeTabKey.value === 'overview') {
    await loadOverviewData();
  } else if (activeTabKey.value === 'trades') {
    await Promise.all([loadTradeData(), loadImportBatches()]);
  } else if (activeTabKey.value === 'account') {
    await loadCashData();
  } else if (activeTabKey.value === 'import') {
    await loadImportBatches();
  }
};

watch(activeTabKey, () => {
  void loadActiveTabData();
});

const handleStepClick = (step: number) => {
  if (step === 1 || step === 2) {
    activeTabKey.value = 'account';
  } else if (step === 3) {
    activeTabKey.value = 'import';
  } else {
    activeTabKey.value = 'overview';
  }
};

const handleCtaClick = () => {
  if (!portfolioList.value || portfolioList.value.length === 0) {
    // 尚未创建组合 -> 跳转到 账户与资金
    activeTabKey.value = 'account';
  } else if (!accounts.value || accounts.value.length === 0) {
    // 尚未添加券商账户 -> 跳转到 账户与资金
    activeTabKey.value = 'account';
  } else if (!trades.value || (trades.value.length === 0 && !tradeTotal.value)) {
    // 尚未导入交易流水 -> 跳转到 数据导入
    activeTabKey.value = 'import';
  } else {
    // 全部设置已完成 -> 跳转到 总览查看持仓
    activeTabKey.value = 'overview';
  }
};

const handleTradePageChange = (params: { current: number; pageSize: number; query: TradePageQuery }) => {
  tradePage.value = params.current;
  tradePageSize.value = params.pageSize;
  tradeQueryParams.value = params.query;
  loadTradeData();
};

const handleTradeRefresh = () => {
  loadTradeData();
};

const handleAccountManagerRefresh = async () => {
  await loadPortfolios();
  await loadActiveTabData();
};

const openTradeModal = () => {
  quickTradeInitialData.value = undefined;
  tradeModalVisible.value = true;
};

const handleQuickTrade = (pos: PortfolioPositionVO, type: TradeType) => {
  quickTradeInitialData.value = {
    accountId: pos.accountId,
    symbol: pos.symbol,
    symbolName: pos.symbolName,
    assetType: pos.assetType,
    tradeType: type
  };
  tradeModalVisible.value = true;
};

const handleEditTrade = (record: PortfolioTradeVO) => {
  quickTradeInitialData.value = {
    accountId: record.accountId,
    symbol: record.symbol,
    symbolName: record.symbolName,
    assetType: record.assetType,
    tradeType: record.tradeType
  };
  tradeModalVisible.value = true;
};

const handleInitSuccess = () => {
  void loadActiveTabData();
};

const handleTradeSuccess = () => {
  void loadActiveTabData();
};

const handleFileUpload = (file: File) => {
  message.success(`文件 ${file.name} 解析导入成功，已更新持仓`);
  void loadActiveTabData();
};

const handleDownloadTemplate = async () => {
  try {
    const template = await downloadTradeImportTemplate();
    const downloadUrl = URL.createObjectURL(template);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = 'AQuant标准交易数据导入模板.xlsx';
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(downloadUrl);
    message.success('交易数据导入模板下载成功');
  } catch (err: any) {
    message.error(err?.message || '交易数据导入模板下载失败');
  }
};

const handleViewBatch = (batch: PortfolioImportBatchVO) => {
  const isTradeTabActive = activeTabKey.value === 'trades';
  tradeQueryParams.value = {
    accountId: batch.accountId
  };
  activeTabKey.value = 'trades';
  if (isTradeTabActive) {
    loadTradeData();
  }
};

onMounted(async () => {
  await loadPortfolios();
  if (currentPortfolioId.value) {
    await loadActiveTabData();
  }
});
</script>

<style scoped>
.portfolio-page-container {
  padding: 8px 4px 40px;
  background: transparent;
}

/* 二级导航 Tab 与筛选选择器栏 */
.portfolio-nav-tabs-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #edf2f7;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 8px;
}

.portfolio-nav-tabs {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-tab-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px 8px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  background: transparent;
  line-height: 1.2;
}

.nav-tab-item:hover {
  color: #0f172a;
  background: #f8fafc;
}

.nav-tab-item.is-active {
  background: #f1f5f9;
  color: #0f172a;
  font-weight: 700;
}

.nav-tab-item .tab-label {
  font-size: 14px;
  line-height: 1.2;
}

.nav-tab-item .tab-underline {
  position: absolute;
  bottom: 0px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 2.5px;
  background: #0f172a;
  border-radius: 2px;
}

.header-right-filters {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 6px;
}

.filter-select-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.select-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.header-select {
  border-radius: 6px;
}

.portfolio-select {
  min-width: 170px;
}

.account-select {
  min-width: 170px;
}

.tab-content-container {
  min-height: 500px;
}

.overview-side-charts {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
