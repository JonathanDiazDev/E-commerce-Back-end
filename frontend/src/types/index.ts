export interface UserResponse {
  id: number
  email: string
  name: string
  role: string
  createdAt: string
}

export interface AuthUserResponse {
  email: string
  name: string
  role: string
}

export interface ProductResponse {
  id: number
  name: string
  description: string
  price: number
  categoryName: string
  status: string
}

export interface CartItemResponse {
  id: number
  productName: string
  quantity: number
  unitPrice: number
  subTotal: number
}

export interface CartResponse {
  id: number
  items: CartItemResponse[]
  totalAmount: number
}

export interface OrderItemResponse {
  productId: number
  productName: string
  quantity: number
  priceAtPurchase: number
}

export interface OrderResponse {
  id: number
  createdAt: string
  status: string
  totalAmount: number
  items: OrderItemResponse[]
}

export interface CategoryResponse {
  id: number
  name: string
  active: boolean
}

export interface AuthRequest {
  email: string
  password: string
}

export interface UserRequest {
  name: string
  email: string
  password: string
}

export interface AddToCartRequest {
  productId: number
  quantity: number
}

export interface UpdateQuantityRequest {
  quantity: number
}

export interface OrderRequest {
  cartId: number
  paymentMethodId: string
}
