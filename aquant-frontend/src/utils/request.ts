import axios from 'axios';
import { message } from 'ant-design-vue';

// Create axios instance
const service = axios.create({
    baseURL: '/api',
    timeout: 20000,
    paramsSerializer: {
        indexes: null
    }
});

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
                    message.error(data.message || '登录状态已失效，请重新登录');
                    localStorage.removeItem('token');
                    localStorage.removeItem('nickname');
                    setTimeout(() => {
                        window.location.reload();
                    }, 1500);
                    return Promise.reject(new Error(data.message || '登录已失效'));
                }
                message.error(data.message || '系统异常');
            }
        }
        return response;
    },
    (error) => {
        if (error.response?.status === 401) {
            // Token 无效 → 静默清除，不打扰用户
            localStorage.removeItem('token');
            localStorage.removeItem('nickname');
            return Promise.reject(error);
        }
        console.error('Response Error:', error);
        message.error(error.message || '网络连接失败');
        return Promise.reject(error);
    }
);

export default service;
