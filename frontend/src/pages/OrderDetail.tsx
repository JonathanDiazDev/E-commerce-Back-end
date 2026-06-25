import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, Package } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
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

export default function OrderDetail() {
  const { id } = useParams<{ id: string }>()
  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    api.get<OrderResponse>(`/orders/${id}`)
      .then(setOrder)
      .catch(() => setOrder(null))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <LoadingSpinner className="min-h-screen" />
  if (!order) return <p className="text-center py-12">Order not found.</p>

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Link to="/orders" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-6">
        <ArrowLeft className="h-4 w-4" />
        Back to orders
      </Link>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-2xl">Order #{order.id}</CardTitle>
              <p className="text-sm text-muted-foreground mt-1">{formatDate(order.createdAt)}</p>
            </div>
            <Badge variant={statusColors[order.status] || 'secondary'} className="text-sm px-3 py-1">
              {order.status}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          {order.items.length === 0 ? (
            <div className="flex flex-col items-center gap-3 py-8">
              <Package className="h-10 w-10 text-muted-foreground" />
              <p className="text-muted-foreground">No items in this order</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Product</TableHead>
                  <TableHead className="text-right">Price</TableHead>
                  <TableHead className="text-center">Qty</TableHead>
                  <TableHead className="text-right">Subtotal</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {order.items.map((item) => (
                  <TableRow key={item.productId}>
                    <TableCell className="font-medium">{item.productName}</TableCell>
                    <TableCell className="text-right">{formatCurrency(item.priceAtPurchase)}</TableCell>
                    <TableCell className="text-center">{item.quantity}</TableCell>
                    <TableCell className="text-right">{formatCurrency(item.priceAtPurchase * item.quantity)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          <div className="flex justify-between items-center border-t pt-4">
            <span className="text-lg font-bold">Total</span>
            <span className="text-lg font-bold">{formatCurrency(order.totalAmount)}</span>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
