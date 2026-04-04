<template>
  <div class="watchlist-container">
    <!-- 分组导航标签 -->
    <div class="group-tags-header">
      <div class="group-label">自选分组：</div>
      <div 
        v-for="group in groups" 
        :key="group.id" 
        class="custom-group-tag"
        :class="{ active: activeGroupId === group.id }"
        @click="handleTabChange(group.id)"
      >
        <span class="tag-name">{{ group.name }}</span>
        <close-outlined 
          v-if="activeGroupId === group.id" 
          class="delete-group-icon"
          @click.stop="onDeleteGroup(group.id)" 
        />
      </div>
      <div class="custom-group-tag add-btn" @click="showAddGroupModal">
        ＋
      </div>
    </div>

    <!-- 自选股内容区 -->
    <div class="watchlist-content" v-if="activeGroupId">

          <a-spin :spinning="loading">
            <a-row :gutter="[16, 16]" class="card-grid">
              <a-col 
                :xs="24" :sm="24" :md="12" :lg="8" :xl="6"
                v-for="stock in stocks" 
                :key="stock.stockCode"
              >
                <div class="draggable-wrapper">
                  <a-card hoverable class="stock-card" size="small">
                    <!-- 头部：代码与名称，移除操作 -->
                    <div class="card-header">
                      <div class="stock-info">
                        <span class="stock-name">{{ stock.stockName }}</span>
                        <span class="stock-code">{{ stock.stockCode }}</span>
                      </div>
                      <a-popconfirm title="确定移除该股票吗？" @confirm="handleRemoveStock(stock.stockCode)">
                        <delete-outlined class="delete-icon" />
                      </a-popconfirm>
                    </div>
                    
                    <!-- 行情数据 -->
                    <div class="quote-info" :class="getPriceColorClass(stock.changePercent)">
                      <div class="latest-price">{{ stock.latestPrice != null ? stock.latestPrice.toFixed(2) : '-' }}</div>
                      <div class="change-percent">
                        {{ stock.changePercent > 0 ? '+' : '' }}{{ stock.changePercent != null ? stock.changePercent.toFixed(2) + '%' : '-' }}
                      </div>
                    </div>
                    
                    <!-- 迷你K线 -->
                    <div class="kline-box">
                      <MiniKlineChart :stockCode="stock.stockCode" />
                    </div>

                    <!-- 基本面数据 -->
                    <a-divider style="margin: 8px 0" />
                    <div class="fundamentals">
                      <div class="fund-item">
                        <span class="label">PE</span>
                        <span class="val">{{ stock.pe != null ? stock.pe.toFixed(2) : '-' }}</span>
                      </div>
                      <div class="fund-item">
                        <span class="label">PEG</span>
                        <span class="val">{{ stock.peg != null ? stock.peg.toFixed(2) : '-' }}</span>
                      </div>
                      <div class="fund-item">
                        <span class="label">ROE</span>
                        <span class="val">{{ stock.roe != null ? stock.roe.toFixed(2) + '%' : '-' }}</span>
                      </div>
                    </div>

                    <!-- 分红数据 -->
                    <template v-if="stock.recentDividends && stock.recentDividends.length > 0">
                      <a-divider style="margin: 6px 0" dashed />
                      <div class="dividends-wrap">
                        <div v-for="(div, idx) in stock.recentDividends" :key="idx" class="dividend-row">
                          <span class="div-date">{{ formatReportDate(div.proposalAnnouncementDate) }}</span>
                          <span class="div-text">{{ formatDividend(div) }}</span>
                        </div>
                      </div>
                    </template>
                  </a-card>
                </div>
              </a-col>
              <!-- 添加股票的常驻卡片 -->
              <a-col :xs="24" :sm="24" :md="12" :lg="8" :xl="6">
                <a-card hoverable class="add-stock-card" size="small" @click="showAddStockModal">
                  <plus-outlined class="add-icon" />
                  <div class="add-text">添加股票</div>
                </a-card>
              </a-col>
            </a-row>
          </a-spin>
    </div>

    <!-- 新增股票 Modal -->
    <a-modal v-model:visible="addStockModalVisible" title="添加自选股票" @ok="handleAddStock" :confirmLoading="addLoading">
      <a-form layout="vertical">
        <a-form-item label="股票代码" required>
          <a-input 
            v-model:value="newStockCode" 
            placeholder="请输入 6 位股票代码，例如 600519" 
            @pressEnter="handleAddStock" 
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新增分组 Modal -->
    <a-modal v-model:visible="groupModalVisible" title="新增自选分组" @ok="handleCreateGroup" :confirmLoading="groupLoading">
      <a-form :model="groupForm" layout="vertical">
        <a-form-item label="分组名称" required>
          <a-input v-model:value="groupForm.name" placeholder="请输入分组名称" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { DeleteOutlined, CloseOutlined, PlusOutlined } from '@ant-design/icons-vue';
