# Aplikacija za razmenu običnih (fiat) i crypto valuta

Ova aplikacija omogućava razmenu fiat i kripto valuta u mikroservisnoj arhitekturi (Spring Cloud + Eureka + API Gateway WebFlux Security Basic Auth + H2).

Svi pozivi idu preko API Gateway-a: http://localhost:8765/.

## KORISNICI I KREDENCIJALI

- **admin@uns.ac.rs** - Admin korisnik sa lozinkom 'password' i rolom 'ADMIN'.
- **user@uns.ac.rs** - Obični korisnik sa lozinkom 'password' i rolom 'USER'.
- **owner@uns.ac.rs** - Vlasnik sa lozinkom 'password' i rolom 'OWNER'.

Auth je Basic kroz Gateway. Gateway korisnike puni sa /users/auth-list i primenjuje role-based pravila.

## USERS SERVICE

### Get All Users

- **URL:** `ttp://localhost:8765/users`
- **Method:** `GET`

### Create a New User

- **URL:** `http://localhost:8765/users/newUser`
- **Method:** `POST`
- **Headers:** `Authorization` required. (ADMIN kreira samo USER; OWNER može USER/ADMIN)

### Update a User

- **URL:** `http://localhost:8765/users/{id}`
- **Method:** `PUT`
- **Headers:** `Authorization` required. (ADMIN menja samo USER; OWNER može sve)

### Delete a User

- **URL:** `http://localhost:8765/users/{id}`
- **Method:** `DELETE`
- **Headers:** `Authorization` required. (ADMIN briše USER; OWNER briše sve)

### Get Current User's Role

- **URL:** `http://localhost:8765/users/current-user-role`
- **Method:** `GET`
- **Headers:** `Authorization` required.

### Get Current User's Email

- **URL:** `http://localhost:8765/users/current-user-email`
- **Method:** `GET`
- **Headers:** `Authorization` required.

### Get User by Email

- **URL:** `http://localhost:8765/users/by-email/{email}`
- **Method:** `GET`

## CURRENCY EXCHANGE SERVICE

### Get Exchange Rate

- **URL:** `http://localhost:8765/currency-exchange`
- **Method:** `GET`
- **Parameters:**
  - `from` (query parameter) - The currency code of the source currency.
  - `to` (query parameter) - The currency code of the target currency.
- **Response:** Returns the exchange rate from the `from` currency to the `to` currency.

## BANK ACCOUNT SERVICE

Role pravila (Gateway):

ADMIN: kreira/čita/menja/briše sve bank naloge

USER: vidi samo svoj račun (/bank-accounts/user)

OWNER: nema pristup bank-account servisu

### Get All Bank Accounts

- **URL:** `http://localhost:8765/bank-accounts`
- **Method:** `GET` (ADMIN)
- **Response:** Returns a list of all bank accounts.

### Get Bank Account by Email

- **URL:** `http://localhost:8765/bank-accounts/{email}`
- **Method:** `GET` (ADMIN)
- **Path Parameters:**
  - `email` - The email of the user whose bank account details are to be fetched.
- **Response:** Returns the bank account details for the specified email.

### Get Bank Account for Current User

- **URL:** `http://localhost:8765/bank-accounts/user`
- **Method:** `GET` (USER self)
- **Headers:** Authorization required.
- **Response:** Returns the bank account details for the user associated with the provided authorization token.

### Create a New Bank Account

- **URL:** `http://localhost:8765/bank-accounts`
- **Method:** `POST` (ADMIN)
- **Headers:** Authorization required.
- **Request Body:** BankAccountDto - The details of the bank account to be created.
- **Response:** Returns the response for the bank account creation request.

### Update Bank Account

- **URL:** `http://localhost:8765/bank-accounts/{email}`
- **Method:** `PUT` (ADMIN)
- **Path Parameters:**
  - `email` - The email of the bank account to be updated.
- **Headers:** Authorization required.
- **Request Body:** BankAccountDto - The updated bank account details.
- **Response:** Returns the response for the bank account update request.

### Delete Bank Account

- **URL:** `http://localhost:8765/bank-accounts/{email}`
- **Method:** `DELETE` (ADMIN) 
- **Path Parameters:**
  - `email` - The email of the bank account to be deleted.
- **Response:** Deletes the bank account for the specified email.

### Get User Currency Amount

- **URL:** `http://localhost:8765/bank-accounts/{email}/{currencyFrom}`
- **Method:** `GET` (ADMIN)
- **Path Parameters:**
  - `email` - The email of the user whose currency amount is to be fetched.
  - `currencyFrom` - The currency code of the currency whose amount is to be fetched.
- **Response:** Returns the amount of the specified currency for the user.

### Update Bank Account Balances

