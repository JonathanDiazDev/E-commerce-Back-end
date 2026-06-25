import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Trash2, Minus, Plus, ShoppingBag, ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { useCartStore } from '@/store/cartStore'
import { formatCurrency } from '@/lib/utils'
import { api } from '@/lib/api'
import type { OrderResponse } from '@/types'
import { toast } from 'sonner'

export default function Cart() {
  const { cart, loading, fetchCart, updateQuantity, removeItem } = useCartStore()
  const [checkingOut, setCheckingOut] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  const handleCheckout = async () => {
    if (!cart || cart.items.length === 0) return
    setCheckingOut(true)
    try {
      const order = await api.post<OrderResponse>('/orders/checkout', {
        cartId: cart.id,
        paymentMethodId: 'pm_card_visa',
      })
      toast.success('Order placed successfully!')
      useCartStore.getState().clearCart()
      navigate(`/orders/${order.id}`)
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Checkout failed'
      toast.error(message)
    } finally {
      setCheckingOut(false)
    }
  }

  if (loading) return <LoadingSpinner className="min-h-screen" />

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Shopping Cart</h1>
        <Link to="/products" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="h-4 w-4" />
          Continue shopping
        </Link>
      </div>

      {!cart || cart.items.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center gap-4 py-12">
            <ShoppingBag className="h-12 w-12 text-muted-foreground" />
            <p className="text-muted-foreground">Your cart is empty</p>
            <Link to="/products">
              <Button>Browse Products</Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>{cart.items.length} item{cart.items.length !== 1 && 's'}</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Product</TableHead>
                  <TableHead className="text-right">Price</TableHead>
                  <TableHead className="text-center">Quantity</TableHead>
                  <TableHead className="text-right">Subtotal</TableHead>
                  <TableHead className="w-12" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {cart.items.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell className="font-medium">{item.productName}</TableCell>
                    <TableCell className="text-right">{formatCurrency(item.unitPrice)}</TableCell>
                    <TableCell>
                      <div className="flex items-center justify-center gap-2">
                        <Button
                          variant="outline"
                          size="icon"
                          className="h-7 w-7"
                          onClick={() => {
                            if (item.quantity > 1) {
                              updateQuantity(item.id, item.quantity - 1)
                            }
                          }}
                          disabled={item.quantity <= 1}
                        >
                          <Minus className="h-3 w-3" />
                        </Button>
                        <span className="w-8 text-center text-sm">{item.quantity}</span>
                        <Button
                          variant="outline"
                          size="icon"
                          className="h-7 w-7"
                          onClick={() => updateQuantity(item.id, item.quantity + 1)}
                        >
                          <Plus className="h-3 w-3" />
                        </Button>
                      </div>
                    </TableCell>
                    <TableCell className="text-right">{formatCurrency(item.subTotal)}</TableCell>
                    <TableCell>
                      <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive" onClick={() => removeItem(item.id)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
          <CardFooter className="flex justify-between border-t px-6 py-4">
            <span className="text-lg font-bold">Total: {formatCurrency(cart.totalAmount)}</span>
            <Button onClick={handleCheckout} disabled={checkingOut}>
              {checkingOut ? 'Processing...' : 'Checkout'}
            </Button>
          </CardFooter>
        </Card>
      )}
    </div>
  )
}
