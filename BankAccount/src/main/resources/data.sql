INSERT INTO bank_account (email) 
	VALUES ('user@uns.ac.rs');
	
INSERT INTO fiat_balance (bank_account_id, currency, balance)
	VALUES (1, 'EUR', 1400.00), 
       	   (1, 'USD', 800.00), 
		   (1, 'CHF', 0.00),
           (1, 'GBP', 20.00),
           (1, 'RSD', 11200.00);
           
           
           ////proveriti