CREATE TABLE entity29 (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2),
    effective_date DATE,
    created_at TIMESTAMP WITH TIME ZONE,
    version BIGINT,
    parent_id BIGINT REFERENCES entity28(id)
);
CREATE INDEX idx_entity29_status ON entity29 (status);
