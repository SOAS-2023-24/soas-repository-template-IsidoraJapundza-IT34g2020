package soas.tradeService.implementation;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class TradeServiceImplementation implements TradeService{

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
	public ResponseEntity<?> trade(String from, String to, BigDecimal amount, String authorizationHeader) {

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
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
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
		
		if (!"EUR".equals(from) && !"USD".equals(from)) {
			amount = convertOtherFiatToUSD(from, amount);
			from = "USD";
		}
		
		TradeServiceModel exchangeRate = getExchangeRate(from, to);
		if (exchangeRate == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exchange rate not found.");
		}
		
		BigDecimal cryptoQuantity = amount.multiply(exchangeRate.getConversionRate());
		
		ResponseEntity<?> updateAccountResponse = bankProxy.updateBalances(userEmail, from, null, amount, null);
		if (!updateAccountResponse.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update bank account.");
		}
		
		ResponseEntity<?> updateWalletResponse = walletProxy.updateBalance(userEmail, to, cryptoQuantity, authorizationHeader);
		if (!updateWalletResponse.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update crypto wallet.");
		}
		
		String message = "Conversion successful: " + amount + " " + from + " exchanged for " + cryptoQuantity + " " + to;
		return ResponseEntity.ok().body(new Object() {
			public Object getBody() {
				return updateWalletResponse.getBody();
			}
			
			public String getMessage() {
				return message;
			}
		});
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
		
		TradeServiceModel exchangeRate = getExchangeRate(from, to);
		if (exchangeRate == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exchange rate not found.");
		}
		
		ResponseEntity<?> updateWalletResponse = walletProxy.updateBalance(userEmail, from, amount.negate(), authorizationHeader);
		if (!updateWalletResponse.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update crypto wallet.");
		}
		
		BigDecimal fiatQuantity = amount.multiply(exchangeRate.getConversionRate());
		
		ResponseEntity<?> updateAccountResponse = bankProxy.updateBalances(userEmail, null, to, null, fiatQuantity);
		if (!updateAccountResponse.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update bank account.");
		}
		
		String message = "Conversion successful: " + amount + " " + from + " exchanged for " + fiatQuantity + " " + to;
		return ResponseEntity.ok().body(new Object() {
			public Object getBody() {
				return updateAccountResponse.getBody();
			}
			public String getMessage() {
				return message;
			}
		});
	}
	
	public TradeServiceModel getExchangeRate(String from, String to) {
		return repo.findByFromAndTo(from, to);
	}	
}
