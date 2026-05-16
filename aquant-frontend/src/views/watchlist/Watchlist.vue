<template>
  <div class="watchlist-container">
    <!-- 顶部分组导航与全局过滤 -->
    <div class="watchlist-sticky-toolbar" v-if="groups.length > 0">
      <div class="group-anchors-header">
        <div class="group-anchors-list" ref="anchorsListEl">
          <template v-for="(group, idx) in visibleGroups" :key="group.id">
            <span
              class="group-anchor"
              :class="{ active: activeGroupId === group.id }"
              :title="group.name"
              @click="scrollToGroup(group.id)"
            >
              {{ group.name }}
            </span>
            <span v-if="idx < visibleGroups.length - 1" class="group-anchor-sep">·</span>
          </template>
        </div>
        <a-dropdown
          v-if="overflowGroups.length > 0"
          :trigger="['hover']"
          placement="bottomRight"
          overlayClassName="group-anchor-more-dropdown"
        >
          <div class="group-anchors-more-wrap">
            <span v-if="visibleGroups.length > 0" class="group-anchor-sep">·</span>
            <span class="group-anchor group-anchor-more" :class="{ active: overflowGroups.some(g => g.id === activeGroupId) }">
              更多
            </span>
          </div>
          <template #overlay>
            <a-menu>
              <a-menu-item
                v-for="group in overflowGroups"
                :key="group.id"
                :class="{ active: activeGroupId === group.id }"
                :title="group.name"
                @click="scrollToGroup(group.id)"
              >
                {{ group.name }}
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <div class="group-anchors-right">
          <div class="global-filter-bar">
            <a-checkbox v-model:checked="globalFilterNoti">仅看有通知设置</a-checkbox>
            <a-input
              v-model:value="globalSearchQuery"
              placeholder="输入名称或代码搜索"
              allowClear
              class="watchlist-search-input global-filter-search"
            >
              <template #prefix>
                <search-outlined />
              </template>
            </a-input>
          </div>
          <a-button type="primary" class="toolbar-action-button" @click="showAddGroupModal">
            <template #icon><plus-outlined /></template>
            新建分组
          </a-button>
        </div>
      </div>
    </div>

    <!-- 整个页面没有任何分组时的空状态 -->
    <div v-if="groups.length === 0 && !groupsLoading" class="full-empty-container">
      <a-empty description="暂无自选分组" >
        <a-button type="primary" @click="showAddGroupModal">
          <template #icon><plus-outlined /></template>
          新建分组
        </a-button>
      </a-empty>
    </div>

    <!-- 各分组卡片区 -->
    <div
      v-for="group in groups"
      :key="group.id"
      class="group-section"
      :data-group-id="group.id"
      :ref="(el) => setGroupSectionRef(group.id, el)"
    >
      <!-- 分组标题与操作 -->
      <div class="group-section-header">
        <div class="group-section-title" :title="group.name">{{ group.name }}</div>
        <div class="group-section-divider"></div>
        <div class="group-section-actions">
          <a-dropdown :trigger="['click']" placement="bottomRight">
            <span class="group-section-more" @click.stop>
              <ellipsis-outlined />
            </span>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="openRenameGroupModal(group)">
                  重命名
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item :disabled="isFirstGroup(group.id)" @click="handleMoveGroup(group.id, 'UP')">
                  上移
                </a-menu-item>
                <a-menu-item :disabled="isLastGroup(group.id)" @click="handleMoveGroup(group.id, 'DOWN')">
                  下移
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item danger @click="onDeleteGroup(group.id)">
                  删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
      <div class="group-section-sort-row" v-if="(groupStocks[group.id] || []).length > 0">
        <div class="sort-options">
          <span class="ctrl-label">排序：</span>
          <span class="ctrl-item" :class="{ active: groupUiState[group.id].sortKey === 'default' }" @click="handleSortChange(group.id, 'default')">默认</span>
          <span class="ctrl-item" :class="{ active: groupUiState[group.id].sortKey === 'latestPrice' }" @click="handleSortChange(group.id, 'latestPrice')">
            最新价 <caret-up-outlined v-if="groupUiState[group.id].sortKey === 'latestPrice' && groupUiState[group.id].sortOrder === 'asc'"/><caret-down-outlined v-else />
          </span>
          <span class="ctrl-item" :class="{ active: groupUiState[group.id].sortKey === 'changePercent' }" @click="handleSortChange(group.id, 'changePercent')">
            涨跌幅 <caret-up-outlined v-if="groupUiState[group.id].sortKey === 'changePercent' && groupUiState[group.id].sortOrder === 'asc'"/><caret-down-outlined v-else />
          </span>
          <span class="ctrl-item" :class="{ active: groupUiState[group.id].sortKey === 'pe' }" @click="handleSortChange(group.id, 'pe')">
            PE <caret-up-outlined v-if="groupUiState[group.id].sortKey === 'pe' && groupUiState[group.id].sortOrder === 'asc'"/><caret-down-outlined v-else />
          </span>
          <span class="ctrl-item" :class="{ active: groupUiState[group.id].sortKey === 'roe' }" @click="handleSortChange(group.id, 'roe')">
            ROE <caret-up-outlined v-if="groupUiState[group.id].sortKey === 'roe' && groupUiState[group.id].sortOrder === 'asc'"/><caret-down-outlined v-else />
          </span>
        </div>
      </div>

      <a-spin :spinning="groupLoading[group.id]">
        <a-row :gutter="[16, 16]" class="card-grid">
          <a-col
            :xs="24" :sm="24" :md="12" :lg="8" :xl="6"
            v-for="stock in getSortedStocks(group.id)"
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
                        class="noti-icon noti-icon--active"
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
                            :disabled="isFirstInGroup(group.id, stock.stockCode)"
                            @click="handleMoveToTop(group.id, stock.stockCode)"
                          >
                            移至最前
                          </a-menu-item>
                          <a-menu-item
                            :disabled="groupUiState[group.id].sortKey !== 'default' || isFirstInGroup(group.id, stock.stockCode)"
                            :title="groupUiState[group.id].sortKey !== 'default' ? '请先切换到默认排序以进行手动排序' : ''"
                            @click="handleMove(group.id, stock.stockCode, 'up')"
                          >
                            往前移
                          </a-menu-item>
                          <a-menu-item
                            :disabled="groupUiState[group.id].sortKey !== 'default' || isLastInGroup(group.id, stock.stockCode)"
                            :title="groupUiState[group.id].sortKey !== 'default' ? '请先切换到默认排序以进行手动排序' : ''"
                            @click="handleMove(group.id, stock.stockCode, 'down')"
                          >
                            往后移
                          </a-menu-item>
                          <a-menu-divider />
                          <a-menu-item @click="showMoveGroupModal(group.id, stock)">
                            修改分组
                          </a-menu-item>
                          <a-menu-divider />
                          <a-menu-item danger @click.stop>
                            <div @click.stop>
                              <a-popconfirm
                                title="确定移除该股票吗？"
                                @confirm="handleRemoveStock(group.id, stock.stockCode)"
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
            <a-card hoverable class="add-stock-card" size="small" @click="showAddStockModal(group.id)">
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
    <a-modal v-model:visible="groupModalVisible" title="新增自选分组" @ok="handleCreateGroup" :confirmLoading="createGroupLoading">
      <a-form :model="groupForm" layout="vertical">
        <a-form-item label="分组名称" required>
          <a-input v-model:value="groupForm.name" placeholder="请输入分组名称" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重命名分组 Modal -->
    <a-modal v-model:visible="renameModalVisible" title="重命名分组" @ok="handleRenameGroup" :confirmLoading="renameLoading">
      <a-form :model="renameForm" layout="vertical">
        <a-form-item label="分组名称" required>
          <a-input v-model:value="renameForm.name" placeholder="请输入分组名称" />
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
          <a-select-option v-for="g in moveGroupOptions" :key="g.id" :value="g.id">
            {{ g.name }}
          </a-select-option>
        </a-select>
        <div v-if="moveGroupOptions.length === 0" style="color: #ff4d4f; margin-top: 8px; font-size: 12px;">暂无其他可迁入的分组</div>
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
          <a-button type="default" @click="handleAddNoti">
            <template #icon><plus-outlined /></template>
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
                        <span style="font-size: 12px; color: #999; white-space: nowrap; cursor: help;">策略:</span>
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
                      class="notification-enable-switch"
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
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
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
  QuestionCircleOutlined,
  CloseCircleFilled
} from '@ant-design/icons-vue';
import MiniKlineChart from './components/MiniKlineChart.vue';
import StockDetailView from './components/StockDetailView.vue';
import {
  getWatchlistGroups,
  getWatchlistStocks,
  createWatchlistGroup,
  updateWatchlistGroup,
  deleteWatchlistGroup,
  moveWatchlistGroup,
  addStockToWatchlist,
  removeStockFromWatchlist,
  moveWatchlistStock,
  moveWatchlistStockToGroup,
  type WatchlistGroupVO,
  type WatchlistStockVO,
  type WatchlistDividendVO
} from '@/api/watchlist';
import { getNotificationList, saveNotification, deleteNotification } from '@/api/notification';

