<template>
  <a-layout class="c-end-layout">
    <a-layout-header class="c-header">
      <div class="header-container">
        <!-- 左侧 Logo 区 -->
        <div class="logo-box">
          <div class="logo">AQuant 量化</div>
        </div>
        
        <!-- 右侧 Navigation 区 -->
        <div class="menu-box">
          <a-menu v-model:selectedKeys="selectedKeys" theme="light" mode="horizontal" class="c-menu">
            
            <!-- Watchlist -->
            <a-sub-menu key="/watchlist">
              <template #title>
                <heart-outlined />
                <span class="nav-text">自选股票</span>
              </template>
              <a-menu-item key="/watchlist/index">
                <router-link to="/watchlist/index">我的自选</router-link>
              </a-menu-item>
            </a-sub-menu>

            <!-- Base Data (Merged) -->
            <a-sub-menu key="/data">
              <template #title>
                <stock-outlined />
                <span class="nav-text">股票数据</span>
              </template>
              <a-menu-item key="/stock-data/index">
                <router-link to="/stock-data/index">股票详情</router-link>
              </a-menu-item>
              <a-menu-item key="/board/index">
                <router-link to="/board/index">行业板块</router-link>
              </a-menu-item>
            </a-sub-menu>

            <!-- Indicators -->
            <a-sub-menu key="/indicators">
              <template #title>
                <line-chart-outlined />
                <span class="nav-text">股票指标</span>
              </template>
              <a-menu-item key="/indicators/dupont">
                <router-link to="/indicators/dupont">杜邦分析</router-link>
              </a-menu-item>
              <a-menu-item key="/indicators/growth">
                <router-link to="/indicators/growth">行业成长性指标</router-link>
              </a-menu-item>
              <a-menu-item key="/indicators/valuation">
                <router-link to="/indicators/valuation">估值指标</router-link>
              </a-menu-item>
            </a-sub-menu>

            <!-- Dividend Data -->
            <a-sub-menu key="/dividend">
              <template #title>
                <pay-circle-outlined />
                <span class="nav-text">分红数据</span>
              </template>
              <a-menu-item key="/dividend/index">
                <router-link to="/dividend/index">分红数据</router-link>
              </a-menu-item>
            </a-sub-menu>
            
            <!-- Strategy -->
            <a-sub-menu key="/strategy">
              <template #title>
                <radar-chart-outlined />
                <span class="nav-text">量化策略</span>
              </template>
              <a-menu-item key="/strategy/dual-ma">
                <router-link to="/strategy/dual-ma">双均线策略</router-link>
              </a-menu-item>
              <a-menu-item key="/strategy/momentum">
                <router-link to="/strategy/momentum">动量策略</router-link>
              </a-menu-item>
            </a-sub-menu>

          </a-menu>
        </div>
      </div>
    </a-layout-header>

    <a-layout-content class="c-content">
      <div class="content-container">
        <router-view />
      </div>
    </a-layout-content>

    <a-layout-footer class="c-footer">
      AQuant ©2025 Created by AQuant Team
    </a-layout-footer>
  </a-layout>
</template>

<script lang="ts" setup>
import { ref, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import {
  StockOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  PayCircleOutlined,
  HeartOutlined
} from '@ant-design/icons-vue';

const route = useRoute();
const selectedKeys = ref<string[]>([]);

// 同步菜单状态
const syncMenuState = () => {
  const path = route.path;
  selectedKeys.value = [path];
};

watch(() => route.path, () => {
  syncMenuState();
});

onMounted(() => {
  syncMenuState();
});
</script>

<style scoped>
.c-end-layout {
  min-height: 100vh;
  background: #f5f7fa; /* 清爽通透的 C 端底色 */
}

/* 顶部玻璃态导航 */
.c-header {
  position: fixed;
  top: 0;
  width: 100%;
  height: 64px;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: saturate(180%) blur(20px);
  padding: 0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);
  display: flex;
  justify-content: center;
}

/* 头部内容主轴，约束最大宽度 */
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1400px;
  padding: 0 24px;
}

/* 拆分为独立的 flex 区块 */
.logo-box {
  display: flex;
  align-items: center;
}

.menu-box {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: flex-end;
}

/* C 端年轻化的 Logo */
.logo {
  height: 64px;
  line-height: 64px;
  color: #1890ff;
  font-size: 22px;
  font-weight: 800;
  cursor: pointer;
  letter-spacing: 1px;
}

/* 菜单去边框并靠右 */
.c-menu {
  line-height: 64px;
  border-bottom: none;
  background: transparent;
  flex: 1;
  justify-content: flex-end;
  font-size: 15px;
}

/* 消除 Ant Design 默认选中时过粗的底部线条 */
:deep(.ant-menu-horizontal) {
  border-bottom: none !important;
}

/* 核心视窗，往下挪开 Header 的高度 */
.c-content {
  margin-top: 64px; 
  padding: 24px 0;
}

/* 内容受控容器，不再全屏无脑延伸 */
.content-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  min-height: calc(100vh - 64px - 70px);
}

.c-footer {
  text-align: center;
  color: #a0a0a0;
  background: transparent;
  padding: 24px 0;
}
/* 菜单文字间距 */
.nav-text {
  margin-left: 6px;
}
</style>