import MiniKlineChart from './components/MiniKlineChart.vue';
import { 
  getWatchlistGroups, 
  getWatchlistStocks, 
  createWatchlistGroup, 
  deleteWatchlistGroup, 
  addStockToWatchlist, 
  removeStockFromWatchlist,
  type WatchlistGroupVO,
  type WatchlistStockVO
} from '@/api/watchlist';

const groups = ref<WatchlistGroupVO[]>([]);
const activeGroupId = ref<number>();
const stocks = ref<WatchlistStockVO[]>([]);
const loading = ref(false);
const addLoading = ref(false);
const newStockCode = ref('');

// Group Modal
const groupModalVisible = ref(false);
const groupLoading = ref(false);
const groupForm = reactive({ name: '' });

const getPriceColorClass = (changePercent: number | undefined | null) => {
  if (changePercent == null) return 'neutral';
  return changePercent > 0 ? 'up' : changePercent < 0 ? 'down' : 'neutral';
};

import type { WatchlistDividendVO } from '@/api/watchlist';
const formatDividend = (div: WatchlistDividendVO) => {
  const parts = [];
  if (div.cashDividendRatio) parts.push(`派${div.cashDividendRatio}`);
  if (div.bonusShareRatio) parts.push(`送${div.bonusShareRatio}`);
  if (div.transferShareRatio) parts.push(`转${div.transferShareRatio}`);
  
  if (parts.length === 0) {
    return div.planStatus || '不分红';
  }
  return `10${parts.join('')}`;
};

const formatReportDate = (dateStr: string | undefined | null) => {
  if (!dateStr) return '-';
  // 如果是 yyyyMMdd 格式，则转换为 yyyy-MM-dd
  if (dateStr.length === 8 && !dateStr.includes('-')) {
    return `${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}`;
  }
  return dateStr;
};

const fetchGroups = async () => {
  try {
    const res = await getWatchlistGroups();
    if (res.data.success) {
      groups.value = res.data.data;
      if (groups.value && groups.value.length > 0 && !activeGroupId.value) {
        const firstGroup = groups.value[0];
        if (firstGroup) {
          activeGroupId.value = firstGroup.id;
          fetchStocks(activeGroupId.value);
        }
      }
    }
  } catch (error) {
    console.error(error);
  }
};

