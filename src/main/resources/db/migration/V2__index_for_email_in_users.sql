CREATE index idx_users_email ON users(email);
CREATE index idx_orders_userid ON orders(user_id,created_at DESC)