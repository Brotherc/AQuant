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
            <template v-for="group in navigationGroups" :key="group.key">
              <a-menu-item
                v-if="group.path"
                :key="group.path"
                @click="handleNavigate(group.path)"
              >
                <component :is="group.icon" />
                <span class="nav-text">{{ group.title }}</span>
              </a-menu-item>
              <a-sub-menu
                v-else
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
            </template>
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
                  <a-menu-item key="eastmoneyCookie" @click="showCookieModal">
                    <key-outlined />
                    <span style="margin-left: 8px;">东财 Cookie</span>
                  </a-menu-item>
                  <a-menu-item key="jywgCookie" @click="showJywgCookieModal">
                    <safety-certificate-outlined />
                    <span style="margin-left: 8px;">交易 Cookie</span>
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
          <div class="page-context-left">
            <a-breadcrumb v-if="currentRouteMeta.parent || currentRouteMeta.child" class="page-breadcrumb">
              <a-breadcrumb-item v-if="currentRouteMeta.parent && currentRouteMeta.parent !== currentRouteMeta.child">
                <span class="page-breadcrumb-parent">{{ currentRouteMeta.parent }}</span>
              </a-breadcrumb-item>
              <a-breadcrumb-item v-if="currentRouteMeta.child">
                <span class="page-breadcrumb-current">{{ currentRouteMeta.child }}</span>
              </a-breadcrumb-item>
            </a-breadcrumb>
            <div id="page-header-extra-left"></div>
          </div>
          <div id="page-header-extra"></div>
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

    <!-- 东财 Cookie Modal -->
    <a-modal
      v-model:visible="cookieModalVisible"
      title="东财会话 Cookie"
      @ok="handleUpdateCookie"
      :confirmLoading="cookieSaving"
      destroyOnClose
    >
      <a-alert
        v-if="cookieConfigured !== null"
        :message="cookieConfigured ? '当前已配置会话 Cookie' : '当前未配置会话 Cookie'"
        :type="cookieConfigured ? 'success' : 'warning'"
        show-icon
        style="margin-bottom: 12px;"
      />
      <a-form layout="vertical">
        <a-form-item label="完整 Cookie 请求头字符串" required>
          <a-textarea
            v-model:value="cookieForm.cookie"
            :rows="4"
            placeholder="qgqp_b_id=...; ct=...; ut=...; ..."
          />
        </a-form-item>
      </a-form>
      <div class="cookie-help">
        <p>获取方式：① 浏览器打开东方财富行情中心页面，如遇滑块验证先完成；② F12 → Network → 任意 push2 请求 → Request Headers → 复制完整 Cookie 值；③ 粘贴保存后立即生效，无需重启。</p>
        <p>Cookie 有时效（重度使用约 30-40 分钟），失效后东财同步会自动熔断并提示，重复上述步骤更新即可。</p>
      </div>
    </a-modal>

    <!-- 交易 Cookie（jywg 网页交易会话）Modal -->
    <a-modal
      v-model:visible="jywgModalVisible"
      title="交易 Cookie（东财证券网页交易）"
      @ok="handleUpdateJywgCookie"
      :confirmLoading="jywgSaving"
      destroyOnClose
      @cancel="stopJywgCountdown"
      @after-close="stopJywgCountdown"
    >
      <a-alert
        v-if="jywgStatus !== null"
        :message="jywgStatusMessage"
        :type="jywgStatusType"
        show-icon
        style="margin-bottom: 12px;"
      />
      <a-form layout="vertical">
        <a-form-item label="在线时长" required>
          <a-radio-group v-model:value="jywgForm.durationMinutes">
            <a-radio-button :value="15">15 分钟</a-radio-button>
            <a-radio-button :value="30">30 分钟</a-radio-button>
            <a-radio-button :value="180">3 小时</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="完整 Cookie 请求头字符串" required>
          <a-textarea
            v-model:value="jywgForm.cookie"
            :rows="4"
            placeholder="浏览器登录 jywg.eastmoneysec.com 后从 DevTools 复制"
          />
        </a-form-item>
        <a-form-item label="validatekey" required>
          <a-input
            v-model:value="jywgForm.validateKey"
            placeholder="任意查询请求 URL 的 validatekey 参数，或页面 #em_validatekey 的 value"
          />
        </a-form-item>
      </a-form>
      <div class="cookie-help">
        <p>获取方式：① 浏览器打开 jywg.eastmoneysec.com 并登录（滑块/验证码先完成）；② F12 → Network → 任意 jywg 查询请求 → 复制请求头完整 Cookie 和 URL 里的 validatekey 参数；③ 选择在线时长保存，立即生效。</p>
        <p>到期后上游会话同步过期，持仓同步会提示重新获取；倒计时结束前更新即可。</p>
        <p>注意：交易 Cookie 与「东财 Cookie」（行情中心）是两个独立凭证，互不通用，需分别维护。</p>
      </div>
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
        <template v-for="group in navigationGroups" :key="group.key">
          <a-menu-item
            v-if="group.path"
            :key="group.path"
            @click="handleDrawerNavigate(group.path)"
          >
            <span class="mobile-nav-title">
              <component :is="group.icon" />
              <span>{{ group.title }}</span>
            </span>
          </a-menu-item>
          <a-sub-menu v-else :key="group.key">
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
        </template>
      </a-menu>
    </a-drawer>
  </a-layout>
