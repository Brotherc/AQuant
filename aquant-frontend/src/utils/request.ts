import axios from 'axios';
import { message } from 'ant-design-vue';
import router from '@/router';

// Create axios instance
const service = axios.create({
    baseURL: '/api',
    timeout: 20000,
    paramsSerializer: {
        indexes: null
    }
});

let authRedirecting = false;

const handleAuthFailure = (messageText: string) => {
    localStorage.removeItem('token');
    localStorage.removeItem('nickname');

    if (!authRedirecting) {
        authRedirecting = true;
        message.error(messageText || '登录状态已失效，请重新登录');

        const currentPath = router.currentRoute.value.fullPath;
        const redirectQuery = currentPath && currentPath !== '/login'
            ? { redirect: currentPath }
            : undefined;

        router.replace({
            path: '/login',
            query: redirectQuery
        }).finally(() => {
            authRedirecting = false;
        });
    }

    const authError = new Error(messageText || '登录已失效') as Error & { isAuthFailure?: boolean };
    authError.isAuthFailure = true;
    return Promise.reject(authError);
};

// Request interceptor - 自动附加 JWT Token
service.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        console.error('Request Error:', error);
        return Promise.reject(error);
    }
);

// Response interceptor
service.interceptors.response.use(
    (response) => {
        const data = response.data;
        // Check for success field in ResponseDTO
        if (data && typeof data === 'object' && 'success' in data) {
            if (!data.success && data.code !== 0 && data.code !== 200) {
                if (data.code === 1000206 || data.code === 1000204) {
                    return handleAuthFailure(data.message || '登录状态已失效，请重新登录');
                }
                message.error(data.message || '系统异常');
            }
        }
        return response;
    },
    (error) => {
        if (error.response?.status === 401) {
            return handleAuthFailure('登录状态已失效，请重新登录');
        }
        console.error('Response Error:', error);
        message.error(error.message || '网络连接失败');
        return Promise.reject(error);
    }
);

export default service;
