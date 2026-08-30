-- idx_conversion_client was redundant: client_id is the leftmost prefix of the
-- unique (client_id, idempotency_key) index. Replace it with a composite that
-- also serves the common "conversion history for a client on a date" query.
DROP INDEX idx_conversion_client;

CREATE INDEX idx_conversion_client_created_at ON conversion_records (client_id, created_at);
