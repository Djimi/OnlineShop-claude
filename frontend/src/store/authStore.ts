import { create } from 'zustand';
import type { AuthState } from '../types/api';

const STORAGE_KEY = 'onlineshop_auth';

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  userId: null,
  username: null,
  isAuthenticated: false,

  setAuth: (token: string, userId: number, username: string) => {
    const authData = { token, userId, username };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(authData));
    set({
      token,
      userId,
      username,
      isAuthenticated: true,
    });
  },

  logout: () => {
    localStorage.removeItem(STORAGE_KEY);
    set({
      token: null,
      userId: null,
      username: null,
      isAuthenticated: false,
    });
  },

  loadFromStorage: () => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const authData = JSON.parse(stored);
        set({
          token: authData.token,
          userId: authData.userId,
          username: authData.username,
          isAuthenticated: true,
        });
      } catch (error) {
        console.error('Failed to load auth from storage:', error);
      }
    }
  },
}));
