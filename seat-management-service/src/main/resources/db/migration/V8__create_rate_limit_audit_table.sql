-- Audit table for rate limit / abuse detection events
CREATE TABLE IF NOT EXISTS rate_limit_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_ip VARCHAR(45) NOT NULL,
    request_uri VARCHAR(500),
    action VARCHAR(50) NOT NULL DEFAULT 'RATE_LIMIT_EXCEEDED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rate_limit_audit_ip ON rate_limit_audit(client_ip);
CREATE INDEX IF NOT EXISTS idx_rate_limit_audit_created ON rate_limit_audit(created_at);
