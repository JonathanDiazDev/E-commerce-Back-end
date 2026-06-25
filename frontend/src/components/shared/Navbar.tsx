import { Link, useNavigate } from 'react-router-dom'
import { ShoppingCart, Package, LogOut, User } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useAuthStore } from '@/store/authStore'
import { useCartStore } from '@/store/cartStore'

export function Navbar() {
  const { user, logout } = useAuthStore()
  const itemCount = useCartStore((s) => s.itemCount)
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  if (!user) return null

  return (
    <nav className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="mx-auto flex h-14 max-w-6xl items-center px-4">
        <Link to="/products" className="flex items-center gap-2 font-bold text-lg">
          <Package className="h-5 w-5" />
          Store
        </Link>

        <div className="flex items-center gap-3 ml-auto">
          <span className="text-sm text-muted-foreground hidden sm:flex items-center gap-1">
            <User className="h-4 w-4" />
            {user.name}
          </span>

          <Link to="/cart">
            <Button variant="ghost" size="icon" className="relative">
              <ShoppingCart className="h-5 w-5" />
              {itemCount > 0 && (
                <Badge className="absolute -top-2 -right-2 h-5 w-5 flex items-center justify-center p-0 text-xs">
                  {itemCount}
                </Badge>
              )}
            </Button>
          </Link>

          <Link to="/orders">
            <Button variant="ghost" size="sm">
              Orders
            </Button>
          </Link>

          <Button variant="ghost" size="sm" onClick={handleLogout}>
            <LogOut className="h-4 w-4 mr-1" />
            Logout
          </Button>
        </div>
      </div>
    </nav>
  )
}
