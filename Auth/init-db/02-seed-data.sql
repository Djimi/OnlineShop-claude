-- Seed data for development and testing

-- Insert initial test user
-- Username: testuser
-- Password: testpass
-- Argon2id hash (OWASP 2025 recommended): $argon2id$v=19$m=47104,t=1,p=1$m8bxmEb2JmcMcYcThrTb+g$lkF3yBcV6NRa6fU8LStOzYm7JM+oI/jqO96KV8ggHok
INSERT INTO users (username, normalized_username, password_hash)
VALUES ('testuser', 'testuser', '$argon2id$v=19$m=47104,t=1,p=1$enqtjCbSdr0S+U9Wdt59sA$xVrlmvOWjdRTtrTaMX9kZ1WBbKIp9VOiklDcVq3GdW4')
ON CONFLICT (username) DO NOTHING;
