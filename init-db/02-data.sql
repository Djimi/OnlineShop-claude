-- Insert initial items data
INSERT INTO items (name, quantity, description) VALUES
    ('Laptop', 15, 'High-performance laptop with 16GB RAM and 512GB SSD'),
    ('Mouse', 50, 'Wireless optical mouse with ergonomic design'),
    ('Keyboard', 0, 'Mechanical keyboard with RGB backlighting'),
    ('Monitor', 25, '27-inch 4K UHD monitor with HDR support'),
    ('Headphones', 8, 'Noise-cancelling over-ear headphones')
ON CONFLICT DO NOTHING;
