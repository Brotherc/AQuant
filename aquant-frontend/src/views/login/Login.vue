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
            <div class="form-extra">
              <a-button type="link" class="forget-btn" @click="openResetModal">
                忘记密码
              </a-button>
            </div>
          </a-form>
        </div>
      </div>
    </div>

    <a-modal
      v-model:open="resetModalVisible"
      title="邮箱找回密码"
      :confirm-loading="resetLoading"
      @ok="handleResetPassword"
      @cancel="handleResetModalCancel"
      destroyOnClose
    >
      <a-form layout="vertical">
        <a-form-item label="邮箱" required>
          <a-input
            v-model:value="resetForm.email"
            placeholder="请输入已绑定的邮箱"
            size="large"
          />
        </a-form-item>
        <a-form-item label="验证码" required>
          <div class="verify-row">
            <a-input
              v-model:value="resetForm.code"
              placeholder="请输入邮箱验证码"
              size="large"
            />
            <a-button
              size="large"
              :loading="sendCodeLoading"
              :disabled="countdown > 0"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s后重发` : '发送验证码' }}
            </a-button>
          </div>
        </a-form-item>
        <a-form-item label="新密码" required>
          <a-input-password
            v-model:value="resetForm.newPassword"
            placeholder="请输入新密码"
            size="large"
          />
        </a-form-item>
        <a-form-item label="确认新密码" required>
          <a-input-password
            v-model:value="resetForm.confirmPassword"
            placeholder="请再次输入新密码"
            size="large"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, h } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { UserOutlined, LockOutlined, LineChartOutlined, StockOutlined, BellOutlined, ExperimentOutlined } from '@ant-design/icons-vue';
import { login, resetPassword, sendResetCode } from '@/api/auth';

const router = useRouter();
const loading = ref(false);
const loginForm = ref({ username: '', password: '' });
const resetModalVisible = ref(false);
const sendCodeLoading = ref(false);
const resetLoading = ref(false);
const countdown = ref(0);
const resetForm = ref({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
});
let countdownTimer: number | null = null;

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

const openResetModal = () => {
  resetForm.value = {
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: '',
  };
  countdown.value = 0;
  clearCountdown();
  resetModalVisible.value = true;
};

const handleResetModalCancel = () => {
  resetModalVisible.value = false;
  clearCountdown();
};

const clearCountdown = () => {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer);
    countdownTimer = null;
  }
};

const startCountdown = () => {
  countdown.value = 60;
  clearCountdown();
  countdownTimer = window.setInterval(() => {
    if (countdown.value <= 1) {
      countdown.value = 0;
      clearCountdown();
      return;
    }
    countdown.value -= 1;
  }, 1000);
};

const validateEmail = (email: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

const handleSendCode = async () => {
  resetForm.value.email = resetForm.value.email.trim();
  if (!resetForm.value.email) {
    message.warning('请输入邮箱地址');
    return;
  }
  if (!validateEmail(resetForm.value.email)) {
    message.warning('请输入正确的邮箱格式');
    return;
  }

  sendCodeLoading.value = true;
  try {
    const res = await sendResetCode({ email: resetForm.value.email });
    if (res.data.success) {
      message.success('验证码已发送，请查收邮箱');
      startCountdown();
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    sendCodeLoading.value = false;
  }
};

const handleResetPassword = async () => {
  resetForm.value.email = resetForm.value.email.trim();
  resetForm.value.code = resetForm.value.code.trim();
  if (!resetForm.value.email || !validateEmail(resetForm.value.email)) {
    message.warning('请输入正确的邮箱地址');
    return;
  }
  if (!resetForm.value.code) {
    message.warning('请输入验证码');
    return;
  }
  if (!resetForm.value.newPassword || resetForm.value.newPassword.length < 6) {
    message.warning('新密码至少6位');
    return;
  }
  if (resetForm.value.newPassword !== resetForm.value.confirmPassword) {
    message.warning('两次输入的新密码不一致');
    return;
  }

  resetLoading.value = true;
  try {
    const res = await resetPassword({
      email: resetForm.value.email,
      code: resetForm.value.code,
      newPassword: resetForm.value.newPassword,
    });
    if (res.data.success) {
      message.success('密码重置成功，请使用新密码登录');
      resetModalVisible.value = false;
      clearCountdown();
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    resetLoading.value = false;
  }
};

onBeforeUnmount(() => {
  clearCountdown();
});
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 50%, #0f0f0f 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.03) 0%, transparent 70%);
  animation: rotate 30s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.login-container {
  display: flex;
  width: 900px;
  min-height: 480px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.8), 0 0 60px rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 左侧品牌 */
.brand-section {
  flex: 1;
  background: linear-gradient(160deg, #0f0f0f 0%, #1a1a1a 50%, #0a0a0a 100%);
  padding: 48px 40px;
  display: flex;
  align-items: center;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.brand-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 30% 50%, rgba(255, 255, 255, 0.05) 0%, transparent 50%);
  pointer-events: none;
}

.brand-content {
  width: 100%;
  position: relative;
  z-index: 1;
}

.brand-logo {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 12px;
  color: #fff;
  text-shadow: 0 0 20px rgba(255, 255, 255, 0.3);
}

.brand-logo-icon {
  color: #fff;
  margin-right: 8px;
  filter: drop-shadow(0 0 8px rgba(255, 255, 255, 0.3));
}

.brand-slogan {
  font-size: 18px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 20px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
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
  color: rgba(255, 255, 255, 0.85);
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateX(4px);
}

.feature-icon {
  font-size: 20px;
  color: #fff;
  filter: drop-shadow(0 0 4px rgba(255, 255, 255, 0.3));
}

/* 右侧表单 */
.form-section {
  flex: 1;
  background: rgba(20, 20, 20, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}

.form-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 70% 50%, rgba(255, 255, 255, 0.03) 0%, transparent 50%);
  pointer-events: none;
}

.form-wrapper {
  width: 100%;
  max-width: 340px;
  position: relative;
  z-index: 1;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 32px;
  text-align: center;
  text-shadow: 0 0 20px rgba(255, 255, 255, 0.2);
}

.auth-form {
  margin-top: 8px;
}

.form-extra {
  margin-top: -6px;
  text-align: right;
}

.forget-btn {
  padding-right: 0;
  color: rgba(255, 255, 255, 0.5);
}

.forget-btn:hover {
  color: rgba(255, 255, 255, 0.9);
}

.submit-btn {
  margin-top: 8px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #2a2a2a 0%, #1a1a1a 100%);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5);
  transition: all 0.3s ease;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #3a3a3a 0%, #2a2a2a 100%);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.7);
  transform: translateY(-2px);
}

.verify-row {
  display: grid;
  grid-template-columns: 1fr 128px;
  gap: 12px;
}

/* 深色主题输入框样式 */
:deep(.ant-input),
:deep(.ant-input-password),
:deep(.ant-input-affix-wrapper) {
  background: rgba(30, 30, 30, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  color: #fff;
  transition: all 0.3s ease;
}

:deep(.ant-input::placeholder),
:deep(.ant-input-password::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

:deep(.ant-input:hover),
:deep(.ant-input-password:hover),
:deep(.ant-input-affix-wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.25);
  background: rgba(30, 30, 30, 0.8);
}

:deep(.ant-input:focus),
:deep(.ant-input-password:focus),
:deep(.ant-input-affix-wrapper-focused) {
  border-color: rgba(255, 255, 255, 0.4);
  background: rgba(30, 30, 30, 0.9);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.1);
}

:deep(.ant-input-prefix) {
  color: rgba(255, 255, 255, 0.5);
}

:deep(.ant-input-suffix) {
  color: rgba(255, 255, 255, 0.5);
}

/* Modal 深色样式 */
:deep(.ant-modal-content) {
  background: rgba(20, 20, 20, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.8);
}

:deep(.ant-modal-header) {
  background: transparent;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.ant-modal-title) {
  color: #fff;
}

:deep(.ant-modal-close) {
  color: rgba(255, 255, 255, 0.5);
}

:deep(.ant-modal-close:hover) {
  color: #fff;
}

:deep(.ant-form-item-label > label) {
  color: rgba(255, 255, 255, 0.85);
}

:deep(.ant-btn-default) {
  background: rgba(30, 30, 30, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #fff;
}

:deep(.ant-btn-default:hover) {
  background: rgba(40, 40, 40, 0.8);
  border-color: rgba(255, 255, 255, 0.25);
  color: #fff;
}

:deep(.ant-btn-default:disabled) {
  background: rgba(30, 30, 30, 0.3);
  border-color: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.3);
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

  .verify-row {
    grid-template-columns: 1fr;
  }
}
</style>
