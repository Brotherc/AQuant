<template>
  <div class="watchlist-container">
    <!-- 分组导航标签 -->
    <div class="group-tags-header">
      <div class="group-label">自选分组：</div>
      <div 
        v-for="group in groups" 
        :key="group.id" 
        class="custom-group-tag"
        :class="{ active: activeGroupId === group.id, editing: editingGroupId === group.id }"
        @click="handleTabChange(group.id)"
        @dblclick="startEditGroup(group)"
      >
        <template v-if="editingGroupId === group.id">
          <a-input
            ref="editInputRef"
            v-model:value="editGroupName"
            size="small"
            class="edit-group-input"
            @blur="submitEditGroup"
            @keyup.enter="submitEditGroup"
            @keyup.esc="cancelEditGroup"
            @click.stop
          />
        </template>
        <template v-else>
          <span class="tag-name">{{ group.name }}</span>
          <edit-outlined 
            v-if="activeGroupId === group.id" 
            class="edit-group-icon"
            @click.stop="startEditGroup(group)" 
          />
          <close-outlined 
            v-if="activeGroupId === group.id" 
            class="delete-group-icon"
            @click.stop="onDeleteGroup(group.id)" 
          />
        </template>
      </div>
      <div class="custom-group-tag add-btn" @click="showAddGroupModal">
        ＋
      </div>
    </div>

    <!-- 自选股内容区 -->
    <div class="watchlist-content" v-if="activeGroupId">
    
      <!-- 轻量级排序与搜索操作带 -->
      <div class="control-bar" v-if="stocks.length > 0">
        <div class="search-box" style="display: flex; align-items: center; gap: 16px;">
          <a-input 
            v-model:value="searchQuery" 
            placeholder="输入名称或代码搜索" 
            allowClear 
            size="small"
            style="width: 200px; border-radius: 4px;"
          >
            <template #prefix>
              <search-outlined style="color: rgba(0, 0, 0, 0.45)" />
            </template>
          </a-input>
          <a-checkbox v-model:checked="filterNoti">仅看有通知设置</a-checkbox>
        </div>
        <div class="sort-options">
          <span class="ctrl-label">排序：</span>
        <span class="ctrl-item" :class="{ active: sortKey === 'default' }" @click="handleSortChange('default')">默认</span>
        <span class="ctrl-item" :class="{ active: sortKey === 'latestPrice' }" @click="handleSortChange('latestPrice')">
          最新价 <caret-up-outlined v-if="sortKey === 'latestPrice' && sortOrder === 'asc'"/><caret-down-outlined v-else />
        </span>
        <span class="ctrl-item" :class="{ active: sortKey === 'changePercent' }" @click="handleSortChange('changePercent')">
          涨跌幅 <caret-up-outlined v-if="sortKey === 'changePercent' && sortOrder === 'asc'"/><caret-down-outlined v-else />
        </span>
        <span class="ctrl-item" :class="{ active: sortKey === 'pe' }" @click="handleSortChange('pe')">
          PE <caret-up-outlined v-if="sortKey === 'pe' && sortOrder === 'asc'"/><caret-down-outlined v-else />
        </span>
        <span class="ctrl-item" :class="{ active: sortKey === 'roe' }" @click="handleSortChange('roe')">
          ROE <caret-up-outlined v-if="sortKey === 'roe' && sortOrder === 'asc'"/><caret-down-outlined v-else />
        </span>
        </div>
      </div>

      <a-spin :spinning="loading">
        <a-row :gutter="[16, 16]" class="card-grid">
          <a-col 
            :xs="24" :sm="24" :md="12" :lg="8" :xl="6"
            v-for="stock in sortedStocks" 
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
                  <div class="header-right">
                    <span @click.stop="openNotiModal(stock)" style="display: inline-flex; align-items: center; cursor: pointer; margin-right: 12px; font-size: 16px;">
                      <bell-filled
                        v-if="stock.hasNotification"
                        style="color: #1890ff;"
                        title="修改通知规则"
                      />
                      <bell-outlined 
                        v-else
                        class="noti-icon"
                        title="新增消息提醒"
                      />
                    </span>
                    <a-dropdown :trigger="['click']">
                      <ellipsis-outlined class="more-icon" />
                      <template #overlay>
                        <a-menu>
                          <a-menu-item @click="openStockDetail(stock)">
                            查看详情
                          </a-menu-item>
                          <a-menu-divider />
                          <a-menu-item 
                            :disabled="isFirst(stock.stockCode)"
                            @click="handleMoveToTop(stock.stockCode)"
                          >
                            移至最前
                          </a-menu-item>
                          <a-tooltip :title="sortKey !== 'default' ? '请先切换到默认排序以进行手动排序' : ''" placement="left">
                            <a-menu-item 
                              :disabled="sortKey !== 'default' || isFirst(stock.stockCode)"
                              @click="handleMove(stock.stockCode, 'up')"
                            >
                              往前移
                            </a-menu-item>
                          </a-tooltip>
                          <a-tooltip :title="sortKey !== 'default' ? '请先切换到默认排序以进行手动排序' : ''" placement="left">
                            <a-menu-item 
                              :disabled="sortKey !== 'default' || isLast(stock.stockCode)"
                              @click="handleMove(stock.stockCode, 'down')"
                            >
                              往后移
                            </a-menu-item>
                          </a-tooltip>
                          <a-menu-divider />
                          <a-menu-item @click="showMoveGroupModal(stock)">
                            修改分组
                          </a-menu-item>
                          <a-menu-divider />
                          <a-menu-item danger @click.stop>
                            <div @click.stop>
                              <a-popconfirm 
                                title="确定移除该股票吗？" 
                                @confirm="handleRemoveStock(stock.stockCode)"
                                placement="left"
                              >
                                <div style="margin: -5px -12px; padding: 5px 12px;">移除</div>
                              </a-popconfirm>
                            </div>
                          </a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </div>
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

    <!-- 整个页面没有任何分组时的空状态 -->
    <div v-else-if="groups.length === 0 && !loading" class="full-empty-container">
      <a-empty description="暂无自选数据" />
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

    <a-modal 
      v-model:visible="detailVisible" 
      title="股票详情" 
      :width="1000" 
      :footer="null" 
      centered
      destroyOnClose
    >
      <StockDetailView v-if="selectedStock" :stock="selectedStock" />
    </a-modal>

    <!-- 修改分组 Modal -->
    <a-modal v-model:visible="moveGroupModalVisible" title="修改分组" @ok="handleExecuteMoveGroup" :confirmLoading="moveGroupLoading">
      <div style="margin-bottom: 20px;">
        <div style="margin-bottom: 8px; color: #8c8c8c;">将股票从当前分组移动到：</div>
        <a-select v-model:value="targetGroupId" placeholder="请选择目标分组" style="width: 100%" size="large">
          <a-select-option v-for="g in otherGroups" :key="g.id" :value="g.id">
            {{ g.name }}
          </a-select-option>
        </a-select>
        <div v-if="otherGroups.length === 0" style="color: #ff4d4f; margin-top: 8px; font-size: 12px;">暂无其他可迁入的分组</div>
      </div>
    </a-modal>

    <!-- 通知设置 Modal -->
    <a-modal
      v-model:visible="notiModalVisible"
      :title="`通知设置 - ${currentStockName}(${currentStockCode})`"
      width="1200px"
      :footer="null"
      destroyOnClose
    >
      <div v-if="notiLoading" style="text-align: center; padding: 20px;">
        <a-spin />
      </div>
      <div v-else>
        <div style="margin-bottom: 16px; text-align: right;">
          <a-button type="primary" ghost @click="handleAddNoti">
            <template #prefix><plus-outlined /></template>
            新增
          </a-button>
        </div>
        
        <a-list :data-source="notiList" bordered>
          <template #renderItem="{ item, index }">
            <a-list-item>
              <div style="width: 100%; padding: 8px 0;">
                <a-row :gutter="12" align="middle">
                  <a-col :span="4">
                    <a-select v-model:value="item.type" style="width: 100%;">
                      <a-select-option :value="1">价格</a-select-option>
                      <a-select-option :value="2">双均线策略</a-select-option>
                    </a-select>
                  </a-col>
                  
                  <a-col :span="10">
                    <div v-if="item.type === 1" style="display: flex; gap: 8px;">
                      <a-select v-model:value="item.condition" style="width: 110px;">
                        <a-select-option value="UP">向上突破</a-select-option>
                        <a-select-option value="DOWN">向下突破</a-select-option>
                      </a-select>
                      <a-input-number 
                        v-model:value="item.thresholdValue" 
                        placeholder="价格" 
                        style="width: 120px;" 
                        :min="0"
                        :precision="2"
                      />
                    </div>
                    <div v-else-if="item.type === 2" style="display: flex; gap: 8px; align-items: center;">
                      <a-select v-model:value="item.condition" style="width: 110px;">
                        <a-select-option value="UP">金叉(买入)</a-select-option>
                        <a-select-option value="DOWN">死叉(卖出)</a-select-option>
                      </a-select>
                      <span style="font-size: 11px; color: #999;">MA:</span>
                      <a-input-number v-model:value="item.maShort" :min="2" style="width: 100px;" />
                      <span style="font-size: 11px; color: #999;">/</span>
                      <a-input-number v-model:value="item.maLong" :min="3" style="width: 100px;" />
                    </div>
                  </a-col>

                  <a-col :span="4">
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <a-tooltip placement="top" :overlayStyle="{ maxWidth: '400px' }">
                        <template #title>
                          <div>每日：触发通知后，今日不再重复报警</div>
                          <div>重复：条件满足时持续报警，间隔1分钟</div>
                        </template>
                        <span style="font-size: 12px; color: #999; white-space: nowrap; cursor: help; border-bottom: 1px dashed #ccc;">策略:</span>
                        <question-circle-outlined style="font-size: 12px; color: #ccc; cursor: help; margin-left: 2px;" />
                      </a-tooltip>
                      <a-radio-group v-model:value="item.notifyStrategy" style="display: flex; flex-wrap: nowrap;">
                        <a-radio :value="1" style="margin-right: 2px; font-size: 12px;">每日</a-radio>
                        <a-radio :value="2" style="margin-right: 0; font-size: 12px;">重复</a-radio>
                      </a-radio-group>
                    </div>
                  </a-col>
                  
                  <a-col :span="3" style="text-align: center;">
                    <a-switch 
                      v-model:checked="item.isEnabled" 
                      :checkedValue="1" 
                      :unCheckedValue="0" 
                      checked-children="启用"
                      un-checked-children="停用"
                      style="min-width: 60px;"
                    />
                  </a-col>
                  
                  <a-col :span="3" style="text-align: right;">
                    <a-space :size="0">
                      <a-button type="link" @click="handleSaveNoti(item)" style="padding: 0 4px;">保存</a-button>
                      <a-popconfirm title="删除？" @confirm="handleDeleteNoti(item, index)">
                        <a-button type="link" danger style="padding: 0 4px;">删除</a-button>
                      </a-popconfirm>
                    </a-space>
                  </a-col>
                </a-row>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { 
  CloseOutlined, 
  PlusOutlined, 
  CaretUpOutlined, 
  CaretDownOutlined, 
  SearchOutlined, 
  EllipsisOutlined, 
  EditOutlined,
  BellOutlined,
  BellFilled,
  QuestionCircleOutlined
} from '@ant-design/icons-vue';
import MiniKlineChart from './components/MiniKlineChart.vue';
import StockDetailView from './components/StockDetailView.vue';
import { 
  getWatchlistGroups,
  getWatchlistStocks, 
  createWatchlistGroup, 
  updateWatchlistGroup,
  deleteWatchlistGroup, 
  addStockToWatchlist, 
  removeStockFromWatchlist,
  moveWatchlistStock,
  moveWatchlistStockToGroup,
  type WatchlistGroupVO,
  type WatchlistStockVO
} from '@/api/watchlist';
import { getNotificationList, saveNotification, deleteNotification } from '@/api/notification';