const groups = ref<WatchlistGroupVO[]>([]);
const groupsLoading = ref(false);
/** 当前在视口内（被锚点高亮）的分组 */
const activeGroupId = ref<number>();
/** 各分组的股票数据：groupId → 股票列表 */
const groupStocks = reactive<Record<number, WatchlistStockVO[]>>({});
/** 各分组的加载状态 */
const groupLoading = reactive<Record<number, boolean>>({});
/** 已经请求过的分组（按需懒加载使用） */
const loadedGroups = reactive<Set<number>>(new Set());

/** 顶部锚点导航：可显示的分组数（其余进入"更多"下拉） */
const MAX_VISIBLE_ANCHOR_GROUPS = 4;
const anchorsListEl = ref<HTMLElement | null>(null);
const visibleCount = ref(Number.MAX_SAFE_INTEGER);
const visibleAnchorCount = computed(() => Math.min(visibleCount.value, MAX_VISIBLE_ANCHOR_GROUPS, groups.value.length));
const visibleGroups = computed(() => groups.value.slice(0, visibleAnchorCount.value));
const overflowGroups = computed(() => groups.value.slice(visibleAnchorCount.value));

/** 全局搜索/通知过滤状态 */
const globalSearchQuery = ref('');
const globalFilterNoti = ref(false);

/** 各分组的排序状态 */
interface GroupUiState {
  sortKey: string;
  sortOrder: 'asc' | 'desc';
}
const groupUiState = reactive<Record<number, GroupUiState>>({});
const ensureUiState = (groupId: number) => {
  if (!groupUiState[groupId]) {
    groupUiState[groupId] = {
      sortKey: 'default',
      sortOrder: 'desc'
    };
  }
};

/** 分组区块 DOM 引用（用于滚动定位与 IntersectionObserver） */
const groupSectionRefs = new Map<number, HTMLElement>();
const setGroupSectionRef = (groupId: number, el: any) => {
  if (el && el instanceof HTMLElement) {
    groupSectionRefs.set(groupId, el);
    observeGroupSection(groupId, el);
  } else {
    const old = groupSectionRefs.get(groupId);
    if (old && intersectionObserver) {
      intersectionObserver.unobserve(old);
    }
    groupSectionRefs.delete(groupId);
  }
};

