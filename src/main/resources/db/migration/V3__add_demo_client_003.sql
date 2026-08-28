-- Demo client CLIENT-003, added in a separate migration so databases that
-- already applied V2 are not invalidated (applied migrations are immutable).
INSERT INTO clients (client_id)
VALUES ('CLIENT-003');

INSERT INTO balances (client_id, currency, amount)
VALUES ('CLIENT-003', 'CAD', 6500.00);
