--Version 2

-- Payment Log table
CREATE TABLE payment_log (
  id                INTEGER PRIMARY KEY,
  payment_date      DATE,
  json_response     TEXT,
  parameters        TEXT,
  amount_string     TEXT,
  email             TEXT,
  occupation        TEXT,
  purpose           TEXT,
  payment_processor TEXT
);
