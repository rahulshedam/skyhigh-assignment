import axios, { AxiosInstance, AxiosError } from 'axios';
import { toast } from 'react-toastify';

const EMAIL_KEY = 'skyhigh_user_email';

const api: AxiosInstance = axios.create({
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor — attach X-User-Email header if logged in
api.interceptors.request.use(
    (config) => {
        const email = localStorage.getItem(EMAIL_KEY);
        if (email) {
            config.headers['X-User-Email'] = email;
        }
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