const groups = ref<WatchlistGroupVO[]>([]);
const activeGroupId = ref<number>();
const stocks = ref<WatchlistStockVO[]>([]);
const loading = ref(false);
const addLoading = ref(false);
const newStockCode = ref('');
const currentStockCode = ref('');

// Group Modal
const groupModalVisible = ref(false);
const groupLoading = ref(false);
const groupForm = reactive({ name: '' });

// 股票详情相关
const detailVisible = ref(false);
const selectedStock = ref<WatchlistStockVO | null>(null);

const openStockDetail = (stock: WatchlistStockVO) => {
  selectedStock.value = stock;
  detailVisible.value = true;
};

// 修改分组名称相关
const editingGroupId = ref<number | null>(null);
const editGroupName = ref('');
const editInputRef = ref<any>(null);

const startEditGroup = (group: WatchlistGroupVO) => {
  if (activeGroupId.value !== group.id) return;
  editingGroupId.value = group.id;
  editGroupName.value = group.name;
  nextTick(() => {
    editInputRef.value?.focus();
  });
};

const cancelEditGroup = () => {
  editingGroupId.value = null;
  editGroupName.value = '';
};

const submitEditGroup = async () => {
  if (!editingGroupId.value) return;
  const newName = editGroupName.value.trim();
  const oldGroup = groups.value.find(g => g.id === editingGroupId.value);
  
  if (!newName || newName === oldGroup?.name) {
    cancelEditGroup();
    return;
  }

  try {
    const res = await updateWatchlistGroup({ id: editingGroupId.value, name: newName });
    if (res.data.success) {
      message.success('修改成功');
      // 更新本地状态
      if (oldGroup) oldGroup.name = newName;
      cancelEditGroup();
    }
  } catch (error) {
    console.error('Failed to update group name:', error);
  }
};

