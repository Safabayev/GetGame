CREATE TABLE IF NOT EXISTS games(
  id UUID PRIMARY KEY,
  title VARCHAR NOT NULL UNIQUE,
  genre VARCHAR NOT NULL,
  platform VARCHAR NOT NULL,
  developer VARCHAR NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