</template>

<script lang="ts" setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  DashboardOutlined,
  StockOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  UserOutlined,
  SafetyCertificateOutlined,
  LogoutOutlined,
  LoginOutlined,
  MailOutlined,
  MenuOutlined,
  FileTextOutlined,
  KeyOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { updateEmail } from '@/api/auth';
import { getEastmoneyCookieStatus, updateEastmoneyCookie, getJywgCookieStatus, updateJywgCookie } from '@/api/eastmoney';
import type { JywgCookieStatus } from '@/api/eastmoney';

type NavigationChild = {
  key: string;
  label: string;
};

type NavigationGroup = {
  key: string;
  title: string;
  icon: any;
  popupClassName?: string;
  children?: NavigationChild[];
  path?: string;
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
    key: '/dashboard',
    title: '大盘全景',
    icon: DashboardOutlined,
    path: '/dashboard'
  },
  {
    key: '/my',
    title: '我的',
    icon: UserOutlined,
    popupClassName: 'top-nav-popup top-nav-popup-compact',
    children: [
      { key: '/watchlist/index', label: '自选' },
      { key: '/portfolio/index', label: '持仓' }
    ]
  },
  {
    key: '/data',
    title: '市场数据',
    icon: StockOutlined,
    popupClassName: 'top-nav-popup top-nav-popup-compact',
    children: [
      { key: '/stock-data/index', label: '股票' },
      { key: '/board/index', label: '行业板块' },
      { key: '/industry-analysis/index', label: '行业涨幅分析' },
      { key: '/fund/index', label: '基金' }
    ]
  },
  {
    key: '/indicators',
    title: '基本面指标',
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
    title: '量化',
    icon: RadarChartOutlined,
    popupClassName: 'top-nav-popup top-nav-popup-compact',
    children: [
      { key: '/strategy/index', label: '策略' }
    ]
  },
  {
    key: '/article',
    title: '投资导航',
    icon: FileTextOutlined,
    popupClassName: 'top-nav-popup top-nav-popup-compact',
    children: [
      { key: '/article/public', label: '广场' },
      { key: '/article/my', label: '我的笔记' },
      { key: '/finance-sites/index', label: '投资书签' }
    ]
  }
];