// 修改分组相关
const moveGroupModalVisible = ref(false);
const moveGroupLoading = ref(false);
const targetGroupId = ref<number>();
const movingStockCode = ref('');

const otherGroups = computed(() => {
  return groups.value.filter(g => g.id !== activeGroupId.value);
});

const showMoveGroupModal = (stock: WatchlistStockVO) => {
  movingStockCode.value = stock.stockCode;
  targetGroupId.value = undefined;
  moveGroupModalVisible.value = true;
};

const handleExecuteMoveGroup = async () => {
  if (!targetGroupId.value) {
    message.warning('请选择目标分组');
    return;
  }
  moveGroupLoading.value = true;
  try {
    const res = await moveWatchlistStockToGroup({
      stockCode: movingStockCode.value,
      fromGroupId: activeGroupId.value!,
      toGroupId: targetGroupId.value
    });
    if (res.data.success) {
      message.success('分组修改成功');
      moveGroupModalVisible.value = false;
      // 刷新当前分组列表
      if (activeGroupId.value) {
        await fetchStocks(activeGroupId.value);
      }
    }
  } catch (error) {
    console.error('Failed to move group:', error);
  } finally {
    moveGroupLoading.value = false;
  }
};

// 搜索与排序状态控制
const searchQuery = ref<string>('');
const filterNoti = ref<boolean>(false);
const sortKey = ref<string>('default');
const sortOrder = ref<'asc' | 'desc'>('desc');

