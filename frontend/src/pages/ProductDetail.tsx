import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, ShoppingCart } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { api } from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
import { useCartStore } from '@/store/cartStore'
import { toast } from 'sonner'
import type { ProductResponse } from '@/types'

export default function ProductDetail() {
  const { id } = useParams<{ id: string }>()
  const [product, setProduct] = useState<ProductResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [quantity, setQuantity] = useState(1)
  const [adding, setAdding] = useState(false)
  const addItem = useCartStore((s) => s.addItem)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    api.get<ProductResponse>(`/products/${id}`)
      .then(setProduct)
      .catch(() => setProduct(null))
      .finally(() => setLoading(false))
  }, [id])

  const handleAddToCart = async () => {
    if (!product) return
    setAdding(true)
    try {
      await addItem(product.id, quantity)
      toast.success(`${product.name} added to cart`)
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to add item'
      toast.error(message)
    } finally {
      setAdding(false)
    }
  }

  if (loading) return <LoadingSpinner className="min-h-screen" />
  if (!product) return <p className="text-center py-12">Product not found.</p>

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Link to="/products" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-6">
        <ArrowLeft className="h-4 w-4" />
        Back to products
      </Link>

      <Card>
        <CardHeader>
          <div className="flex items-start justify-between">
            <div>
              <CardTitle className="text-2xl">{product.name}</CardTitle>
              <p className="text-sm text-muted-foreground mt-1">{product.categoryName}</p>
            </div>
            <Badge variant={product.status === 'ACTIVE' ? 'default' : 'secondary'}>
              {product.status}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          <p className="text-muted-foreground">{product.description}</p>
          <p className="text-3xl font-bold">{formatCurrency(product.price)}</p>

          <div className="flex items-end gap-4 pt-4 border-t">
            <div className="w-24 space-y-2">
              <Label htmlFor="quantity">Quantity</Label>
              <Input
                id="quantity"
                type="number"
                min={1}
                value={quantity}
                onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
              />
            </div>
            <Button onClick={handleAddToCart} disabled={adding} className="gap-2">
              <ShoppingCart className="h-4 w-4" />
              {adding ? 'Adding...' : 'Add to Cart'}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