let intersectionObserver: IntersectionObserver | null = null;
let activeGroupUpdateFrame: number | null = null;
let initialActiveUnlockFrame: number | null = null;
let initialActiveUnlockSecondFrame: number | null = null;
let initialActiveLocked = true;

const getActiveAnchorTop = () => {
  const toolbar = document.querySelector('.watchlist-sticky-toolbar') as HTMLElement | null;
  return toolbar ? toolbar.getBoundingClientRect().bottom + 16 : 140;
};

const updateActiveGroupByPosition = () => {
  if (initialActiveLocked || groupSectionRefs.size === 0) return;
  const anchorTop = getActiveAnchorTop();
  let nearestPassed: { id: number; top: number } | null = null;
  let nearestPending: { id: number; top: number } | null = null;

  for (const group of groups.value) {
    const el = groupSectionRefs.get(group.id);
    if (!el) continue;
    const rect = el.getBoundingClientRect();
    if (rect.bottom <= anchorTop) continue;
    const top = rect.top - anchorTop;

    if (top <= 0) {
      if (!nearestPassed || top > nearestPassed.top) {
        nearestPassed = { id: group.id, top };
      }
    } else if (!nearestPending || top < nearestPending.top) {
      nearestPending = { id: group.id, top };
    }
  }

  const nextActiveId = nearestPassed?.id || nearestPending?.id;
  if (nextActiveId && activeGroupId.value !== nextActiveId) {
    activeGroupId.value = nextActiveId;
  }
};

const scheduleActiveGroupUpdate = () => {
  if (initialActiveLocked) return;
  if (activeGroupUpdateFrame != null) {
    cancelAnimationFrame(activeGroupUpdateFrame);
  }
  activeGroupUpdateFrame = requestAnimationFrame(() => {
    activeGroupUpdateFrame = null;
    updateActiveGroupByPosition();
  });
};

/**
 * 第一次出现在视口时：触发懒加载 + 更新 activeGroupId
 */
const observeGroupSection = (groupId: number, el: HTMLElement) => {
  if (!intersectionObserver) return;
  intersectionObserver.observe(el);
};

const initIntersectionObserver = () => {
  intersectionObserver = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      const groupId = Number(entry.target.getAttribute('data-group-id'));
      if (entry.isIntersecting) {
        if (!loadedGroups.has(groupId)) {
          loadedGroups.add(groupId);
          fetchStocks(groupId);
        }
      }
    }
    scheduleActiveGroupUpdate();
  }, {
    threshold: [0, 0.2, 0.5],
    rootMargin: '-140px 0px -40% 0px'
  });
};

// 股票详情相关
const detailVisible = ref(false);
const selectedStock = ref<WatchlistStockVO | null>(null);

const openStockDetail = (stock: WatchlistStockVO) => {
  selectedStock.value = stock;
  detailVisible.value = true;
};

// 重命名分组 Modal
const renameModalVisible = ref(false);
const renameLoading = ref(false);
const renamingGroupId = ref<number>();
const renameForm = reactive({ name: '' });

const openRenameGroupModal = (group: WatchlistGroupVO) => {
  renamingGroupId.value = group.id;
  renameForm.name = group.name;
  renameModalVisible.value = true;
};

const handleRenameGroup = async () => {
  if (!renamingGroupId.value) return;
  const newName = renameForm.name.trim();
  if (!newName) {
    message.warning('请输入分组名称');
    return;
  }
  const target = groups.value.find(g => g.id === renamingGroupId.value);
  if (target && target.name === newName) {
    renameModalVisible.value = false;
    return;
  }
  renameLoading.value = true;
  try {
    const res = await updateWatchlistGroup({ id: renamingGroupId.value, name: newName });
    if (res.data.success) {
      message.success('修改成功');
      if (target) target.name = newName;
      renameModalVisible.value = false;
    }
  } catch (error) {
    console.error('Failed to update group name:', error);
  } finally {
    renameLoading.value = false;
  }
};

// 修改分组（移动股票）相关
const moveGroupModalVisible = ref(false);
const moveGroupLoading = ref(false);
const targetGroupId = ref<number>();
const movingStockCode = ref('');
const movingFromGroupId = ref<number>();

const moveGroupOptions = computed(() => {
  return groups.value.filter(g => g.id !== movingFromGroupId.value);
});

const showMoveGroupModal = (groupId: number, stock: WatchlistStockVO) => {
  movingStockCode.value = stock.stockCode;
  movingFromGroupId.value = groupId;
  targetGroupId.value = undefined;
  moveGroupModalVisible.value = true;
};

const handleExecuteMoveGroup = async () => {
  if (!targetGroupId.value || !movingFromGroupId.value) {
    message.warning('请选择目标分组');
    return;
  }
  moveGroupLoading.value = true;
  try {
    const res = await moveWatchlistStockToGroup({
      stockCode: movingStockCode.value,
      fromGroupId: movingFromGroupId.value,
      toGroupId: targetGroupId.value
    });
    if (res.data.success) {
      message.success('分组修改成功');
      moveGroupModalVisible.value = false;
      // 刷新源分组与目标分组
      await fetchStocks(movingFromGroupId.value);
      if (loadedGroups.has(targetGroupId.value)) {
        await fetchStocks(targetGroupId.value);
      }
    }
  } catch (error) {
    console.error('Failed to move group:', error);
  } finally {
    moveGroupLoading.value = false;
  }
};