const currentRouteMeta = computed(() => {
  if (route.path === '/dashboard') {
    return undefined;
  }
  if (route.path === '/watchlist/index' || route.path === '/watchlist' || route.path === '/portfolio/index' || route.path === '/portfolio') {
    return { parent: '', child: '' };
  }
  if (route.path === '/industry-detail/index') {
    return { parent: '市场数据', child: '行业详情' };
  }
  for (const group of navigationGroups) {
    if (group.path && group.path === route.path) {
      return { parent: group.title, child: group.title };
    }
    if (group.children) {
      const child = group.children.find((item) => item.key === route.path);
      if (child) {
        return { parent: group.title, child: child.label };
      }
    }
  }
  return undefined;
});

// 同步菜单状态
const syncMenuState = () => {
  const path = route.path;
  selectedKeys.value = [path];
  const activeGroup = navigationGroups.find((group) =>
    group.path === path || (group.children && group.children.some((child) => child.key === path))
  );
  drawerOpenKeys.value = activeGroup && activeGroup.children ? [activeGroup.key] : [];
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

// 东财 Cookie 相关
const cookieModalVisible = ref(false);
const cookieSaving = ref(false);
const cookieConfigured = ref<boolean | null>(null);
const cookieForm = ref({ cookie: '' });

const showCookieModal = async () => {
  cookieForm.value.cookie = '';
  cookieConfigured.value = null;
  cookieModalVisible.value = true;
  try {
    const res = await getEastmoneyCookieStatus();
    if (res.data.success) {
      cookieConfigured.value = res.data.data === true;
    }
  } catch (error) {
    console.error('Failed to query eastmoney cookie status:', error);
  }
};

const handleUpdateCookie = async () => {
  const cookie = cookieForm.value.cookie.trim();
  if (!cookie) {
    message.warning('请粘贴完整的 Cookie 字符串');
    return;
  }
  cookieSaving.value = true;
  try {
    const res = await updateEastmoneyCookie({ cookie });
    if (res.data.success) {
      message.success('东财 Cookie 更新成功，已即时生效并自动触发同步');
      cookieConfigured.value = true;
      cookieModalVisible.value = false;
    }
  } catch (error) {
    console.error('Failed to update eastmoney cookie:', error);
  } finally {
    cookieSaving.value = false;
  }
};

// 交易 Cookie（jywg 网页交易会话）相关：与行情 Cookie 相互独立，带在线时长倒计时
const jywgModalVisible = ref(false);
const jywgSaving = ref(false);
const jywgStatus = ref<JywgCookieStatus | null>(null);
const jywgForm = ref({ cookie: '', validateKey: '', durationMinutes: 15 });
let jywgCountdownTimer: number | null = null;

const jywgRemainingSeconds = ref(0);

const jywgStatusMessage = computed(() => {
  const status = jywgStatus.value;
  if (!status) return '';
  if (!status.configured) return '当前未配置交易 Cookie';
  if (status.expired || jywgRemainingSeconds.value <= 0) {
    return `交易会话已过期，请重新获取 Cookie${jywgUpdateTimeText.value ? `（上次更新于 ${jywgUpdateTimeText.value}）` : ''}`;
  }
  return `交易会话有效，剩余 ${jywgRemainingDisplay.value}${jywgUpdateTimeText.value ? `（更新于 ${jywgUpdateTimeText.value}）` : ''}`;
});

const jywgUpdateTimeText = computed(() => {
  const updateTime = jywgStatus.value?.updateTime;
  if (!updateTime) return '';
  const parsed = new Date(updateTime);
  return Number.isNaN(parsed.getTime()) ? '' : parsed.toLocaleString('zh-CN', { hour12: false });
});

const jywgStatusType = computed(() => {
  const status = jywgStatus.value;
  if (!status) return 'info';
  if (!status.configured) return 'warning';
  return status.expired || jywgRemainingSeconds.value <= 0 ? 'error' : 'success';
});

const jywgRemainingDisplay = computed(() => {
  const total = jywgRemainingSeconds.value;
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  return minutes > 0 ? `${minutes} 分 ${seconds.toString().padStart(2, '0')} 秒` : `${seconds} 秒`;
});

const stopJywgCountdown = () => {
  if (jywgCountdownTimer !== null) {
    window.clearInterval(jywgCountdownTimer);
    jywgCountdownTimer = null;
  }
};

const startJywgCountdown = () => {
  stopJywgCountdown();
  jywgCountdownTimer = window.setInterval(() => {
    if (jywgRemainingSeconds.value > 0) {
      jywgRemainingSeconds.value -= 1;
    }
  }, 1000);
};

const applyJywgStatus = (status: JywgCookieStatus) => {
  jywgStatus.value = status;
  jywgRemainingSeconds.value = status.configured && !status.expired ? status.remainingSeconds : 0;
};

const showJywgCookieModal = async () => {
  jywgForm.value = { cookie: '', validateKey: '', durationMinutes: 15 };
  jywgStatus.value = null;
  jywgModalVisible.value = true;
  try {
    const res = await getJywgCookieStatus();
    if (res.data.success) {
      applyJywgStatus(res.data.data);
      startJywgCountdown();
    }
  } catch (error) {
    console.error('Failed to query jywg cookie status:', error);
  }
};

const handleUpdateJywgCookie = async () => {
  const cookie = jywgForm.value.cookie.trim();
  const validateKey = jywgForm.value.validateKey.trim();
  if (!cookie || !validateKey) {
    message.warning('请填写完整的 Cookie 与 validatekey');
    return;
  }
  jywgSaving.value = true;
  try {
    const res = await updateJywgCookie({
      cookie,
      validateKey,
      durationMinutes: jywgForm.value.durationMinutes
    });
    if (res.data.success) {
      applyJywgStatus(res.data.data);
      stopJywgCountdown();
      jywgModalVisible.value = false;
      message.success(`交易 Cookie 已生效（在线 ${jywgForm.value.durationMinutes} 分钟），正在自动同步持仓/成交`);
      // 通知持仓页在服务端异步同步完成后刷新数据
      window.dispatchEvent(new CustomEvent('aquant:broker-sync-started'));
    }
  } catch (error) {
    console.error('Failed to update jywg cookie:', error);
  } finally {
    jywgSaving.value = false;
  }
};

onUnmounted(() => {
  stopJywgCountdown();
});
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
  background: transparent;
  flex: 1;
  justify-content: flex-end;
  font-size: 14px;
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
  align-items: center;
  justify-content: center;
  width: 36px !important;
  min-width: 36px;
  height: 36px !important;
  margin-right: 12px;
  padding: 0 !important;
  line-height: 1 !important;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
}

