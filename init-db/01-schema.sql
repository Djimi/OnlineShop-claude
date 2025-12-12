-- Create items table if it doesn't exist
CREATE TABLE IF NOT EXISTS items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    description VARCHAR(500)
);

-- Create index on name for faster lookups
CREATE INDEX IF NOT EXISTS idx_items_name ON items(name);
