import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { api } from '@/lib/api'
import { formatCurrency } from '@/lib/utils'
import type { ProductResponse } from '@/types'

export default function Products() {
  const [products, setProducts] = useState<ProductResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')

  useEffect(() => {
    setLoading(true)
    const params = new URLSearchParams()
    if (search) params.set('name', search)
    if (category) params.set('category', category)

    const query = params.toString()
    api.get<ProductResponse[]>(`/products${query ? `?${query}` : ''}`)
      .then(setProducts)
      .catch(() => setProducts([]))
      .finally(() => setLoading(false))
  }, [search, category])

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">Products</h1>

      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search by name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
        <Input
          placeholder="Filter by category..."
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="sm:w-60"
        />
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : products.length === 0 ? (
        <p className="text-center text-muted-foreground py-12">No products found.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {products.map((product) => (
            <Card key={product.id}>
              <CardHeader>
                <CardTitle className="text-lg">{product.name}</CardTitle>
                <CardDescription>{product.categoryName}</CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground line-clamp-2">{product.description}</p>
                <p className="text-xl font-bold mt-3">{formatCurrency(product.price)}</p>
              </CardContent>
              <CardFooter className="flex justify-between">
                <Badge variant={product.status === 'ACTIVE' ? 'default' : 'secondary'}>
                  {product.status}
                </Badge>
                <Link to={`/products/${product.id}`}>
                  <Button size="sm">View Details</Button>
                </Link>
              </CardFooter>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