.mobile-nav-trigger:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-surface-hover) !important;
}

.mobile-nav-trigger :deep(.ant-btn-icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-inline-end: 0 !important;
}

.mobile-nav-trigger :deep(.anticon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
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
  min-height: auto;
}

.page-context {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 4px 0 2px;
  border-radius: var(--radius-lg);
  background: transparent;
  border: none;
}

.page-context-left {
  display: flex;
  align-items: center;
  gap: 16px;
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
  margin-inline: 8px;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.page-breadcrumb-parent {
  color: var(--color-text-secondary);
  font-size: 15px;
  font-weight: var(--font-weight-medium);
  letter-spacing: -0.1px;
}

.page-breadcrumb-current {
  color: var(--color-text-primary);
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  letter-spacing: -0.15px;
}

@media (max-width: 768px) {
  .page-breadcrumb-parent {
    font-size: 14px;
  }

  .page-breadcrumb-current {
    font-size: 15px;
  }

  .page-breadcrumb :deep(.ant-breadcrumb-separator) {
    margin-inline: 6px;
    font-size: 13px;
  }
}

.c-footer {
  text-align: center;
  color: var(--color-text-tertiary);
  background: transparent;
  padding: 24px 0;
  font-size: var(--font-size-xs);
}

.cookie-help {
  margin-top: 4px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
}

.cookie-help p {
  margin: 0 0 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.cookie-help p:last-child {
  margin-bottom: 0;
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
