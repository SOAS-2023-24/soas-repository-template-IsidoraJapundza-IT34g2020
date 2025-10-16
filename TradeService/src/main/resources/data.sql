-- BTC to USD and EUR
INSERT INTO trade_service (id, currency_from, to_currency, rate) VALUES
(1, 'BTC', 'USD', 62000.00),  -- Example rate for BTC to USD
(2, 'BTC', 'EUR', 58000.00);  -- Example rate for BTC to EUR

-- ETH to USD and EUR
INSERT INTO trade_service (id, currency_from, to_currency, rate) VALUES
(3, 'ETH', 'USD', 2800.00),   -- Example rate for ETH to USD
(4, 'ETH', 'EUR', 2600.00);   -- Example rate for ETH to EUR

-- LTC to USD and EUR
INSERT INTO trade_service (id, currency_from, to_currency, rate) VALUES
(5, 'LTC', 'USD', 140.00),     -- Example rate for LTC to USD
(6, 'LTC', 'EUR', 130.00);     -- Example rate for LTC to EUR

-- USD to BTC, ETH, LTC
INSERT INTO trade_service (id, currency_from, to_currency, rate) VALUES
(7, 'USD', 'BTC', 1 / 62000.00), -- Example rate for USD to BTC
(8, 'USD', 'ETH', 1 / 2800.00),  -- Example rate for USD to ETH
(9, 'USD', 'LTC', 1 / 140.00);    -- Example rate for USD to LTC

-- EUR to BTC, ETH, LTC
INSERT INTO trade_service (id, currency_from, to_currency, rate) VALUES
(10, 'EUR', 'BTC', 1/ 58000.00), -- Example rate for EUR to BTC
(11, 'EUR', 'ETH', 1 / 2600.00),  -- Example rate for EUR to ETH
(12, 'EUR', 'LTC', 1 / 130.00);     -- Example rate for EUR to LTC