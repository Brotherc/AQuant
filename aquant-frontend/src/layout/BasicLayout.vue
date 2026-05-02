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
            <a-sub-menu key="/watchlist" popupClassName="top-nav-popup top-nav-popup-compact">
              <template #title>
                <heart-outlined />
                <span class="nav-text">自选股票</span>
              </template>
              <a-menu-item key="/watchlist/index">
                <router-link to="/watchlist/index">我的自选</router-link>
              </a-menu-item>
            </a-sub-menu>

            <!-- Base Data (Merged) -->
            <a-sub-menu key="/data" popupClassName="top-nav-popup top-nav-popup-compact">
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
            <a-sub-menu key="/indicators" popupClassName="top-nav-popup">
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
            <a-sub-menu key="/dividend" popupClassName="top-nav-popup top-nav-popup-compact">
              <template #title>
                <pay-circle-outlined />
                <span class="nav-text">分红数据</span>
              </template>
              <a-menu-item key="/dividend/index">
                <router-link to="/dividend/index">分红数据</router-link>
              </a-menu-item>
            </a-sub-menu>
            
            <!-- Strategy -->
            <a-sub-menu key="/strategy" popupClassName="top-nav-popup top-nav-popup-compact">
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

            <a-sub-menu key="/finance-sites" popupClassName="top-nav-popup top-nav-popup-compact">
              <template #title>
                <global-outlined />
                <span class="nav-text">投资工具</span>
              </template>
              <a-menu-item key="/finance-sites/index">
                <router-link to="/finance-sites/index">常用网站</router-link>
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
                <a-menu-item key="updateEmail" @click="showUpdateEmailModal">
                  <mail-outlined />
                  <span style="margin-left: 8px;">修改邮箱</span>
                </a-menu-item>
                <a-menu-divider />
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
        <div v-if="currentRouteMeta" class="page-context">
          <a-breadcrumb class="page-breadcrumb">
            <a-breadcrumb-item>{{ currentRouteMeta.parent }}</a-breadcrumb-item>
            <a-breadcrumb-item>{{ currentRouteMeta.child }}</a-breadcrumb-item>
          </a-breadcrumb>
          <div class="page-title">{{ currentRouteMeta.child }}</div>
        </div>
        <router-view />
      </div>
    </a-layout-content>

    <a-layout-footer class="c-footer">
      AQuant ©2025 Created by AQuant Team
    </a-layout-footer>

    <!-- 修改邮箱 Modal -->
    <a-modal
      v-model:visible="emailModalVisible"
      title="修改邮箱"
      @ok="handleUpdateEmail"
      :confirmLoading="emailLoading"
      destroyOnClose
    >
      <a-form layout="vertical">
        <a-form-item label="新邮箱地址" required>
          <a-input v-model:value="emailForm.email" placeholder="请输入您的新邮箱" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-layout>
</template>

<script lang="ts" setup>
import { computed, ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  StockOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  PayCircleOutlined,
  GlobalOutlined,
  HeartOutlined,
  LogoutOutlined,
  LoginOutlined,
  MailOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { updateEmail } from '@/api/auth';

const route = useRoute();
const router = useRouter();
const selectedKeys = ref<string[]>([]);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const nickname = ref(localStorage.getItem('nickname') || '用户');

const routeMetaMap: Record<string, { parent: string; child: string }> = {
  '/watchlist/index': { parent: '自选股票', child: '我的自选' },
  '/stock-data/index': { parent: '股票数据', child: '股票详情' },
  '/board/index': { parent: '股票数据', child: '行业板块' },
  '/indicators/dupont': { parent: '股票指标', child: '杜邦分析' },
  '/indicators/growth': { parent: '股票指标', child: '行业成长性指标' },
  '/indicators/valuation': { parent: '股票指标', child: '估值指标' },
  '/dividend/index': { parent: '分红数据', child: '分红数据' },
  '/strategy/dual-ma': { parent: '量化策略', child: '双均线策略' },
  '/strategy/momentum': { parent: '量化策略', child: '动量策略' },
  '/finance-sites/index': { parent: '投资工具', child: '常用网站' },
};

const currentRouteMeta = computed(() => routeMetaMap[route.path]);

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

// 修改邮箱相关
const emailModalVisible = ref(false);
const emailLoading = ref(false);
const emailForm = ref({ email: '' });

const showUpdateEmailModal = () => {
  emailForm.value.email = '';
  emailModalVisible.value = true;
};

const handleUpdateEmail = async () => {
  if (!emailForm.value.email) {
    message.warning('请输入邮箱地址');
    return;
  }
  // 简单的正则校验
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(emailForm.value.email)) {
    message.warning('请输入正确的邮箱格式');
    return;
  }

  emailLoading.value = true;
  try {
    const res = await updateEmail(emailForm.value);
    if (res.data.success) {
      message.success('邮箱修改成功');
      emailModalVisible.value = false;
    }
  } catch (error) {
    console.error('Failed to update email:', error);
  } finally {
    emailLoading.value = false;
  }
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
  flex-shrink: 0;
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
  white-space: nowrap;
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

.page-context {
  margin-bottom: 20px;
  padding: 16px 20px;
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(24, 144, 255, 0.08), rgba(24, 144, 255, 0.02));
  border: 1px solid rgba(24, 144, 255, 0.12);
}

.page-breadcrumb {
  margin-bottom: 8px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f1f1f;
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

<style>
.top-nav-popup.ant-menu-submenu-popup > .ant-menu {
  min-width: 132px;
}

.top-nav-popup-compact.ant-menu-submenu-popup > .ant-menu {
  min-width: 112px;
}

.top-nav-popup.ant-menu-submenu-popup .ant-menu-item,
.top-nav-popup.ant-menu-submenu-popup .ant-menu-submenu-title {
  padding-inline: 14px !important;
}
</style>
