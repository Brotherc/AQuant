<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible style="overflow: auto; height: 100vh; position: fixed; left: 0; top: 0; bottom: 0; z-index: 1001">
      <div class="logo" />
      <a-menu v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys" theme="dark" mode="inline">
        
        <!-- Stock Data -->
        <a-sub-menu key="/stock-data">
          <template #title>
            <span>
              <stock-outlined />
              <span>股票数据</span>
            </span>
          </template>
          <a-menu-item key="/stock-data/index">
            <router-link to="/stock-data/index">股票数据</router-link>
          </a-menu-item>
        </a-sub-menu>

        <!-- Board Data -->
        <a-sub-menu key="/board">
          <template #title>
            <span>
              <appstore-outlined />
              <span>板块数据</span>
            </span>
          </template>
          <a-menu-item key="/board/index">
            <router-link to="/board/index">板块数据</router-link>
          </a-menu-item>
        </a-sub-menu>

        <!-- Indicators -->
        <a-sub-menu key="/indicators">
          <template #title>
            <span>
              <line-chart-outlined />
              <span>股票指标</span>
            </span>
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
            <span>
              <pay-circle-outlined />
              <span>分红数据</span>
            </span>
          </template>
          <a-menu-item key="/dividend/index">
            <router-link to="/dividend/index">分红数据</router-link>
          </a-menu-item>
        </a-sub-menu>
        
        <!-- Strategy -->
        <a-sub-menu key="/strategy">
          <template #title>
            <span>
              <radar-chart-outlined />
              <span>策略</span>
            </span>
          </template>
          <a-menu-item key="/strategy/dual-ma">
            <router-link to="/strategy/dual-ma">双均线策略</router-link>
          </a-menu-item>
        </a-sub-menu>

      </a-menu>
    </a-layout-sider>
    <a-layout :style="{ marginLeft: collapsed ? '80px' : '200px', transition: 'margin-left 0.2s' }">
      <a-layout-header style="background: #fff; padding: 0; position: fixed; top: 0; right: 0; z-index: 1000; width: 100%; transition: width 0.2s" :style="{ width: `calc(100% - ${collapsed ? '80px' : '200px'})` }" />
      <a-layout-content style="margin: 64px 16px 0">
        <div :style="{ padding: '24px', background: '#fff', minHeight: '360px', marginTop: '16px' }">
          <router-view />
        </div>
      </a-layout-content>
      <a-layout-footer style="text-align: center">
        AQuant ©2025 Created by AQuant Team
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>
<script lang="ts" setup>
import { ref, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import {
  StockOutlined,
  HistoryOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  AppstoreOutlined,
  PayCircleOutlined,
} from '@ant-design/icons-vue';

const route = useRoute();
const collapsed = ref<boolean>(false);
const selectedKeys = ref<string[]>([]);
const openKeys = ref<string[]>([]);

// 同步菜单状态
const syncMenuState = () => {
  const path = route.path;
  selectedKeys.value = [path];
  
  // 提取父级路径，例如 /strategy/dual-ma -> /strategy
  const parentPath = '/' + path.split('/')[1];
  if (parentPath && !openKeys.value.includes(parentPath)) {
    openKeys.value.push(parentPath);
  }
};

watch(() => route.path, () => {
  syncMenuState();
});

onMounted(() => {
  syncMenuState();
});
</script>
<style>
.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.3);
}
</style>
