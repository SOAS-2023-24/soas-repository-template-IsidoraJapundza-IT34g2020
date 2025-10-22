package soas.tradeService.implementation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import api.dto.TradeResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import api.dto.CurrencyExchangeDto;
import api.feignProxies.BankAccountProxy;
import api.feignProxies.CryptoWalletProxy;
import api.feignProxies.CurrencyExchangeProxy;
import api.feignProxies.UsersProxy;
import api.services.TradeService;
import feign.FeignException;
import soas.tradeService.model.TradeServiceModel;
import soas.tradeService.repository.TradeServiceRepository;

@RestController
public class TradeServiceImplementation implements TradeService {

	@Autowired
	private UsersProxy userProxy;
	
	@Autowired
	private TradeServiceRepository repo;
	
	@Autowired 
	private BankAccountProxy bankProxy;
	
	@Autowired
	private CryptoWalletProxy walletProxy;
	
	@Autowired
    private CurrencyExchangeProxy currencyExchangeProxy;

	@Override
	@GetMapping("/trade-service")
	public ResponseEntity<?> trade(@RequestParam String from, @RequestParam String to, @RequestParam("quantity") BigDecimal amount, @RequestHeader("Authorization") String authorizationHeader) {

		try {
			String userRole = userProxy.getCurrentUserRole(authorizationHeader);
		
			if ("USER".equals(userRole)) {
				String userEmail = userProxy.getCurrentUserEmail(authorizationHeader);
				
				if (isValidFiatToCryptoExchange(from, to)) {
					return handleFiatToCryptoExchange(from, to, amount, userEmail, authorizationHeader);
                } else if (isValidCryptoToFiatExchange(from, to)) {
                    return handleCryptoToFiatExchange(from, to, amount, userEmail, authorizationHeader);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request.");
                }
				
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request.");
			}
		} catch (FeignException e) {
			return handleFeign(e);
			/*return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());*/
		}
	}
	
	// helpeser
	private boolean isValidFiatToCryptoExchange(String from, String to) {
		return isSupportedFiatCurrency(from) && isSupportedCryptoCurrency(to);
	}
	
	private boolean isValidCryptoToFiatExchange(String from, String to) {
		return isSupportedCryptoCurrency(from) && isSupportedFiatCurrency(to);
	}
	
	private boolean isSupportedFiatCurrency(String currency) {
		return "EUR".equals(currency) ||
			   "USD".equals(currency) ||
			   "GBP".equals(currency) ||
			   "CHF".equals(currency) ||
			   "RSD".equals(currency);
	}
	
	private boolean isSupportedCryptoCurrency(String currency) {
		return "BTC".equals(currency) ||
			   "ETH".equals(currency) ||
			   "LTC".equals(currency);
	}
	
	private ResponseEntity<?> handleFiatToCryptoExchange(String from, String to, BigDecimal amount, String userEmail, String authorizationHeader) {
		
		final String originalFrom = from;
		final BigDecimal originalAmount = amount;
		
		BigDecimal tradeFiatAmount = amount;
		String tradeFiat = from;
		
		if (!"EUR".equals(from) && !"USD".equals(from)) {
			tradeFiatAmount = convertOtherFiatToUSD(from, amount); //amount
			tradeFiat = "USD"; // ffrom
		}
		
		TradeServiceModel exchangeRate = getExchangeRate(tradeFiat, to);
		if (exchangeRate == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exchange rate not found.");
		}
		
		BigDecimal cryptoQuantity = tradeFiatAmount.multiply(exchangeRate.getConversionRate()).setScale(8, RoundingMode.HALF_UP);
		
		ResponseEntity<?> updateAccountResponse = bankProxy.updateBalances(userEmail, originalFrom, null, originalAmount, null);
		if (!updateAccountResponse.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update bank account.");
		}
		
		ResponseEntity<?> updateWalletResponse = walletProxy.updateBalance(userEmail, to, cryptoQuantity, authorizationHeader);
		if (!updateWalletResponse.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update crypto wallet.");
		}
		
		String message = "Conversion successful: " + originalAmount.stripTrailingZeros().toPlainString() + " " + originalFrom + " exchanged for " + cryptoQuantity.stripTrailingZeros().toPlainString() + " " + to;
		
		var bankSnapshot = bankProxy.getBankAccountByEmail(userEmail);
		var walletSnapshot = walletProxy.getUsersWallet(authorizationHeader);
		
		return ResponseEntity.ok(new TradeResponse(message, bankSnapshot, walletSnapshot));

		/*return ResponseEntity.ok().body(new Object() {
			public Object getBody() {
				return updateWalletResponse.getBody();
			}
			
			public String getMessage() {
				return message;
			}
		});*/
	}
	
