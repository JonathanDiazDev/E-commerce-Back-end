import { create } from 'zustand'
import type { AuthUserResponse } from '@/types'
import { api } from '@/lib/api'

interface AuthState {
  user: AuthUserResponse | null
  loading: boolean
  setUser: (user: AuthUserResponse | null) => void
  checkSession: () => Promise<void>
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  loading: true,

  setUser: (user) => set({ user }),

  checkSession: async () => {
    try {
      const user = await api.get<AuthUserResponse>('/auth/me')
      set({ user, loading: false })
    } catch {
      set({ user: null, loading: false })
    }
  },

  login: async (email, password) => {
    const user = await api.post<AuthUserResponse>('/auth/login', { email, password })
    set({ user })
  },

  register: async (name, email, password) => {
    await api.post('/auth/register', { name, email, password })
  },

  logout: async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      // ignore
    }
    set({ user: null })
  },
}))