// 排序处理
const handleSortChange = (groupId: number, key: string) => {
  ensureUiState(groupId);
  const state = groupUiState[groupId];
  if (key === 'default') {
    state.sortKey = 'default';
    return;
  }
  if (state.sortKey === key) {
    state.sortOrder = state.sortOrder === 'asc' ? 'desc' : 'asc';
  } else {
    state.sortKey = key;
    state.sortOrder = 'desc';
  }
};

const getSortedStocks = (groupId: number): WatchlistStockVO[] => {
  ensureUiState(groupId);
  const state = groupUiState[groupId];
  let result = groupStocks[groupId] || [];

  if (globalSearchQuery.value) {
    const q = globalSearchQuery.value.trim().toLowerCase();
    result = result.filter(stock =>
      (stock.stockName && stock.stockName.toLowerCase().includes(q)) ||
      (stock.stockCode && stock.stockCode.toLowerCase().includes(q))
    );
  }
  if (globalFilterNoti.value) {
    result = result.filter(stock => stock.hasNotification);
  }
  if (state.sortKey === 'default') return result;

  return [...result].sort((a, b) => {
    let valA = (a as any)[state.sortKey];
    let valB = (b as any)[state.sortKey];
    if (valA == null || isNaN(valA)) valA = state.sortOrder === 'asc' ? Infinity : -Infinity;
    if (valB == null || isNaN(valB)) valB = state.sortOrder === 'asc' ? Infinity : -Infinity;
    return state.sortOrder === 'asc' ? valA - valB : valB - valA;
  });
};

const getPriceColorClass = (changePercent: number | undefined | null) => {
  if (changePercent == null) return 'neutral';
  return changePercent > 0 ? 'up' : changePercent < 0 ? 'down' : 'neutral';
};

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
  if (dateStr.length === 8 && !dateStr.includes('-')) {
    return `${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}`;
  }
  return dateStr;
};

const fetchGroups = async () => {
  groupsLoading.value = true;
  try {
    const res = await getWatchlistGroups();
    if (res.data.success) {
      groups.value = res.data.data || [];
      // 初始化每个分组的 ui 状态
      for (const g of groups.value) {
        ensureUiState(g.id);
      }
      // 默认 active 为第一个
      if (groups.value.length > 0 && !activeGroupId.value) {
        activeGroupId.value = groups.value[0]!.id;
      }
    }
  } catch (error) {
    console.error(error);
  } finally {
    groupsLoading.value = false;
  }
};

const fetchStocks = async (groupId: number) => {
  groupLoading[groupId] = true;
  try {
    const res = await getWatchlistStocks(groupId);
    if (res.data.success) {
      groupStocks[groupId] = res.data.data || [];
    }
  } catch (error) {
    console.error(error);
  } finally {
    groupLoading[groupId] = false;
  }
};

/** 滚动到指定分组 */
const scrollToGroup = (groupId: number) => {
  const el = groupSectionRefs.get(groupId);
  if (el) {
    activeGroupId.value = groupId;
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

/* === 分组上移/下移 === */
const isFirstGroup = (groupId: number) => groups.value.length > 0 && groups.value[0]?.id === groupId;
const isLastGroup = (groupId: number) => groups.value.length > 0 && groups.value[groups.value.length - 1]?.id === groupId;

const handleMoveGroup = async (groupId: number, action: 'UP' | 'DOWN') => {
  try {
    const res = await moveWatchlistGroup({ id: groupId, action });
    if (res.data.success) {
      await fetchGroups();
    }
  } catch (error) {
    console.error('Move group failed:', error);
  }
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
          delete groupStocks[targetKey];
          delete groupLoading[targetKey];
          delete groupUiState[targetKey];
          loadedGroups.delete(targetKey);
          await fetchGroups();
          if (activeGroupId.value === targetKey) {
            activeGroupId.value = groups.value[0]?.id;
          }
        }
      } catch (error) {
        console.error(error);
      }
    },
  });
};

// 新建分组
const groupModalVisible = ref(false);
const createGroupLoading = ref(false);
const groupForm = reactive({ name: '' });

const showAddGroupModal = () => {
  groupForm.name = '';
  groupModalVisible.value = true;
};

const handleCreateGroup = async () => {
  if (!groupForm.name) {
    message.warning('请输入分组名称');
    return;
  }
  createGroupLoading.value = true;
  try {
    const res = await createWatchlistGroup(groupForm);
    if (res.data.success) {
      message.success('创建成功');
      groupModalVisible.value = false;
      await fetchGroups();
    }
  } catch (error) {
    console.error(error);
  } finally {
    createGroupLoading.value = false;
  }
};

// 添加股票
const addStockModalVisible = ref(false);
const addLoading = ref(false);
const newStockCode = ref('');
const addStockTargetGroupId = ref<number>();

const showAddStockModal = (groupId: number) => {
  addStockTargetGroupId.value = groupId;
  newStockCode.value = '';
  addStockModalVisible.value = true;
};

const handleAddStock = async () => {
  if (!addStockTargetGroupId.value) {
    message.warning('请先选择分组');
    return;
  }
  if (!newStockCode.value || newStockCode.value.length < 6) {
    message.warning('请输入正确的股票代码');
    return;
  }
  addLoading.value = true;
  try {
    const res = await addStockToWatchlist({
      groupId: addStockTargetGroupId.value,
      stockCode: newStockCode.value
    });
    if (res.data.success) {
      message.success('添加成功');
      addStockModalVisible.value = false;
      newStockCode.value = '';
      await fetchStocks(addStockTargetGroupId.value);
    }
  } catch (error) {
    console.error(error);
  } finally {
    addLoading.value = false;
  }
};

const handleRemoveStock = async (groupId: number, stockCode: string) => {
  try {
    const res = await removeStockFromWatchlist(groupId, stockCode);
    if (res.data.success) {
      message.success('移除成功');
      await fetchStocks(groupId);
    }
  } catch (error) {
    console.error(error);
  }
};

