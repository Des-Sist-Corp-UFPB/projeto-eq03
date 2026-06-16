import axios, { AxiosError } from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  _isRefreshRequest?: boolean;
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
type FailedPromise = {
  resolve: (value?: string | null) => void;
  reject: (reason?: Error | AxiosError) => void;
};

let failedQueue: FailedPromise[] = [];

const processQueue = (error: Error | AxiosError | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.request.use(
  (config: CustomAxiosRequestConfig) => {
    if (config._isRefreshRequest || config.url?.includes('/auth/')) {
      return config;
    }

    const token = localStorage.getItem('@Salon:token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as CustomAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise(function (resolve, reject) {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            return api(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('@Salon:refreshToken');

      if (!refreshToken) {
        isRefreshing = false;
        localStorage.removeItem('@Salon:token');
        localStorage.removeItem('@Salon:refreshToken');
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const { data } = await api.post(
          '/auth/refresh',
          {
            refreshToken,
          },
          {
            _isRefreshRequest: true,
          } as CustomAxiosRequestConfig
        );

        localStorage.setItem('@Salon:token', data.accessToken);
        localStorage.setItem('@Salon:refreshToken', data.refreshToken);

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        }

        processQueue(null, data.accessToken);
        return api(originalRequest);
      } catch (err) {
        processQueue(err as Error | AxiosError, null);
        localStorage.removeItem('@Salon:token');
        localStorage.removeItem('@Salon:refreshToken');
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    if (error.response?.status === 403) {
      localStorage.removeItem('@Salon:token');
      localStorage.removeItem('@Salon:refreshToken');
      window.location.href = '/login';
      return Promise.reject(error);
    }

    return Promise.reject(error);
  }
);

export default api;
