import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'sonner'
import { Navbar } from '@/components/shared/Navbar'
import { PrivateRoute } from '@/components/shared/PrivateRoute'
import { useAuthStore } from '@/store/authStore'
import { useCartStore } from '@/store/cartStore'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import Products from '@/pages/Products'
import ProductDetail from '@/pages/ProductDetail'
import Cart from '@/pages/Cart'
import Orders from '@/pages/Orders'
import OrderDetail from '@/pages/OrderDetail'

function AppContent() {
  const user = useAuthStore((s) => s.user)

  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/products" replace /> : <Login />} />
        <Route path="/register" element={user ? <Navigate to="/products" replace /> : <Register />} />
        <Route path="/products" element={<Products />} />
        <Route path="/products/:id" element={<ProductDetail />} />
        <Route element={<PrivateRoute />}>
          <Route path="/cart" element={<Cart />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/orders/:id" element={<OrderDetail />} />
        </Route>
        <Route path="*" element={<Navigate to="/products" replace />} />
      </Routes>
      <Toaster richColors position="top-right" />
    </>
  )
}

export default function App() {
  const checkSession = useAuthStore((s) => s.checkSession)
  const fetchCart = useAuthStore((s) => s.user) ? useCartStore.getState().fetchCart : undefined

  useEffect(() => {
    checkSession()
  }, [checkSession])

  useEffect(() => {
    const unsub = useAuthStore.subscribe((state, prev) => {
      if (state.user && !prev.user) {
        useCartStore.getState().fetchCart()
      }
    })
    return unsub
  }, [])

  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  )
}
