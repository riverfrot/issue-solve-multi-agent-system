import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import store from '@/store';

// Add request interceptor for loading states
axios.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    // Don't show loading for health checks
    if (config.url && !config.url.includes('/health')) {
      store.commit('updateIsLoading', true);
    }
    return config;
  },
  (error: AxiosError) => {
    store.commit('updateIsLoading', false);
    return Promise.reject(error);
  },
);

// Add response interceptor
axios.interceptors.response.use(
  (response: AxiosResponse) => {
    store.commit('updateIsLoading', false);
    return response;
  },
  (error: AxiosError) => {
    store.commit('updateIsLoading', false);
    
    // Handle common errors
    if (error.response) {
      switch (error.response.status) {
        case 401:
          console.warn('Unauthorized access');
          break;
        case 403:
          console.warn('Forbidden access');
          break;
        case 404:
          console.warn('Resource not found');
          break;
        case 500:
          console.error('Server error');
          break;
        default:
          console.error('Request failed:', error.response.status);
      }
    } else if (error.request) {
      console.error('Network error:', error.message);
    } else {
      console.error('Request setup error:', error.message);
    }
    
    return Promise.reject(error);
  },
);