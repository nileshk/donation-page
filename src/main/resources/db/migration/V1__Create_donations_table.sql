--Version 1

-- Donations table
CREATE TABLE donations (
  id            INTEGER PRIMARY KEY,
  payment_date  DATE,
  amount_cents  INTEGER,
  amount_string TEXT,
  name          TEXT,
  address1      TEXT,
  address2      TEXT,
  city          TEXT,
  state         TEXT,
  zip           TEXT,
  country       TEXT,
  email         TEXT,
  occupation    TEXT,
  purpose       TEXT
);