const isFirstInGroup = (groupId: number, stockCode: string) => {
  const list = groupStocks[groupId] || [];
  return list.length > 0 && list[0]?.stockCode === stockCode;
};

const isLastInGroup = (groupId: number, stockCode: string) => {
  const list = groupStocks[groupId] || [];
  return list.length > 0 && list[list.length - 1]?.stockCode === stockCode;
};

const handleMove = async (groupId: number, stockCode: string, direction: 'up' | 'down') => {
  const list = groupStocks[groupId] || [];
  const index = list.findIndex(s => s.stockCode === stockCode);
  if (index === -1) return;

  const action = direction === 'up' ? 'UP' : 'DOWN';
  const newStocks = [...list];

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

  await syncMove(groupId, stockCode, action, newStocks);
};

const handleMoveToTop = async (groupId: number, stockCode: string) => {
  const list = groupStocks[groupId] || [];
  const index = list.findIndex(s => s.stockCode === stockCode);
  if (index <= 0) return;

  const newStocks = [...list];
  const [target] = newStocks.splice(index, 1);
  if (target) {
    newStocks.unshift(target);
    await syncMove(groupId, stockCode, 'TOP', newStocks);
  }
};

const syncMove = async (groupId: number, stockCode: string, action: 'UP' | 'DOWN' | 'TOP', newStocks: WatchlistStockVO[]) => {
  try {
    const res = await moveWatchlistStock({
      groupId,
      stockCode,
      action
    });
    if (res.data.success) {
      groupStocks[groupId] = newStocks;
      const sortKey = groupUiState[groupId]?.sortKey;
      if (action === 'TOP' && sortKey !== 'default') {
        message.success('已移至手动排序首位，切换到默认排序即可查看');
      } else if (sortKey !== 'default') {
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
const currentStockCode = ref('');
const currentStockName = ref('');
/** 当前打开通知 Modal 的股票所在分组（用于保存后刷新） */
const notiSourceGroupId = ref<number>();

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
  // 找到该股票所在分组（遍历已加载的分组数据）
  notiSourceGroupId.value = undefined;
  for (const g of groups.value) {
    const list = groupStocks[g.id] || [];
    if (list.some(s => s.stockCode === stock.stockCode)) {
      notiSourceGroupId.value = g.id;
      break;
    }
  }
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
    type: 1,
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
      const listRes = await getNotificationList(currentStockCode.value);
      notiList.value = processNotiList(listRes.data.data || []);
      if (notiSourceGroupId.value) {
        fetchStocks(notiSourceGroupId.value);
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
      if (notiSourceGroupId.value) {
        fetchStocks(notiSourceGroupId.value);
      }
    }
  } catch (error) {
    console.error('Delete notification failed:', error);
  }
};

onMounted(async () => {
  initialActiveLocked = true;
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  initIntersectionObserver();
  await fetchGroups();
  // 初次进入主动加载第一个分组（IntersectionObserver 在 ref 设置后才能触发）
  await nextTick();
  if (groups.value.length > 0) {
    const firstId = groups.value[0]!.id;
    activeGroupId.value = firstId;
    if (!loadedGroups.has(firstId)) {
      loadedGroups.add(firstId);
      fetchStocks(firstId);
    }
  }
  // 初始化锚点容量观察
  setupAnchorsResize();
  initialActiveUnlockFrame = requestAnimationFrame(() => {
    initialActiveUnlockFrame = null;
    initialActiveUnlockSecondFrame = requestAnimationFrame(() => {
      initialActiveUnlockSecondFrame = null;
      initialActiveLocked = false;
      updateActiveGroupByPosition();
      window.addEventListener('scroll', scheduleActiveGroupUpdate, { passive: true });
      window.addEventListener('resize', scheduleActiveGroupUpdate);
    });
  });
});

onBeforeUnmount(() => {
  initialActiveLocked = true;
  window.removeEventListener('scroll', scheduleActiveGroupUpdate);
  window.removeEventListener('resize', scheduleActiveGroupUpdate);
  if (initialActiveUnlockFrame != null) {
    cancelAnimationFrame(initialActiveUnlockFrame);
    initialActiveUnlockFrame = null;
  }
  if (initialActiveUnlockSecondFrame != null) {
    cancelAnimationFrame(initialActiveUnlockSecondFrame);
    initialActiveUnlockSecondFrame = null;
  }
  if (activeGroupUpdateFrame != null) {
    cancelAnimationFrame(activeGroupUpdateFrame);
    activeGroupUpdateFrame = null;
  }
  if (intersectionObserver) {
    intersectionObserver.disconnect();
    intersectionObserver = null;
  }
  groupSectionRefs.clear();
  if (anchorsResizeObserver) {
    anchorsResizeObserver.disconnect();
    anchorsResizeObserver = null;
  }
});

/* ============================================================
 * 顶部锚点容量测量：根据容器宽度决定 visibleCount
 * ============================================================ */
let anchorsResizeObserver: ResizeObserver | null = null;
const setupAnchorsResize = () => {
  if (!anchorsListEl.value) return;
  anchorsResizeObserver = new ResizeObserver(() => {
    measureAnchors();
  });
  anchorsResizeObserver.observe(anchorsListEl.value);
};

let measuring = false;
const measureAnchors = async () => {
  if (measuring) return;
  measuring = true;
  // 先重置为全部显示，等下一帧测量
  visibleCount.value = groups.value.length;
  await nextTick();

  const container = anchorsListEl.value;
  if (!container) {
    measuring = false;
    return;
  }
  const containerWidth = container.clientWidth;

  const items = Array.from(container.querySelectorAll<HTMLElement>(':scope > .group-anchor:not(.group-anchor-more)'));
  const seps = Array.from(container.querySelectorAll<HTMLElement>(':scope > .group-anchor-sep'));
  // 给"更多"预留固定宽度：分隔符 + 文字按钮 + 缓冲（避免边界情况下被挤掉）
  const reservedForMore = 80;

  // 全部都能放下
  const totalWidth = items.reduce((sum, it, i) => sum + it.offsetWidth + (i > 0 ? (seps[i - 1]?.offsetWidth || 12) : 0), 0);
  if (totalWidth <= containerWidth) {
    visibleCount.value = items.length;
    measuring = false;
    return;
  }

  // 否则按顺序累加，给"更多"预留固定空间
  let used = 0;
  let count = 0;
  for (let i = 0; i < items.length; i++) {
    const sepWidth = i > 0 ? (seps[i - 1]?.offsetWidth || 12) : 0;
    const itemWidth = items[i]!.offsetWidth + sepWidth;
    if (used + itemWidth + reservedForMore > containerWidth) break;
    used += itemWidth;
    count = i + 1;
  }

  visibleCount.value = Math.max(1, count);
  measuring = false;
};

// groups 变化时重新测量
watch(() => groups.value.length, async () => {
  await nextTick();
  measureAnchors();
});
</script>

<style scoped>
/* ========================================
   Watchlist - Apple Design 深色模式
   ======================================== */

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.noti-icon {
  font-size: 16px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all var(--transition-base) var(--transition-timing);
}

.noti-icon--active {
  color: var(--color-accent);
}

.noti-icon:hover {
  color: var(--color-accent);
  transform: scale(1.1);
}

.noti-icon--active:hover {
  color: var(--color-accent-hover);
}

.watchlist-container {
  padding: 0;
  max-width: 100%;
  margin: 0 auto;
}

/* ========================================
   顶部锚点导航
   ======================================== */
.watchlist-sticky-toolbar {
  position: sticky;
  top: calc(64px + var(--spacing-sm));
  z-index: 20;
  margin-bottom: var(--spacing-lg);
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 26px rgba(36, 63, 94, 0.08);
  backdrop-filter: saturate(180%) blur(12px);
}

.group-anchors-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
  padding: 0;
  margin-bottom: 0;
}

.group-anchors-list {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 4px;
  flex: 0 1 auto;
  max-width: min(760px, 56vw);
  min-width: 0;
  overflow: hidden;
}

.group-anchor {
  cursor: pointer;
  padding: 4px 8px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  user-select: none;
  white-space: nowrap;
  flex-shrink: 0;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-anchor-more {
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: var(--color-accent);
  font-weight: var(--font-weight-medium);
}

.group-anchors-more-wrap {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 0;
  margin-right: auto;
}

.group-anchor-more.active {
  color: var(--color-accent);
  font-weight: var(--font-weight-semibold);
}

:global(.group-anchor-more-dropdown .ant-dropdown-menu) {
  width: 260px;
  max-width: min(260px, calc(100vw - 32px));
}

:global(.group-anchor-more-dropdown .ant-dropdown-menu-item) {
  overflow: hidden;
}

:global(.group-anchor-more-dropdown .ant-dropdown-menu-title-content) {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-anchor:hover {
  color: var(--color-text-primary);
  background: rgba(76, 127, 184, 0.08);
}

.group-anchor.active:not(.group-anchor-more) {
  color: var(--color-accent);
  border: 1px solid rgba(76, 127, 184, 0.32);
  background: transparent;
  font-weight: var(--font-weight-semibold);
}

.group-anchor-sep {
  color: var(--color-text-tertiary);
  user-select: none;
  padding: 0 2px;
}

.group-anchors-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-left: auto;
  flex-shrink: 0;
}

.global-filter-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: var(--color-text-secondary);
}

.global-filter-bar :deep(.ant-checkbox-wrapper) {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  line-height: 32px;
}

.global-filter-search {
  width: 240px;
  max-width: 24vw;
}

.toolbar-action-button {
  height: 32px;
  line-height: 30px;
}

@media (max-width: 980px) {
  .group-anchors-header {
    flex-wrap: wrap;
  }

  .group-anchors-list {
    flex: 1 1 100%;
    max-width: 100%;
  }

  .group-anchors-right {
    width: 100%;
    margin-left: 0;
    flex-wrap: wrap;
  }

  .global-filter-search {
    max-width: min(240px, 52vw);
  }
}

@media (max-width: 640px) {
  .watchlist-sticky-toolbar {
    top: calc(64px + 6px);
    padding: 10px 12px;
  }

  .group-anchors-right {
    justify-content: flex-start;
  }

  .global-filter-bar {
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 8px;
  }

  .global-filter-search {
    width: 100%;
    max-width: 100%;
  }

}

/* ========================================
   分组区块
   ======================================== */
.group-section {
  scroll-margin-top: 140px;
  margin-bottom: var(--spacing-2xl, 32px);
}

@media (max-width: 640px) {
  .group-section {
    scroll-margin-top: 220px;
  }
}

.group-section[data-group-id]:not([data-group-id]) {
  /* placeholder so attribute selector parses */
}

.group-section-header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xs);
  padding: 0 4px;
}

