<template>
  <div class="strategy-index-container">
    <div class="strategy-layout">
      <!-- 左侧策略列表导航 -->
      <aside class="strategy-sidebar">
        <div class="strategy-sidebar__header">
          <div class="strategy-sidebar__title">
            <span>策略列表</span>
          </div>
          <span class="strategy-count-badge">{{ strategyList.length }} 个策略</span>
        </div>

        <div class="strategy-menu-list">
          <div
            v-for="item in strategyList"
            :key="item.key"
            class="strategy-menu-item"
            :class="{ 'is-active': activeStrategyKey === item.key }"
            @click="handleSelectStrategy(item.key)"
          >
            <div class="strategy-menu-item__icon-wrap">
              <component :is="item.icon" />
            </div>
            <div class="strategy-menu-item__main">
              <div class="strategy-menu-item__top">
                <span class="strategy-menu-item__name">{{ item.name }}</span>
                <span class="strategy-menu-item__tag">{{ item.tag }}</span>
              </div>
              <div class="strategy-menu-item__desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧对应策略的数据与工作台 -->
      <main class="strategy-content-wrap">
        <component :is="activeComponent" :key="activeStrategyKey" />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, markRaw } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  LineChartOutlined,
  ThunderboltOutlined,
  StockOutlined,
  AppstoreOutlined,
} from '@ant-design/icons-vue';
import DualMA from './DualMA.vue';
import Momentum from './Momentum.vue';
import MACD from './MACD.vue';
import Grid from './Grid.vue';

interface StrategyItem {
  key: string;
  name: string;
  tag: string;
  desc: string;
  icon: any;
  component: any;
}

const route = useRoute();
const router = useRouter();

const strategyList: StrategyItem[] = [
  {
    key: 'dual-ma',
    name: '双均线策略',
    tag: '趋势跟踪',
    desc: '基于短/长周期均线金叉死叉信号与收益回测',
    icon: markRaw(LineChartOutlined),
    component: markRaw(DualMA),
  },
  {
    key: 'momentum',
    name: '动量策略',
    tag: '强弱动能',
    desc: '基于回望期多空动量收益率筛选与胜率回测',
    icon: markRaw(ThunderboltOutlined),
    component: markRaw(Momentum),
  },
  {
    key: 'macd',
    name: 'MACD策略',
    tag: '趋势动能',
    desc: '基于DIF与DEA金叉死叉识别趋势拐点并进行回测',
    icon: markRaw(StockOutlined),
    component: markRaw(MACD),
  },
  {
    key: 'grid',
    name: '网格交易策略',
    tag: '震荡交易',
    desc: '按固定涨跌幅分层低买高卖，控制仓位并进行收益回测',
    icon: markRaw(AppstoreOutlined),
    component: markRaw(Grid),
  },
];

const activeStrategyKey = ref<string>('dual-ma');

// 根据 activeStrategyKey 动态加载对应策略组件
const activeComponent = computed(() => {
  const target = strategyList.find((item) => item.key === activeStrategyKey.value);
  return target ? target.component : DualMA;
});

// 切换选中的策略
const handleSelectStrategy = (key: string) => {
  if (activeStrategyKey.value === key) return;
  activeStrategyKey.value = key;
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      type: key,
    },
  });
};

// 从路由参数初始化或同步
const syncFromRoute = () => {
  const typeParam = route.query.type as string;
  if (typeParam && strategyList.some((item) => item.key === typeParam)) {
    activeStrategyKey.value = typeParam;
  } else {
    activeStrategyKey.value = 'dual-ma';
  }
};

watch(
  () => route.query.type,
  () => {
    syncFromRoute();
  }
);

onMounted(() => {
  syncFromRoute();
});
</script>

<style scoped>
.strategy-index-container {
  width: 100%;
}

.strategy-layout {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
}

/* ==========================================================================
   左侧策略列表导航侧边栏（素雅克制设计）
   ========================================================================== */
.strategy-sidebar {
  width: 250px;
  min-width: 250px;
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  padding: 12px 10px;
  flex-shrink: 0;
  position: sticky;
  top: 16px;
}

.strategy-sidebar__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px 10px 4px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 8px;
}

.strategy-sidebar__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.strategy-count-badge {
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  background: #f1f5f9;
  padding: 1px 7px;
  border-radius: 6px;
}

.strategy-menu-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.strategy-menu-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.strategy-menu-item:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
}

.strategy-menu-item.is-active {
  background: #ffffff;
  border-color: #0f172a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.strategy-menu-item__icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
  margin-top: 2px;
  background: #f1f5f9;
  color: #475569;
  transition: all 0.15s ease;
}

.strategy-menu-item.is-active .strategy-menu-item__icon-wrap {
  background: #0f172a;
  color: #ffffff;
}

.strategy-menu-item__main {
  flex: 1;
  min-width: 0;
}

.strategy-menu-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 3px;
}

.strategy-menu-item__name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.strategy-menu-item.is-active .strategy-menu-item__name {
  color: #0f172a;
}

.strategy-menu-item__tag {
  font-size: 10.5px;
  font-weight: 400;
  padding: 0 5px;
  border-radius: 4px;
  white-space: nowrap;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.strategy-menu-item.is-active .strategy-menu-item__tag {
  background: #e2e8f0;
  color: #334155;
  border-color: #cbd5e1;
}

.strategy-menu-item__desc {
  font-size: 11px;
  color: #64748b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ==========================================================================
   右侧策略内容工作台
   ========================================================================== */
.strategy-content-wrap {
  flex: 1;
  min-width: 0;
}

/* ==========================================================================
   响应式断点适配
   ========================================================================== */
@media (max-width: 992px) {
  .strategy-layout {
    flex-direction: column;
  }

  .strategy-sidebar {
    width: 100%;
    min-width: 100%;
    position: static;
  }

  .strategy-menu-list {
    flex-direction: row;
  }

  .strategy-menu-item {
    flex: 1;
  }
}

@media (max-width: 640px) {
  .strategy-menu-list {
    flex-direction: column;
  }
}
</style>