	private BigDecimal convertOtherFiatToUSD(String from, BigDecimal amount) {
		ResponseEntity<CurrencyExchangeDto> response = currencyExchangeProxy.getExchange(from, "USD");
		CurrencyExchangeDto responseBody = response.getBody();
		BigDecimal exchangeValue = responseBody.getExchangeValue();
		
		if (exchangeValue == null) {
			throw new RuntimeException("Exchange rate not found for conversion from " + from + " to USD.");
		}
		return amount.multiply(exchangeValue);
	}
	
	private ResponseEntity<?> handleCryptoToFiatExchange(String from, String to, BigDecimal amount, String userEmail, String authorizationHeader) {
		
		/*TradeServiceModel exchangeRate = getExchangeRate(from, to);
		if (exchangeRate == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exchange rate not found.");
		}*/
		
		ResponseEntity<?> updateWalletResponse = walletProxy.updateBalance(userEmail, from, amount.negate(), authorizationHeader);
		if (!updateWalletResponse.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update crypto wallet.");
		}
		
		BigDecimal fiatQuantity; // = amount.multiply(exchangeRate.getConversionRate());
		
		//
		if ("USD".equalsIgnoreCase(to) || "EUR".equalsIgnoreCase(to)) {
			TradeServiceModel direct = getExchangeRate(from, to);
			if (direct == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("Exchange rate not found for " + from + " to " + to);
			}
			fiatQuantity = amount.multiply(direct.getConversionRate());
		} else {
			// pokusaj preko usd
			TradeServiceModel toUsd = getExchangeRate(from, "USD");
			if (toUsd != null) {
				BigDecimal usdAmount = amount.multiply(toUsd.getConversionRate());
				var usdToTarget = currencyExchangeProxy.getExchange("USD", to);
				if (usdToTarget.getBody() == null || usdToTarget.getBody().getExchangeValue() == null) {
					return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
	                        .body("Fiat conversion unavailable USD -> " + to);
				}
				fiatQuantity = usdAmount.multiply(usdToTarget.getBody().getExchangeValue());
			} else {
				// fallback preko eur
				TradeServiceModel toEur = getExchangeRate(from, "EUR");
				if (toEur == null) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exchange rate not found for " + from + " to USD/EUR");
				}
				BigDecimal eurAmount = amount.multiply(toEur.getConversionRate());
				var eurToTarget = currencyExchangeProxy.getExchange("EUR", to);
				if (eurToTarget.getBody() == null || eurToTarget.getBody().getExchangeValue() == null) {
					return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
							.body("Fiat conversion unavailable EUR -> " + to);
				}
				fiatQuantity = eurAmount.multiply(eurToTarget.getBody().getExchangeValue());
			}
		}
		
		fiatQuantity = fiatQuantity.setScale(4, RoundingMode.HALF_UP);
		//
		
		ResponseEntity<?> updateAccountResponse = bankProxy.updateBalances(userEmail, null, to.toUpperCase(), null, fiatQuantity);
		if (!updateAccountResponse.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update bank account.");
		}
		
		String message = "Conversion successful: " + amount.stripTrailingZeros().toPlainString() + " " + from + " exchanged for " + fiatQuantity.stripTrailingZeros().toPlainString() + " " + to;
		var bankSnapshot   = bankProxy.getBankAccountByEmail(userEmail);
		var walletSnapshot = walletProxy.getUsersWallet(authorizationHeader);

		return ResponseEntity.ok(new TradeResponse(message, bankSnapshot, walletSnapshot));
		
		/*return ResponseEntity.ok().body(new Object() {
			public Object getBody() {
				return updateAccountResponse.getBody();
			}
			public String getMessage() {
				return message;
			}
		});*/
	}
	
	public TradeServiceModel getExchangeRate(String from, String to) {
		return repo.findByFromAndTo(from, to);
	}	
	
	private ResponseEntity<?> handleFeign(FeignException e) {
	    int status = e.status(); // npr. 400
	    String msg;
	    try {
	        msg = e.contentUTF8(); 
	        if (msg == null || msg.isBlank()) msg = e.getMessage();
	    } catch (Throwable ignore) {
	        msg = e.getMessage();
	    }
	    if (status >= 400 && status < 500) {
	        return ResponseEntity.status(status).body(msg);
	    }
	    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
	            .body("Downstream error: " + msg);
	}
}
