import axios, { AxiosInstance, AxiosError } from 'axios';
import { toast } from 'react-toastify';

const api: AxiosInstance = axios.create({
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor
api.interceptors.request.use(
    (config) => {
        // You can add auth tokens here if needed
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor
api.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
        const message = (error.response?.data as any)?.message || error.message || 'An unexpected error occurred';

        if (error.response?.status === 429) {
            toast.error('Too many requests. Please wait a moment.', { position: 'top-center' });
        } else {
            toast.error(message);
        }

        return Promise.reject(error);
    }
);

export default api;
