import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Package } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { api } from '@/lib/api'
import { formatCurrency, formatDate } from '@/lib/utils'
import type { OrderResponse } from '@/types'

const statusColors: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  PAID: 'default',
  PENDING: 'secondary',
  PENDING_PAYMENT: 'secondary',
  SHIPPED: 'default',
  DELIVERED: 'default',
  CANCELLED: 'destructive',
  REFUNDED: 'outline',
  FAILED: 'destructive',
}

export default function Orders() {
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get<OrderResponse[]>('/orders')
      .then(setOrders)
      .catch(() => setOrders([]))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingSpinner className="min-h-screen" />

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">My Orders</h1>

      {orders.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-4 py-12">
            <Package className="h-12 w-12 text-muted-foreground" />
            <p className="text-muted-foreground">No orders yet</p>
            <Link to="/products">
              <Badge className="cursor-pointer" variant="secondary">Start Shopping</Badge>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => (
            <Link key={order.id} to={`/orders/${order.id}`}>
              <Card className="hover:border-primary/50 transition-colors cursor-pointer">
                <CardHeader className="pb-3">
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-base">Order #{order.id}</CardTitle>
                    <Badge variant={statusColors[order.status] || 'secondary'}>
                      {order.status}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">{formatDate(order.createdAt)}</span>
                    <span className="font-semibold">{formatCurrency(order.totalAmount)}</span>
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">
                    {order.items.length} item{order.items.length !== 1 && 's'}
                  </p>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
