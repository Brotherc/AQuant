<template>
  <a-layout class="c-end-layout">
    <a-layout-header class="c-header">
      <div class="header-container">
        <!-- 左侧 Logo 区 -->
        <div class="logo-box">
          <div class="logo">AQuant 量化</div>
        </div>
        
        <!-- 中间 Navigation 区 -->
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

        <!-- 右侧用户区 -->
        <div class="user-box">
          <!-- 未登录：显示登录按钮 -->
          <div v-if="!isLoggedIn" class="login-trigger" @click="goLogin">
            <login-outlined />
            <span style="margin-left: 6px;">登录</span>
          </div>

          <!-- 已登录：显示用户头像 + 退出 -->
          <a-dropdown v-else>
            <div class="user-trigger">
              <a-avatar size="small" style="background-color: #1890ff;">
                {{ nickname.charAt(0) }}
              </a-avatar>
              <span class="user-nickname">{{ nickname }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">
                  <logout-outlined />
                  <span style="margin-left: 8px;">退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
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
import { useRoute, useRouter } from 'vue-router';
import {
  StockOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  PayCircleOutlined,
  HeartOutlined,
  LogoutOutlined,
  LoginOutlined
} from '@ant-design/icons-vue';

const route = useRoute();
const router = useRouter();
const selectedKeys = ref<string[]>([]);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const nickname = ref(localStorage.getItem('nickname') || '用户');

// 同步菜单状态
const syncMenuState = () => {
  const path = route.path;
  selectedKeys.value = [path];
};

watch(() => route.path, () => {
  syncMenuState();
  // 路由切换时刷新登录状态（登录完成后跳回来）
  isLoggedIn.value = !!localStorage.getItem('token');
  nickname.value = localStorage.getItem('nickname') || '用户';
});

onMounted(() => {
  syncMenuState();
});

const goLogin = () => {
  router.push('/login');
};

const handleLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('nickname');
  isLoggedIn.value = false;
  nickname.value = '用户';
};
</script>

<style scoped>
.c-end-layout {
  min-height: 100vh;
  background: #f5f7fa;
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

/* 头部内容主轴 */
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1400px;
  padding: 0 24px;
}

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

.logo {
  height: 64px;
  line-height: 64px;
  color: #1890ff;
  font-size: 22px;
  font-weight: 800;
  cursor: pointer;
  letter-spacing: 1px;
}

.c-menu {
  line-height: 64px;
  border-bottom: none;
  background: transparent;
  flex: 1;
  justify-content: flex-end;
  font-size: 15px;
}

:deep(.ant-menu-horizontal) {
  border-bottom: none !important;
}

/* 用户信息区 */
.user-box {
  display: flex;
  align-items: center;
  margin-left: 24px;
  flex-shrink: 0;
}

/* 未登录的"登录"文字 */
.login-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  color: #000;
  font-weight: 500;
  transition: color 0.2s;
}

.login-trigger:hover {
  color: #1890ff;
}

/* 已登录的用户区 */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  height: 32px;
  padding: 0 10px;
  border-radius: 4px;
  margin: 16px 0;
  transition: background 0.2s;
}

.user-trigger:hover {
  background: rgba(0, 0, 0, 0.04);
}

.user-nickname {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.c-content {
  margin-top: 64px; 
  padding: 24px 0;
}

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

.nav-text {
  margin-left: 6px;
}
</style>
