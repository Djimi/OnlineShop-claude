import api from './api';
import type { RegisterRequest, RegisterResponse, LoginRequest, LoginResponse, ValidateResponse } from '../types/api';

export const authService = {
  register: async (data: RegisterRequest): Promise<RegisterResponse> => {
    const response = await api.post<RegisterResponse>('/auth/register', data);
    return response.data;
  },

  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/login', data);
    return response.data;
  },

  validate: async (): Promise<ValidateResponse> => {
    const response = await api.get<ValidateResponse>('/auth/validate');
    return response.data;
  },
};