- **URL:** `http://localhost:8765/bank-accounts/account`
- **Method:** `PUT` (ADMIN)
- **Request Parameters:**
  - `email` - The email of the bank account to be updated.
  - `from` - The currency code of the source currency (optional).
  - `to` - The currency code of the target currency (optional).
  - `quantity` - The quantity of currency to be updated (optional).
  - `totalAmount` - The total amount to be updated (optional).
- **Response:** Returns the response for the bank account balance update request.

## CURRENCY CONVERSION SERVICE

### Get Currency Conversion with Feign

- **URL:** `http://localhost:8765/currency-conversion-feign`
- **Method:** `GET`
- **Request Parameters:**
  - `from` - The currency code of the source currency.
  - `to` - The currency code of the target currency.
  - `quantity` - The amount of source currency to be converted.
- **Headers:** Authorization required.
- **Response:** Returns the currency conversion details based on the provided parameters and authorization.

## CRYPTO WALLET SERVICE

### Get All Crypto Wallets

- **URL:** `http://localhost:8765/crypto-wallet`
- **Method:** `GET` (ADMIN) 
- **Response:** Returns a list of all crypto wallets.

### Get Crypto Wallet by Email

- **URL:** `http://localhost:8765/crypto-wallet/{email}`
- **Method:** `GET` (ADMIN) 
- **Path Variables:**
  - `email` - The email address associated with the crypto wallet.
- **Response:** Returns the crypto wallet details for the specified email.

### Delete Crypto Wallet

- **URL:** `http://localhost:8765/crypto-wallet/{email}`
- **Method:** `DELETE` (ADMIN) 
- **Path Variables:**
  - `email` - The email address associated with the crypto wallet.
- **Response:** Deletes the crypto wallet for the specified email.

### Get User Crypto State

- **URL:** `http://localhost:8765/crypto-wallet/{email}/{cryptoFrom}`
- **Method:** `GET` (ADMIN) 
- **Path Variables:**
  - `email` - The email address associated with the crypto wallet.
  - `cryptoFrom` - The crypto currency code for which the state is queried.
- **Response:** Returns the amount of the specified crypto currency in the user's wallet.

### Create Crypto Wallet

- **URL:** `http://localhost:8765/crypto-wallet`
- **Method:** `POST` (ADMIN) 
- **Headers:** Authorization required.
- **Request Body:** `CryptoWalletDto` - Details of the crypto wallet to be created.
- **Response:** Creates a new crypto wallet and returns the result.

### Update Crypto Wallet

- **URL:** `http://localhost:8765/crypto-wallet/{email}`
- **Method:** `PUT` (ADMIN) 
- **Path Variables:**
  - `email` - The email address associated with the crypto wallet.
- **Headers:** Authorization required.
- **Request Body:** `CryptoWalletDto` - Updated details of the crypto wallet.
- **Response:** Updates the crypto wallet for the specified email and returns the result.

### Get User's Crypto Wallet

- **URL:** `http://localhost:8765/crypto-wallet/user`
- **Method:** `GET` (USER self)
- **Headers:** Authorization required.
- **Response:** Returns the crypto wallet details for the currently authenticated user.

### Update Wallet State

- **URL:** `http://localhost:8765/crypto-wallet/wallet`
- **Method:** `PUT` (ADMIN) 
- **Request Parameters:**
  - `email` - The email address associated with the crypto wallet.
  - `from` - The crypto currency code to be updated.
  - `to` - The crypto currency code to be updated (if applicable).
  - `quantity` - The quantity of the crypto currency to be updated.
  - `totalAmount` - The total amount to be updated (if applicable).
- **Response:** Updates the state of the crypto wallet and returns the result.

## CRYPTO EXCHANGE SERVICE

### Get Crypto Exchange Rate

- **URL:** `http://localhost:8765/crypto-exchange`
- **Method:** `GET`
- **Request Parameters:**
  - `from` - The crypto currency code to exchange from.
  - `to` - The crypto currency code to exchange to.
- **Response:** Returns the exchange rate between the specified crypto currencies.

## CRYPTO CONVERSION SERVICE

### Get Crypto Conversion

- **URL:** `http://localhost:8765/crypto-conversion`
- **Method:** `GET`
- **Request Parameters:**
  - `from` - The crypto currency code to convert from.
  - `to` - The crypto currency code to convert to.
  - `quantity` - The amount of the `from` currency to convert.
- **Headers:**
  - `Authorization` - Required header for authorization.
- **Response:** Returns the result of the conversion from the specified crypto currency to another, including the converted amount.

## TRADE SERVICE

### Trade

- **URL:** `http://localhost:8765/trade-service`
- **Method:** `GET`
- **Request Parameters:**
  - `from` - The currency or crypto code to trade from.
  - `to` - The currency or crypto code to trade to.
  - `amount` - The amount of the `from` currency or crypto to trade.
- **Headers:**
  - `Authorization` - Required header for authorization.
- **Response:** Returns the result of the trade operation, including the traded amount and possibly additional details about the transaction.
