import axios from 'axios';
import { message } from 'ant-design-vue';
import router from '@/router';

const aiRequest = axios.create({
  baseURL: '/ai-api',
  timeout: 20000
});

let authRedirecting = false;

const errorMessage = (detail: unknown, fallback: string) => (
  typeof detail === 'string' && detail ? detail : fallback
);

aiRequest.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

aiRequest.interceptors.response.use(
  (response) => response,
  async (error) => {
    const detail = error.response?.data?.detail;
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('nickname');
      if (!authRedirecting) {
        authRedirecting = true;
        message.error(errorMessage(detail, '登录状态已失效，请重新登录'));
        await router.replace({
          path: '/login',
          query: { redirect: router.currentRoute.value.fullPath }
        }).finally(() => {
          authRedirecting = false;
        });
      }
    } else {
      message.error(errorMessage(detail, error.message || 'AI 服务请求失败'));
    }
    return Promise.reject(error);
  }
);

export default aiRequest;