const fetchStocks = async (groupId: number) => {
  loading.value = true;
  try {
    const res = await getWatchlistStocks(groupId);
    if (res.data.success) {
      stocks.value = res.data.data;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleTabChange = (key: any) => {
  activeGroupId.value = key;
  fetchStocks(key);
};

const onDeleteGroup = (targetKey: number) => {
  Modal.confirm({
    title: '确定删除该分组吗？',
    content: '删除分组将同步移除该分组下的所有自选股票。',
    onOk: async () => {
      try {
        const res = await deleteWatchlistGroup(targetKey);
        if (res.data.success) {
          message.success('删除成功');
          fetchGroups().then(() => {
            if (activeGroupId.value === targetKey) {
              const firstGroup = groups.value && groups.value.length > 0 ? groups.value[0] : undefined;
              activeGroupId.value = firstGroup ? firstGroup.id : undefined;
              if (activeGroupId.value) fetchStocks(activeGroupId.value);
              else stocks.value = [];
            }
          });
        }
      } catch (error) {
        console.error(error);
      }
    },
  });
};

const showAddGroupModal = () => {
  groupForm.name = '';
  groupModalVisible.value = true;
};

const handleCreateGroup = async () => {
  if (!groupForm.name) {
    message.warning('请输入分组名称');
    return;
  }
  groupLoading.value = true;
  try {
    const res = await createWatchlistGroup(groupForm);
    if (res.data.success) {
      message.success('创建成功');
      groupModalVisible.value = false;
      fetchGroups();
    } else {
      message.error(res.data.message || '创建失败');
    }
  } catch (error) {
    console.error(error);
  } finally {
    groupLoading.value = false;
  }
};

const addStockModalVisible = ref(false);

const showAddStockModal = () => {
  if (!activeGroupId.value) {
    message.warning('请先创建并选择一个分组');
    return;
  }
  newStockCode.value = '';
  addStockModalVisible.value = true;
};

const handleAddStock = async () => {
  if (!activeGroupId.value) {
    message.warning('请先创建分组');
    return;
  }
  if (!newStockCode.value || newStockCode.value.length < 6) {
    message.warning('请输入正确的股票代码');
    return;
  }
  addLoading.value = true;
  try {
    const res = await addStockToWatchlist({
      groupId: activeGroupId.value,
      stockCode: newStockCode.value
    });
    if (res.data.success) {
      message.success('添加成功');
      addStockModalVisible.value = false;
      newStockCode.value = '';
      fetchStocks(activeGroupId.value);
    }
  } catch (error) {
    console.error(error);
  } finally {
    addLoading.value = false;
  }
};

const handleRemoveStock = async (stockCode: string) => {
  if (!activeGroupId.value) return;
  try {
    const res = await removeStockFromWatchlist(activeGroupId.value, stockCode);
    if (res.data.success) {
      message.success('移除成功');
      fetchStocks(activeGroupId.value);
    }
  } catch (error) {
    console.error(error);
  }
};


onMounted(() => {
  fetchGroups();
});
</script>

<style scoped>
.watchlist-container {
  padding: 16px;
}

.group-tags-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 24px;
  gap: 8px;
}

.group-label {
  font-size: 16px;
  font-weight: 500;
  color: #111;
  margin-right: 8px;
}

.custom-group-tag {
  font-size: 13px;
  padding: 4px 12px;
  cursor: pointer;
  border-radius: 6px;
  border: 1px solid #d9d9d9;
  display: flex;
  align-items: center;
  transition: all 0.2s ease;
  color: #111;
  background-color: #fff;
  user-select: none;
}

.custom-group-tag:hover {
  background-color: #fafafa;
  border-color: #1677ff;
  color: #1677ff;
}

.custom-group-tag.active {
  background-color: #fff;
  border-color: #1677ff;
  color: #1677ff;
}

.custom-group-tag.active:hover {
  background-color: #f5f5f5;
  border-color: #4096ff;
  color: #4096ff;
}

.delete-group-icon {
  margin-left: 6px;
  font-size: 12px;
  color: inherit;
  opacity: 0.8;
  transition: opacity 0.2s;
  cursor: pointer;
}

.delete-group-icon:hover {
  opacity: 1;
}

.custom-group-tag.add-btn {
  color: #888;
  padding: 4px 10px;
  border: 1px dashed #d9d9d9;
}

.custom-group-tag.add-btn:hover {
  color: #1677ff;
  border-color: #1677ff;
  background-color: #fafafa;
}

.draggable-wrapper {
  cursor: move;
  height: 100%;
}

.stock-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.add-stock-card {
  height: 100%;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9 !important;
  background-color: transparent;
  cursor: pointer;
  transition: all 0.3s ease;
}

.add-stock-card:hover {
  border-color: #1677ff !important;
  background-color: #f0f5ff;
}

.add-stock-card:hover .add-icon,
.add-stock-card:hover .add-text {
  color: #1677ff;
}

.add-icon {
  font-size: 32px;
  color: #bfbfbf;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.add-text {
  font-size: 15px;
  color: #888;
  font-weight: 500;
  transition: all 0.3s;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.stock-info {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stock-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.stock-code {
  font-size: 12px;
  color: #888;
}

.delete-icon {
  color: #ff4d4f;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.delete-icon:hover {
  opacity: 1;
}

.quote-info {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.latest-price {
  font-size: 20px;
  font-weight: bold;
}

.change-percent {
  font-size: 14px;
  font-weight: 500;
}

.up {
  color: #ff4d4f;
}

.down {
  color: #52c41a;
}

.neutral {
  color: #333;
}

.kline-box {
  flex-grow: 1;
  min-height: 100px;
}

.fundamentals {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.fund-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.fund-item .label {
  color: #888;
  margin-bottom: 2px;
}

.fund-item .val {
  color: #333;
  font-weight: 500;
}

.dividends-wrap {
  margin-top: 4px;
}

.dividend-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #666;
  margin-bottom: 2px;
}

.div-date {
  color: #999;
}

.div-text {
  color: #d46b08; /* 橙色高亮分红信息 */
  font-weight: 500;
}
</style>