const handleSortChange = (key: string) => {
  if (key === 'default') {
    sortKey.value = 'default';
    return;
  }
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortKey.value = key;
    sortOrder.value = 'desc';
  }
};

const sortedStocks = computed(() => {
  let result = stocks.value;
  
  // 1. 本地实时过滤
  if (searchQuery.value) {
    const q = searchQuery.value.trim().toLowerCase();
    result = result.filter(stock => 
      (stock.stockName && stock.stockName.toLowerCase().includes(q)) || 
      (stock.stockCode && stock.stockCode.toLowerCase().includes(q))
    );
  }

  // 1.5 按是否有预警通知过滤
  if (filterNoti.value) {
    result = result.filter(stock => stock.hasNotification);
  }

  // 2. 本地实时排序
  if (sortKey.value === 'default') return result;
  
  return [...result].sort((a, b) => {
    let valA = (a as any)[sortKey.value];
    let valB = (b as any)[sortKey.value];
    
    // 把空值或者无效值放到末尾
    if (valA == null || isNaN(valA)) valA = sortOrder.value === 'asc' ? Infinity : -Infinity;
    if (valB == null || isNaN(valB)) valB = sortOrder.value === 'asc' ? Infinity : -Infinity;
    
    return sortOrder.value === 'asc' ? valA - valB : valB - valA;
  });
});

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

