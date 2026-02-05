DO $$ BEGIN
  CREATE TYPE user_role AS ENUM ('CUSTOMER', 'OWNER');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE booking_status AS ENUM ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE payment_status AS ENUM ('INIT', 'APPROVED', 'FAILED', 'CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE refund_status AS ENUM ('REQUESTED', 'APPROVED', 'REJECTED', 'DONE', 'FAILED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role user_role NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS camps (
  id BIGSERIAL PRIMARY KEY,
  owner_id BIGINT NOT NULL REFERENCES users(id),
  name VARCHAR(200) NOT NULL,
  address VARCHAR(500),
  phone VARCHAR(50),
  description TEXT,
  check_in_time TIME,
  check_out_time TIME,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_camps_owner ON camps(owner_id);

CREATE TABLE IF NOT EXISTS sites (
  id BIGSERIAL PRIMARY KEY,
  camp_id BIGINT NOT NULL REFERENCES camps(id),
  name VARCHAR(200) NOT NULL,
  base_price NUMERIC(12,2) NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'KRW',
  capacity INT NOT NULL DEFAULT 4,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (camp_id, name)
);

CREATE INDEX IF NOT EXISTS idx_sites_camp ON sites(camp_id);

CREATE TABLE IF NOT EXISTS refund_policy_versions (
  id BIGSERIAL PRIMARY KEY,
  camp_id BIGINT NOT NULL REFERENCES camps(id),
  version INT NOT NULL,
  rule_json JSONB NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (camp_id, version)
);

CREATE INDEX IF NOT EXISTS idx_rpv_camp_active ON refund_policy_versions(camp_id, is_active);

CREATE TABLE IF NOT EXISTS bookings (
  id BIGSERIAL PRIMARY KEY,
  booking_code VARCHAR(30) UNIQUE NOT NULL,
  customer_id BIGINT NOT NULL REFERENCES users(id),
  camp_id BIGINT NOT NULL REFERENCES camps(id),
  site_id BIGINT NOT NULL REFERENCES sites(id),
  head_count INT NOT NULL DEFAULT 1,

  check_in_date DATE NOT NULL,
  check_out_date DATE NOT NULL,
  nights_count INT NOT NULL,
  status booking_status NOT NULL,

  amount_snapshot_subtotal NUMERIC(12,2) NOT NULL,
  amount_snapshot_discount NUMERIC(12,2) NOT NULL DEFAULT 0,
  amount_snapshot_total NUMERIC(12,2) NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'KRW',

  refund_policy_version_id BIGINT REFERENCES refund_policy_versions(id),
  refund_rule_snapshot_json JSONB,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CHECK (check_in_date < check_out_date),
  CHECK (nights_count > 0)
);

CREATE INDEX IF NOT EXISTS idx_bookings_customer ON bookings(customer_id);
CREATE INDEX IF NOT EXISTS idx_bookings_camp_dates ON bookings(camp_id, check_in_date, check_out_date);

CREATE TABLE IF NOT EXISTS booking_nights (
  id BIGSERIAL PRIMARY KEY,
  booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  site_id BIGINT NOT NULL REFERENCES sites(id),
  night_date DATE NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (site_id, night_date)
);

CREATE INDEX IF NOT EXISTS idx_bn_booking ON booking_nights(booking_id);
CREATE INDEX IF NOT EXISTS idx_bn_site_date ON booking_nights(site_id, night_date);

CREATE TABLE IF NOT EXISTS payments (
  id BIGSERIAL PRIMARY KEY,
  booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  status payment_status NOT NULL,
  provider VARCHAR(50),
  provider_tx_id VARCHAR(100),
  amount NUMERIC(12,2) NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'KRW',
  approved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (provider, provider_tx_id)
);

CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments(booking_id);

CREATE TABLE IF NOT EXISTS refunds (
  id BIGSERIAL PRIMARY KEY,
  booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  idempotency_key VARCHAR(80) NOT NULL,
  status refund_status NOT NULL,
  requested_amount NUMERIC(12,2) NOT NULL,
  approved_amount NUMERIC(12,2),
  currency VARCHAR(10) NOT NULL DEFAULT 'KRW',
  reason VARCHAR(500),
  provider_refund_id VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_refunds_booking ON refunds(booking_id);
