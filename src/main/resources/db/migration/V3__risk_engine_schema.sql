CREATE SCHEMA IF NOT EXISTS risk;

CREATE TABLE IF NOT EXISTS risk.risk_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    symbol VARCHAR(20) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    requested_risk_pct DOUBLE PRECISION NOT NULL,
    lot_size DOUBLE PRECISION NOT NULL,
    decision VARCHAR(50) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