const isFirst = (stockCode: string) => {
  return stocks.value.length > 0 && stocks.value[0]?.stockCode === stockCode;
};

const isLast = (stockCode: string) => {
  return stocks.value.length > 0 && stocks.value[stocks.value.length - 1]?.stockCode === stockCode;
};

const handleMove = async (stockCode: string, direction: 'up' | 'down') => {
  if (!activeGroupId.value) return;
  
  const index = stocks.value.findIndex(s => s.stockCode === stockCode);
  if (index === -1) return;
  
  const action = direction === 'up' ? 'UP' : 'DOWN';
  
  const newStocks = [...stocks.value];
  if (direction === 'up' && index > 0) {
    const s1 = newStocks[index];
    const s2 = newStocks[index - 1];
    if (s1 && s2) {
      newStocks[index] = s2;
      newStocks[index - 1] = s1;
    }
  } else if (direction === 'down' && index < newStocks.length - 1) {
    const s1 = newStocks[index];
    const s2 = newStocks[index + 1];
    if (s1 && s2) {
      newStocks[index] = s2;
      newStocks[index + 1] = s1;
    }
  } else {
    return;
  }
  
  await syncMove(stockCode, action, newStocks);
};

const handleMoveToTop = async (stockCode: string) => {
  if (!activeGroupId.value) return;
  
  const index = stocks.value.findIndex(s => s.stockCode === stockCode);
  if (index <= 0) return;
  
  const newStocks = [...stocks.value];
  const [target] = newStocks.splice(index, 1);
  if (target) {
    newStocks.unshift(target);
    await syncMove(stockCode, 'TOP', newStocks);
  }
};

const syncMove = async (stockCode: string, action: 'UP' | 'DOWN' | 'TOP', newStocks: WatchlistStockVO[]) => {
  if (!activeGroupId.value) return;
  
  try {
    const res = await moveWatchlistStock({
      groupId: activeGroupId.value,
      stockCode,
      action
    });
    if (res.data.success) {
      // 始终更新底层数据，确保切换回默认排序时是正确的
      stocks.value = newStocks;
      
      if (action === 'TOP' && sortKey.value !== 'default') {
        message.success('已移至手动排序首位，切换到默认排序即可查看');
      } else if (sortKey.value === 'default') {
        // 默认排序下不需要额外提示
      } else {
        message.success('置顶排序更新成功');
      }
    }
  } catch (error) {
    console.error('Move failed:', error);
  }
};


// --- 通知提醒相关 ---
const notiModalVisible = ref(false);
const notiLoading = ref(false);
const notiList = ref<any[]>([]);
const currentStockName = ref('');

const processNotiList = (list: any[]) => {
  return list.map((item: any) => {
    if (item.params) {
      try {
        const p = JSON.parse(item.params);
        if (item.type === 1) {
          item.condition = p.condition || 'UP';
        } else if (item.type === 2) {
          item.condition = p.condition || 'UP';
          item.maShort = p.maShort || 5;
          item.maLong = p.maLong || 20;
        }
      } catch (e) {
        if (item.type === 1) item.condition = 'UP';
        else if (item.type === 2) {
          item.condition = 'UP';
          item.maShort = 5;
          item.maLong = 20;
        }
      }
    } else {
      // 无参数时的默认值
      if (item.type === 1) item.condition = 'UP';
      else if (item.type === 2) {
        item.condition = 'UP';
        item.maShort = 5;
        item.maLong = 20;
      }
    }
    item.notifyStrategy = item.notifyStrategy || 1;
    return item;
  });
};

const openNotiModal = async (stock: any) => {
  currentStockCode.value = stock.stockCode;
  currentStockName.value = stock.stockName;
  notiModalVisible.value = true;
  notiLoading.value = true;
  try {
    const res = await getNotificationList(stock.stockCode);
    notiList.value = processNotiList(res.data.data || []);
  } catch (error) {
    console.error('Fetch notification list failed:', error);
  } finally {
    notiLoading.value = false;
  }
};

