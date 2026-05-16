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
            <a-sub-menu
              v-for="group in navigationGroups"
              :key="group.key"
              :popupClassName="group.popupClassName"
            >
              <template #title>
                <component :is="group.icon" />
                <span class="nav-text">{{ group.title }}</span>
              </template>
              <a-menu-item
                v-for="child in group.children"
                :key="child.key"
                @click="handleNavigate(child.key)"
              >
                {{ child.label }}
              </a-menu-item>
            </a-sub-menu>
          </a-menu>
        </div>

        <div class="header-actions">
          <a-button type="text" class="mobile-nav-trigger" @click="openNavDrawer">
            <template #icon>
              <menu-outlined />
            </template>
          </a-button>

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
                <a-avatar size="small" style="background-color: var(--color-bg-surface); color: var(--color-accent);">
                  <template #icon><user-outlined /></template>
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
      </div>
    </a-layout-header>

    <a-layout-content class="c-content">
      <div class="content-container">
        <div v-if="currentRouteMeta" class="page-context">
          <a-breadcrumb class="page-breadcrumb">
            <a-breadcrumb-item>
              <span class="page-breadcrumb-parent">{{ currentRouteMeta.parent }}</span>
            </a-breadcrumb-item>
            <a-breadcrumb-item>
              <span class="page-breadcrumb-current">{{ currentRouteMeta.child }}</span>
            </a-breadcrumb-item>
          </a-breadcrumb>
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

    <a-drawer
      v-model:visible="navDrawerVisible"
      placement="right"
      title="导航菜单"
      width="320"
      class="mobile-nav-drawer"
    >
      <a-menu
        mode="inline"
        :selectedKeys="selectedKeys"
        v-model:openKeys="drawerOpenKeys"
      >
        <a-sub-menu v-for="group in navigationGroups" :key="group.key">
          <template #title>
            <span class="mobile-nav-title">
              <component :is="group.icon" />
              <span>{{ group.title }}</span>
            </span>
          </template>
          <a-menu-item
            v-for="child in group.children"
            :key="child.key"
            @click="handleDrawerNavigate(child.key)"
          >
            {{ child.label }}
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-drawer>
  </a-layout>
</template>