.group-section-title {
  display: inline-flex;
  align-items: center;
  flex: 0 1 auto;
  min-width: 80px;
  max-width: min(420px, calc(100% - 112px));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0;
  border: none;
  background: transparent;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  line-height: 28px;
}

.group-section-divider {
  flex: 1 1 48px;
  min-width: 24px;
  height: 1px;
  background: linear-gradient(90deg, rgba(76, 127, 184, 0.22), rgba(76, 127, 184, 0.05));
}

.group-section-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  flex: 0 0 auto;
  min-width: 0;
}

.group-section-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1;
  transition: all var(--transition-fast);
}

.group-section-more:hover {
  background: rgba(76, 127, 184, 0.08);
  color: var(--color-accent);
}

/* ========================================
   分组标签 - Segmented Control 风格（保留原样式以兼容其他位置）
   ======================================== */

.group-tags-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xl);
  gap: var(--spacing-sm);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 251, 254, 0.98));
  padding: 18px 20px;
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.group-tags-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.group-tags-right {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.custom-group-tag {
  font-size: var(--font-size-sm);
  min-height: 40px;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(110, 132, 158, 0.16);
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--transition-base) var(--transition-timing);
  color: var(--color-text-secondary);
  background: rgba(255, 255, 255, 0.72);
  user-select: none;
  font-weight: var(--font-weight-medium);
  box-shadow: 0 4px 10px rgba(25, 48, 78, 0.04);
}

