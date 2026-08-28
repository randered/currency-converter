-- Demo clients so the service is usable out of the box.
-- CLIENT-001: 10,000 USD and 8,000 EUR
-- CLIENT-002: 5,000 GBP
INSERT INTO clients (client_id)
VALUES ('CLIENT-001'),
       ('CLIENT-002');

INSERT INTO balances (client_id, currency, amount)
VALUES ('CLIENT-001', 'USD', 10000.00),
       ('CLIENT-001', 'EUR', 8000.00),
       ('CLIENT-002', 'GBP', 5000.00);
