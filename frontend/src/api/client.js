import axios from 'axios';

// In dev, '/api' is proxied to :8080 by Vite; in production nginx proxies it to
// the backend. VITE_API_URL can override for other setups.
const baseURL = import.meta.env.VITE_API_URL || '/api';

export const TOKEN_KEY = 'esic_token';
export const USER_KEY = 'esic_user';

const client = axios.create({ baseURL });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      const path = window.location.pathname;
      if (!['/login', '/', '/forgot', '/reset'].includes(path)) {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  }
);

/** Extracts a human-readable message from an axios error. */
export function errMsg(error, fallback = 'Something went wrong') {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback
  );
}

export default client;
