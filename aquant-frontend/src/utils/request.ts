import axios from 'axios';
import { message } from 'ant-design-vue';

// Create axios instance
const service = axios.create({
    baseURL: '/api',
    timeout: 10000,
    paramsSerializer: {
        indexes: null
    }
});

// Request interceptor
service.interceptors.request.use(
    (config) => {
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
                message.error(data.message || '系统异常');
                // Optional: Reject promise handling if needed, but for now we just show error
                // return Promise.reject(new Error(data.message || 'Error'));
            }
        }
        return response;
    },
    (error) => {
        console.error('Response Error:', error);
        message.error(error.message || '网络连接失败');
        return Promise.reject(error);
    }
);

export default service;
