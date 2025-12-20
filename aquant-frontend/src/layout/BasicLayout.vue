<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo" />
      <a-menu v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys" theme="dark" mode="inline">
        
        <!-- Stock Data -->
        <a-sub-menu key="sub1">
          <template #title>
            <span>
              <stock-outlined />
              <span>股票数据</span>
            </span>
          </template>
          <a-menu-item key="1">
            <router-link to="/stock-data/index">股票数据</router-link>
          </a-menu-item>
        </a-sub-menu>

        <!-- History -->
        <a-sub-menu key="sub2">
          <template #title>
            <span>
              <history-outlined />
              <span>历史行情</span>
            </span>
          </template>
          <a-menu-item key="2">
            <router-link to="/history/index">历史行情</router-link>
          </a-menu-item>
        </a-sub-menu>

        <!-- Indicators -->
        <a-sub-menu key="sub3">
          <template #title>
            <span>
              <line-chart-outlined />
              <span>股票指标</span>
            </span>
          </template>
          <a-menu-item key="3">
            <router-link to="/indicators/dupont">杜邦分析</router-link>
          </a-menu-item>
          <a-menu-item key="4">
            <router-link to="/indicators/growth">行业成长性指标</router-link>
          </a-menu-item>
          <a-menu-item key="5">
            <router-link to="/indicators/valuation">估值指标</router-link>
          </a-menu-item>
        </a-sub-menu>

      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; padding: 0" />
      <a-layout-content style="margin: 0 16px">
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
import { ref } from 'vue';
import {
  StockOutlined,
  HistoryOutlined,
  LineChartOutlined,
} from '@ant-design/icons-vue';
const collapsed = ref<boolean>(false);
const selectedKeys = ref<string[]>(['1']);
const openKeys = ref<string[]>(['sub1', 'sub2', 'sub3']);
</script>
<style>
.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.3);
}
</style>
