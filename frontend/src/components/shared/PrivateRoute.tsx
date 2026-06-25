import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { LoadingSpinner } from './LoadingSpinner'

export function PrivateRoute() {
  const { user, loading } = useAuthStore()

  if (loading) return <LoadingSpinner className="min-h-screen" />
  if (!user) return <Navigate to="/login" replace />

  return <Outlet />
}
