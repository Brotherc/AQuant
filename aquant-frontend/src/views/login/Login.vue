<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-logo"><line-chart-outlined class="brand-logo-icon" /> AQuant</div>
          <div class="brand-slogan">智能量化分析平台</div>
          <div class="brand-desc">
            专业的股票数据分析、技术指标追踪与量化策略回测平台，助您把握市场脉搏。
          </div>
          <div class="brand-features">
            <div class="feature-item">
              <stock-outlined class="feature-icon" />
              <span>实时行情 & K线分析</span>
            </div>
            <div class="feature-item">
              <bell-outlined class="feature-icon" />
              <span>股票价格智能通知</span>
            </div>
            <div class="feature-item">
              <experiment-outlined class="feature-icon" />
              <span>量化策略回测引擎</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-section">
        <div class="form-wrapper">
          <div class="form-title">账号登录</div>

          <a-form
            :model="loginForm"
            @finish="handleLogin"
            layout="vertical"
            class="auth-form"
          >
            <a-form-item name="username" :rules="[{ required: true, message: '请输入用户名' }]">
              <a-input
                v-model:value="loginForm.username"
                placeholder="用户名"
                size="large"
                :prefix="h(UserOutlined)"
              />
            </a-form-item>
            <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password
                v-model:value="loginForm.password"
                placeholder="密码"
                size="large"
                :prefix="h(LockOutlined)"
              />
            </a-form-item>
            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                block
                :loading="loading"
                class="submit-btn"
              >
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { UserOutlined, LockOutlined, LineChartOutlined, StockOutlined, BellOutlined, ExperimentOutlined } from '@ant-design/icons-vue';
import { login } from '@/api/auth';

const router = useRouter();
const loading = ref(false);
const loginForm = ref({ username: '', password: '' });

const handleLogin = async () => {
  loading.value = true;
  try {
    const res = await login(loginForm.value);
    if (res.data.success) {
      const data = res.data.data;
      localStorage.setItem('token', data.token);
      localStorage.setItem('nickname', data.nickname || data.username);
      message.success('登录成功');
      router.push('/');
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  padding: 20px;
}

.login-container {
  display: flex;
  width: 900px;
  min-height: 480px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* 左侧品牌 */
.brand-section {
  flex: 1;
  background: linear-gradient(160deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 48px 40px;
  display: flex;
  align-items: center;
  color: #fff;
}

.brand-content {
  width: 100%;
}

.brand-logo {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 12px;
}

.brand-slogan {
  font-size: 18px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 20px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.8;
  margin-bottom: 36px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #fff;
}

.feature-icon {
  font-size: 20px;
}

/* 右侧表单 */
.form-section {
  flex: 1;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 340px;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: #000;
  margin-bottom: 32px;
  text-align: center;
}

.auth-form {
  margin-top: 8px;
}

.submit-btn {
  margin-top: 8px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  letter-spacing: 2px;
}

:deep(.ant-input-affix-wrapper) {
  border-radius: 8px;
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    width: 100%;
    max-width: 420px;
  }
  
  .brand-section {
    padding: 32px 24px;
  }
  
  .brand-desc,
  .brand-features {
    display: none;
  }
}
</style>