const handleAddNoti = () => {
  notiList.value.push({
    stockCode: currentStockCode.value,
    type: 1, // 默认价格通知
    thresholdValue: null,
    condition: 'UP',
    maShort: 5,
    maLong: 20,
    params: '',
    isEnabled: 1,
    notifyStrategy: 1
  });
};

const handleSaveNoti = async (item: any) => {
  if (item.type === 1) {
    if (!item.thresholdValue && item.thresholdValue !== 0) {
      message.warning('请输入通知价格');
      return;
    }
  } else if (item.type === 2) {
    if (!item.maShort || !item.maLong) {
      message.warning('请完整填写均线周期');
      return;
    }
    if (item.maShort >= item.maLong) {
      message.warning('长线周期必须大于短线周期');
      return;
    }
  }
  
  // 组装 params
  if (item.type === 1) {
    item.params = JSON.stringify({ condition: item.condition || 'UP' });
  } else if (item.type === 2) {
    item.params = JSON.stringify({ 
      condition: item.condition || 'UP',
      maShort: Math.floor(item.maShort), 
      maLong: Math.floor(item.maLong) 
    });
  }

  try {
    const res = await saveNotification(item);
    if (res.data.success) {
      message.success('通知设置已保存');
      // 重新拉取一次以获取新创建的 ID 并确保数据回显正确
      const listRes = await getNotificationList(currentStockCode.value);
      notiList.value = processNotiList(listRes.data.data || []);
      // 刷新列表以同步铃铛图标状态
      if (activeGroupId.value) {
        fetchStocks(activeGroupId.value);
      }
    }
  } catch (error) {
    console.error('Save notification failed:', error);
  }
};

const handleDeleteNoti = async (item: any, index: number) => {
  if (!item.id) {
    notiList.value.splice(index, 1);
    return;
  }
  try {
    const res = await deleteNotification(item.id);
    if (res.data.success) {
      message.success('通知已删除');
      notiList.value.splice(index, 1);
      // 刷新列表以同步铃铛图标状态
      if (activeGroupId.value) {
        fetchStocks(activeGroupId.value);
      }
    }
  } catch (error) {
    console.error('Delete notification failed:', error);
  }
};

onMounted(() => {
  fetchGroups();
});
</script>

<style scoped>
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.noti-icon {
  font-size: 16px;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.2s;
}

.noti-icon:hover {
  color: #1890ff;
  transform: scale(1.1);
}

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

.edit-group-icon {
  margin-left: 8px;
  font-size: 13px;
  color: inherit;
  opacity: 0.6;
  transition: all 0.2s;
  cursor: pointer;
}

.edit-group-icon:hover {
  opacity: 1;
  transform: scale(1.1);
}

.custom-group-tag.editing {
  padding: 0;
  border-color: transparent;
  background-color: transparent;
}

.edit-group-input {
  width: 120px;
  height: 28px;
  font-size: 13px;
  border-radius: 4px;
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

.more-icon {
  font-size: 18px;
  color: #888;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.more-icon:hover {
  background-color: #f5f5f5;
  color: #1677ff;
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

/* 排序与搜索控制器样式 */
.control-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  row-gap: 10px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #888;
}

.search-box {
  display: flex;
  align-items: center;
  flex: 0 1 auto;
  min-width: 0;
  flex-wrap: wrap;
}

.sort-options {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex: 1 1 480px;
  max-width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  padding-bottom: 2px;
}

.control-bar .ctrl-label {
  margin-right: 8px;
  white-space: nowrap;
  flex: 0 0 auto;
}

.control-bar .ctrl-item {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  min-width: fit-content;
  margin-left: 16px;
  cursor: pointer;
  transition: color 0.2s;
  white-space: nowrap;
}

.control-bar .ctrl-item:hover {
  color: #333;
}

.control-bar .ctrl-item.active {
  color: #1890ff; /* 激活项采用焦点色 */
  font-weight: 500;
}

.control-bar .ctrl-item .anticon {
  margin-left: 2px;
  font-size: 11px;
}

/* 空状态样式 */
.empty-stocks-wrapper {
  padding: 80px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.full-empty-container {
  padding: 120px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}
</style>