.custom-group-tag:hover {
  background: rgba(255, 255, 255, 0.96);
  color: var(--color-text-primary);
  border-color: var(--color-border-hover);
  box-shadow: 0 8px 16px rgba(25, 48, 78, 0.08);
  transform: translateY(-1px);
}

.custom-group-tag.active {
  background: linear-gradient(180deg, rgba(76, 127, 184, 0.16), rgba(76, 127, 184, 0.08));
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
  border-color: rgba(76, 127, 184, 0.4);
  box-shadow: 0 12px 24px rgba(76, 127, 184, 0.16);
}

.custom-group-tag.active:hover {
  background: linear-gradient(180deg, rgba(76, 127, 184, 0.2), rgba(76, 127, 184, 0.1));
  border-color: rgba(76, 127, 184, 0.46);
}

.delete-group-icon {
  margin-left: 6px;
  font-size: 12px;
  color: inherit;
  opacity: 0.8;
  transition: opacity var(--transition-fast);
  cursor: pointer;
}

.delete-group-icon:hover {
  opacity: 1;
}

.custom-group-tag .tag-name {
  margin-right: 0 !important;
}

.custom-group-tag .tag-actions {
  display: flex;
  align-items: center;
  gap: 4px !important;
  margin-left: 4px;
}

.edit-group-icon {
  margin-left: 0;
  font-size: 13px;
  color: inherit;
  opacity: 0.7;
  transition: all var(--transition-fast);
  cursor: pointer;
}

.edit-group-icon:hover {
  opacity: 1;
  transform: scale(1.1);
}

.custom-group-tag.editing {
  padding: 0;
  background-color: transparent;
  box-shadow: none;
}

.edit-group-input {
  width: 120px;
  height: 32px;
  font-size: var(--font-size-sm);
  border-radius: var(--radius-md);
}

.custom-group-tag.add-btn {
  color: var(--color-accent);
  min-height: 42px;
  padding: 8px 18px;
  border: 1px dashed rgba(76, 127, 184, 0.26);
  border-radius: var(--radius-lg);
  background: rgba(76, 127, 184, 0.04);
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: none;
}

.custom-group-tag.add-btn:hover {
  color: var(--color-accent-hover);
  border-color: rgba(76, 127, 184, 0.36);
  background-color: rgba(76, 127, 184, 0.08);
  transform: translateY(-1px);
}

/* ========================================
   卡片网格 - Apple 风格间距
   ======================================== */

.card-grid {
  margin-bottom: var(--spacing-lg);
}

.draggable-wrapper {
  cursor: move;
  height: 100%;
}

.stock-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-elevated) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-xl) !important;
  box-shadow: var(--shadow-sm) !important;
  transition: all var(--transition-base) var(--transition-timing) !important;
  padding: 18px !important;
}

.stock-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover) !important;
  border-color: var(--color-border-hover) !important;
}

.add-stock-card {
  height: 100%;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px dashed var(--color-border) !important;
  background-color: transparent !important;
  cursor: pointer;
  transition: all var(--transition-base) var(--transition-timing);
  border-radius: var(--radius-xl) !important;
}

.add-stock-card:hover {
  border-color: var(--color-accent) !important;
  background-color: var(--color-accent-light) !important;
  transform: translateY(-2px);
}

.add-stock-card:hover .add-icon,
.add-stock-card:hover .add-text {
  color: var(--color-accent);
}

.add-icon {
  font-size: 32px;
  color: var(--color-text-tertiary);
  margin-bottom: 12px;
  transition: all var(--transition-base);
}

.add-text {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-base);
}

/* ========================================
   股票卡片内容
   ======================================== */

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.stock-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.stock-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  letter-spacing: -0.2px;
}

.stock-code {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-family: var(--font-family-mono);
  font-weight: 600;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 4px;
  border: 1px solid var(--color-border);
  letter-spacing: 0.5px;
}

.more-icon {
  font-size: 18px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 6px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.more-icon:hover {
  background-color: rgba(255, 255, 255, 0.08);
  color: var(--color-text-primary);
}

.delete-icon {
  color: var(--color-error);
  cursor: pointer;
  opacity: 0.7;
  transition: opacity var(--transition-fast);
}

.delete-icon:hover {
  opacity: 1;
}

/* ========================================
   行情数据 - Apple 柔和色彩
   ======================================== */

.quote-info {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.latest-price {
  font-size: 24px;
  font-weight: var(--font-weight-semibold);
  font-family: var(--font-family-mono);
  letter-spacing: -0.5px;
}

.change-percent {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  font-family: var(--font-family-mono);
}

.up {
  color: var(--color-error);
}

.down {
  color: var(--color-success);
}

.neutral {
  color: var(--color-text-secondary);
}

.kline-box {
  flex-grow: 1;
  min-height: 100px;
  margin-bottom: 12px;
}

/* ========================================
   基本面数据 - 弱化显示
   ======================================== */

.fundamentals {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-xs);
  padding-top: 12px;
  border-top: 1px solid var(--color-divider);
}

.fund-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.fund-item .label {
  color: var(--color-text-tertiary);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.fund-item .val {
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-semibold);
  font-family: var(--font-family-mono);
}

/* ========================================
   分红信息 - 减少饱和度
   ======================================== */

.dividends-wrap {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--color-divider);
}

