-- Seed data for development and testing

-- Insert initial test user
-- Username: testuser
-- Password: testpass
-- BCrypt hash (cost factor 10): $2a$10$LU2Hu7aKH9lUPmGh7d3W0uPmKMOKqy4cAH1PV7UoqVzaI0mLQltJK
INSERT INTO users (username, password_hash)
VALUES ('testuser', '$2a$15$F62WeSVx9CaeuMylgN52P.r0/o0awMFKt0x7V1Cu/AEFDz0Wxl/lO')
ON CONFLICT (username) DO NOTHING;
