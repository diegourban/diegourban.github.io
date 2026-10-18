CREATE TABLE entity05 (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2),
    effective_date DATE,
    created_at TIMESTAMP WITH TIME ZONE,
    version BIGINT,
    parent_id BIGINT REFERENCES entity04(id)
);
CREATE INDEX idx_entity05_status ON entity05 (status);