<script lang="ts" setup>
import { computed, ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  StockOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  HeartOutlined,
  LogoutOutlined,
  LoginOutlined,
  MailOutlined,
  MenuOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { updateEmail } from '@/api/auth';

type NavigationChild = {
  key: string;
  label: string;
};

type NavigationGroup = {
  key: string;
  title: string;
  icon: any;
  popupClassName: string;
  children: NavigationChild[];
};

const route = useRoute();
const router = useRouter();
const selectedKeys = ref<string[]>([]);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const nickname = ref(localStorage.getItem('nickname') || '用户');
const navDrawerVisible = ref(false);
const drawerOpenKeys = ref<string[]>([]);

const navigationGroups: NavigationGroup[] = [
  {
    key: '/watchlist',
    title: '自选股票',
    icon: HeartOutlined,
    popupClassName: 'top-nav-popup top-nav-popup-compact',
    children: [{ key: '/watchlist/index', label: '我的自选' }]
  },
  {
    key: '/data',
    title: '股票数据',
    icon: StockOutlined,
    popupClassName: 'top-nav-popup top-nav-popup-compact',
    children: [
      { key: '/stock-data/index', label: '股票详情' },
      { key: '/board/index', label: '行业板块' }
    ]
  },
  {
    key: '/indicators',
    title: '股票指标',
    icon: LineChartOutlined,
    popupClassName: 'top-nav-popup',
    children: [
      { key: '/indicators/dupont', label: '杜邦分析' },
      { key: '/indicators/growth', label: '行业成长性指标' },
      { key: '/indicators/valuation', label: '估值指标' },
      { key: '/dividend/index', label: '分红数据' }
    ]
  },
  {
    key: '/strategy',
    title: '量化策略',
    icon: RadarChartOutlined,
    popupClassName: 'top-nav-popup top-nav-popup-compact',
    children: [
      { key: '/strategy/dual-ma', label: '双均线策略' },
      { key: '/strategy/momentum', label: '动量策略' }
    ]
  },
  {
    key: '/article',
    title: '投资社区',
    icon: FileTextOutlined,
    popupClassName: 'top-nav-popup top-nav-popup-compact',
    children: [
      { key: '/article/public', label: '广场' },
      { key: '/article/my', label: '我的笔记' },
      { key: '/finance-sites/index', label: '常用网站' }
    ]
  }
];

const currentRouteMeta = computed(() => {
  for (const group of navigationGroups) {
    const child = group.children.find((item) => item.key === route.path);
    if (child) {
      return { parent: group.title, child: child.label };
    }
  }
  return undefined;
});

// 同步菜单状态
const syncMenuState = () => {
  const path = route.path;
  selectedKeys.value = [path];
  const activeGroup = navigationGroups.find((group) => group.children.some((child) => child.key === path));
  drawerOpenKeys.value = activeGroup ? [activeGroup.key] : [];
};

watch(() => route.path, () => {
  syncMenuState();
  navDrawerVisible.value = false;
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

const handleNavigate = (path: string) => {
  if (route.path !== path) {
    router.push(path);
  }
};

const handleDrawerNavigate = (path: string) => {
  navDrawerVisible.value = false;
  handleNavigate(path);
};

const openNavDrawer = () => {
  syncMenuState();
  navDrawerVisible.value = true;
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
  background: var(--color-bg-primary);
}

/* 顶部导航 - Apple Style */
.c-header {
  position: fixed;
  top: 0;
  width: 100%;
  height: 64px;
  z-index: 1000;
  background: var(--color-bg-header);
  border-bottom: 1px solid var(--color-divider);
  padding: 0;
  box-shadow: 0 12px 28px rgba(36, 63, 94, 0.08);
  backdrop-filter: blur(18px);
  display: flex;
  justify-content: center;
}

/* 头部内容主轴 - 全宽布局 */
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 100%;
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

.header-actions {
  display: flex;
  align-items: center;
  margin-left: 24px;
  flex-shrink: 0;
}

.logo {
  height: 64px;
  line-height: 64px;
  color: var(--color-text-primary);
  font-size: 20px;
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  letter-spacing: -0.5px;
  white-space: nowrap;
}

.c-menu {
  line-height: 64px;
  border-bottom: none;
  background: transparent;
  flex: 1;
  justify-content: flex-end;
  font-size: 14px;
}

.c-menu :deep(.ant-menu-submenu) {
  margin: 0 2px;
  padding: 0 10px;
}

.c-menu :deep(.ant-menu-item) {
  color: var(--color-text-secondary);
  border-bottom: 2px solid transparent;
  transition: all var(--transition-base) var(--transition-timing);
  margin: 0;
  padding: 0 20px;
}

.c-menu :deep(.ant-menu-item:hover) {
  color: var(--color-text-primary);
  background: transparent;
}

.c-menu :deep(.ant-menu-item-selected) {
  color: var(--color-accent);
  background: transparent;
  border-bottom-color: var(--color-accent);
  font-weight: var(--font-weight-semibold);
}

.c-menu :deep(.ant-menu-submenu-title) {
  color: var(--color-text-secondary);
  transition: all var(--transition-base) var(--transition-timing);
  padding: 0 !important;
}

.c-menu :deep(.ant-menu-submenu-title:hover) {
  color: var(--color-text-primary);
  background: transparent;
}

.c-menu :deep(.ant-menu-submenu-selected .ant-menu-submenu-title) {
  color: var(--color-accent);
  font-weight: var(--font-weight-semibold);
}

.c-menu :deep(.ant-menu-submenu-title .anticon) {
  margin-right: 4px;
  font-size: 14px;
}

.nav-text {
  margin-left: 0;
}

:deep(.ant-menu-horizontal) {
  border-bottom: none !important;
}

.mobile-nav-trigger {
  display: none;
  margin-right: 8px;
  color: var(--color-text-secondary);
}

.mobile-nav-trigger:hover {
  color: var(--color-text-primary);
}

/* 用户信息区 */
.user-box {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* 未登录的"登录"文字 */
.login-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
  padding: 6px 12px;
  border-radius: var(--radius-md);
  transition: all var(--transition-base) var(--transition-timing);
}

.login-trigger:hover {
  color: var(--color-accent);
  background: var(--color-bg-surface-hover);
}

/* 已登录的用户区 */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  height: 36px;
  padding: 0 12px;
  border-radius: var(--radius-md);
  margin: 14px 0;
  transition: all var(--transition-base) var(--transition-timing);
}

.user-trigger:hover {
  background: var(--color-bg-surface);
}

.user-nickname {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.c-content {
  margin-top: 64px; 
  padding: 32px 0;
  background: var(--color-bg-primary);
}

.content-container {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 24px;
  min-height: calc(100vh - 64px - 70px);
}

.page-context {
  margin-bottom: 20px;
  padding: 10px 0 4px;
  border-radius: var(--radius-lg);
  background: transparent;
  border: none;
}

.page-breadcrumb {
  margin-bottom: 0;
}

.page-breadcrumb :deep(.ant-breadcrumb-link) {
  color: inherit;
}

.page-breadcrumb :deep(.ant-breadcrumb-link:hover) {
  color: var(--color-text-primary);
}

.page-breadcrumb :deep(.ant-breadcrumb-separator) {
  margin-inline: 12px;
  color: var(--color-text-tertiary);
  font-size: 18px;
}

.page-breadcrumb-parent {
  color: var(--color-text-secondary);
  font-size: 18px;
  font-weight: var(--font-weight-medium);
  letter-spacing: -0.2px;
}

.page-breadcrumb-current {
  color: var(--color-text-primary);
  font-size: 19px;
  font-weight: var(--font-weight-semibold);
  letter-spacing: -0.3px;
}

@media (max-width: 768px) {
  .page-breadcrumb-parent {
    font-size: 16px;
  }

  .page-breadcrumb-current {
    font-size: 17px;
  }

  .page-breadcrumb :deep(.ant-breadcrumb-separator) {
    margin-inline: 8px;
    font-size: 16px;
  }
}

.c-footer {
  text-align: center;
  color: var(--color-text-tertiary);
  background: transparent;
  padding: 24px 0;
  font-size: var(--font-size-xs);
}

.nav-text {
  margin-left: 0;
}

@media (max-width: 1180px) {
  .header-container {
    padding: 0 16px;
  }

  .content-container {
    padding: 0 16px;
  }

  .menu-box {
    display: none;
  }

  .mobile-nav-trigger {
    display: inline-flex;
  }

  .header-actions {
    margin-left: auto;
  }

  .user-nickname {
    display: none;
  }
}
</style>

<style>
.top-nav-popup.ant-menu-submenu-popup > .ant-menu {
  min-width: 132px;
  padding: 0 !important;
  overflow: hidden;
}

.top-nav-popup-compact.ant-menu-submenu-popup > .ant-menu {
  min-width: 112px;
}

.top-nav-popup.ant-menu-submenu-popup .ant-menu-item,
.top-nav-popup.ant-menu-submenu-popup .ant-menu-submenu-title {
  display: flex !important;
  align-items: center;
  width: 100% !important;
  padding-inline: 14px !important;
  margin: 0 !important;
  border-radius: 0 !important;
  box-sizing: border-box;
}

.mobile-nav-drawer .ant-drawer-body {
  padding: 12px 0;
}

.mobile-nav-drawer .ant-menu {
  border-inline-end: none !important;
}

.mobile-nav-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