.dividend-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-xs);
  margin-bottom: 4px;
}

.div-date {
  color: var(--color-text-tertiary);
  font-family: var(--font-family-mono);
}

.div-text {
  color: var(--color-error);
  font-weight: var(--font-weight-medium);
  opacity: 0.9;
}

/* ========================================
   过滤与排序控制器
   ======================================== */

.group-section-sort-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: var(--spacing-md);
  padding: 0 4px;
}

.sort-options {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex: 0 1 auto;
  max-width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  padding-bottom: 2px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.sort-options .ctrl-label {
  margin-right: var(--spacing-sm);
  white-space: nowrap;
  flex: 0 0 auto;
  color: var(--color-text-tertiary);
  font-weight: var(--font-weight-medium);
}

.sort-options .ctrl-item {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  min-width: fit-content;
  margin-left: var(--spacing-md);
  cursor: pointer;
  transition: color var(--transition-fast);
  white-space: nowrap;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-weight: var(--font-weight-medium);
}

.sort-options .ctrl-item:hover {
  color: var(--color-text-primary);
  background: rgba(255, 255, 255, 0.05);
}

.sort-options .ctrl-item.active {
  color: var(--color-accent);
  font-weight: var(--font-weight-semibold);
  background: var(--color-accent-light);
}

.sort-options .ctrl-item .anticon {
  margin-left: 2px;
  font-size: 11px;
}

/* ========================================
   空状态样式
   ======================================== */

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

/* ========================================
   搜索框样式 - 浅色主题下增强可见性
   ======================================== */

.watchlist-search-input.ant-input-affix-wrapper,
.watchlist-search-input.ant-input-affix-wrapper:hover,
.watchlist-search-input.ant-input-affix-wrapper:focus {
  background: rgba(255, 255, 255, 0.96) !important;
  border-color: rgba(110, 132, 158, 0.2) !important;
  box-shadow: 0 6px 16px rgba(25, 48, 78, 0.06) !important;
  border-radius: var(--radius-lg) !important;
}

.watchlist-search-input.ant-input-affix-wrapper-focused,
.watchlist-search-input.ant-input-affix-wrapper-focused:hover {
  background: #ffffff !important;
  border-color: rgba(76, 127, 184, 0.28) !important;
  box-shadow: 0 0 0 3px rgba(76, 127, 184, 0.1), 0 8px 18px rgba(25, 48, 78, 0.08) !important;
  border-radius: var(--radius-lg) !important;
}

.watchlist-search-input :deep(.ant-input),
.watchlist-search-input.ant-input-affix-wrapper > input.ant-input,
.watchlist-search-input.ant-input-affix-wrapper:hover > input.ant-input,
.watchlist-search-input.ant-input-affix-wrapper-focused > input.ant-input,
.watchlist-search-input :deep(.ant-input:hover),
.watchlist-search-input :deep(.ant-input:focus) {
  background: transparent !important;
  background-color: transparent !important;
  color: var(--color-text-primary) !important;
  box-shadow: none !important;
  border: none !important;
}

.watchlist-search-input :deep(.ant-input::placeholder) {
  color: var(--color-text-secondary) !important;
}

.watchlist-search-input :deep(.ant-input-prefix),
.watchlist-search-input :deep(.anticon-search) {
  color: var(--color-accent) !important;
}

.watchlist-search-input :deep(.ant-input-suffix),
.watchlist-search-input :deep(.ant-input-clear-icon) {
  color: var(--color-text-secondary) !important;
}

.watchlist-search-input :deep(.ant-input-clear-icon:hover) {
  color: var(--color-text-secondary) !important;
}

.notification-enable-switch :deep(.ant-switch-inner-checked),
.notification-enable-switch :deep(.ant-switch-inner-unchecked) {
  color: inherit;
}

/* ========================================
   通知设置 Modal - 控件对齐
   ======================================== */

/* 确保所有输入控件垂直居中对齐 */
:deep(.ant-list-item .ant-row) {
  align-items: center !important;
}

/* Select 下拉框对齐 */
:deep(.ant-list-item .ant-select) {
  display: flex;
  align-items: center;
}

:deep(.ant-list-item .ant-select-selector) {
  display: flex;
  align-items: center;
  height: 32px;
}

/* InputNumber 对齐 */
:deep(.ant-list-item .ant-input-number) {
  height: 32px;
  display: flex;
  align-items: center;
}

:deep(.ant-list-item .ant-input-number-input) {
  height: 30px;
}

/* Radio Group 对齐 */
:deep(.ant-list-item .ant-radio-group) {
  display: flex;
  align-items: center;
}

:deep(.ant-list-item .ant-radio-wrapper) {
  display: flex;
  align-items: center;
  margin: 0;
}

/* Switch 开关对齐 */
:deep(.ant-list-item .ant-switch) {
  vertical-align: middle;
}

/* 按钮对齐 */
:deep(.ant-list-item .ant-btn-link) {
  height: auto;
  line-height: 1;
  padding: 4px 8px;
}
</style>
