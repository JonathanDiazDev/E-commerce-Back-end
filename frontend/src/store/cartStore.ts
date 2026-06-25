import { create } from 'zustand'
import type { CartResponse } from '@/types'
import { api } from '@/lib/api'

interface CartState {
  cart: CartResponse | null
  loading: boolean
  itemCount: number
  fetchCart: () => Promise<void>
  addItem: (productId: number, quantity: number) => Promise<void>
  updateQuantity: (cartItemId: number, quantity: number) => Promise<void>
  removeItem: (cartItemId: number) => Promise<void>
  clearCart: () => void
}

export const useCartStore = create<CartState>((set, get) => ({
  cart: null,
  loading: false,
  itemCount: 0,

  fetchCart: async () => {
    set({ loading: true })
    try {
      const cart = await api.get<CartResponse>('/carts')
      const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0)
      set({ cart, itemCount, loading: false })
    } catch {
      set({ cart: null, itemCount: 0, loading: false })
    }
  },

  addItem: async (productId, quantity) => {
    const cart = await api.post<CartResponse>('/carts/item', { productId, quantity })
    const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0)
    set({ cart, itemCount })
  },

  updateQuantity: async (cartItemId, quantity) => {
    const cart = await api.put<CartResponse>(`/carts/item/${cartItemId}`, { quantity })
    const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0)
    set({ cart, itemCount })
  },

  removeItem: async (cartItemId) => {
    const cart = await api.delete<CartResponse>(`/carts/item/${cartItemId}`)
    const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0)
    set({ cart, itemCount })
  },

  clearCart: () => set({ cart: null, itemCount: 0 }),
}))
