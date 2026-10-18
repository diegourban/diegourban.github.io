CREATE TABLE entity39 (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2),
    effective_date DATE,
    created_at TIMESTAMP WITH TIME ZONE,
    version BIGINT,
    parent_id BIGINT REFERENCES entity38(id)
);
CREATE INDEX idx_entity39_status ON entity39 (status);
