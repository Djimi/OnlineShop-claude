-- Insert initial items data
INSERT INTO items (id, name, quantity, description) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Laptop', 15, 'High-performance laptop with 16GB RAM and 512GB SSD'),
    ('550e8400-e29b-41d4-a716-446655440002', 'Mouse', 50, 'Wireless optical mouse with ergonomic design'),
    ('550e8400-e29b-41d4-a716-446655440003', 'Keyboard', 0, 'Mechanical keyboard with RGB backlighting'),
    ('550e8400-e29b-41d4-a716-446655440004', 'Monitor', 25, '27-inch 4K UHD monitor with HDR support'),
    ('550e8400-e29b-41d4-a716-446655440005', 'Headphones', 8, 'Noise-cancelling over-ear headphones')
ON CONFLICT DO NOTHING;
