package currencyConversion;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import api.dto.CurrencyConversionDto;
import api.dto.CurrencyExchangeDto;
import api.feignProxies.BankAccountProxy;
import api.dto.BankAccountDto;
import api.feignProxies.CurrencyExchangeProxy;
import api.feignProxies.UsersProxy;
import api.services.CurrencyConversionService;
import feign.FeignException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

@RestController
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

	@Autowired
	private CurrencyExchangeProxy exchangeProxy;
	
	//private RestTemplate template = new RestTemplate();
	
	@Autowired
	private BankAccountProxy bankAccountProxy;
	
	@Autowired
	private UsersProxy usersProxy;
	
	Retry retry;
	CurrencyExchangeDto response;
	
	public CurrencyConversionServiceImpl(RetryRegistry registry) {
		this.retry = registry.retry("default");
	}	
	
	@Override
	public ResponseEntity<?> getConversionFeign(@RequestParam String from, @RequestParam String to, @RequestParam  BigDecimal quantity, @RequestHeader("Authorization") String authorizationHeader) {
		try {
			String user = usersProxy.getCurrentUserRole(authorizationHeader);
			
			if (!"USER".equalsIgnoreCase(user)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only USER role is allowed to use currency conversion.");
			}
			
			if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
				return ResponseEntity.badRequest().body("Quantity must be > 0.");
			}
			
			if (!isFiat(from) || !isFiat(to)) {
				return ResponseEntity.badRequest().body("Unsupported fiat currency. Allowed: EUR, USD, GBP, CHF, RSD.");
			}
			
			String userEmail = usersProxy.getCurrentUserEmail(authorizationHeader);
			BankAccountDto bankAccount = bankAccountProxy.getBankAccountByEmail(userEmail);
			
			if (bankAccount == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bank account not found for user.");
			}
			
			BigDecimal availableCurrencyAmount  = bankAccountProxy.getUserCurrencyAmount(userEmail, from);
			if (availableCurrencyAmount == null) availableCurrencyAmount = BigDecimal.ZERO;
			if (availableCurrencyAmount.compareTo(quantity) < 0) {
				return ResponseEntity.badRequest().body("Insufficient funds in " + from + ".");
				//"User doesn't have enough amount in the bank account for exchanging."
			}
			
			ResponseEntity<CurrencyExchangeDto> response = exchangeProxy.getExchange(from, to);
			
			if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Exchange rate not found for " + from + " -> " + to + ".");
				//throw new ServiceUnavailableException("Exchange service response is null.");
			}
			
			CurrencyExchangeDto responseBody = response.getBody();
			
			// racunanje iznosa
			BigDecimal exchangeValue = responseBody.getExchangeValue();
			BigDecimal totalExchanged = exchangeValue.multiply(quantity); 

			// azuriranje stanja
			ResponseEntity<?> updatedBalances = bankAccountProxy.updateBalances(userEmail, from, to, quantity, totalExchanged);
			
			if (updatedBalances == null || !updatedBalances.getStatusCode().is2xxSuccessful()) {
				return ResponseEntity.status(updatedBalances.getStatusCode()).body("Failed to update balances.");
			}
			
			String message = "Conversion was successfull! " + quantity + " - "+ from + " is exchanged for " + to;
			
			// proveriti i srediti jos
			return ResponseEntity.ok().body(new Object() {
				public Object getBody() {
					return updatedBalances.getBody();
				}
				public String getMessage() {
					return message;
				}
			});
			
		} catch (FeignException ex) {
			ex.printStackTrace();
			return ResponseEntity.status(ex.status()).body(ex.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + ex.getMessage());
		}
	}

	public CurrencyConversionDto exchangeToConversion(CurrencyExchangeDto dto, BigDecimal quantity) {
		return new CurrencyConversionDto(dto, quantity, quantity.multiply(dto.getExchangeValue()), dto.getTo());
	}
	
	private boolean isFiat(String c) {
		return "EUR".equalsIgnoreCase(c) ||
		       "USD".equalsIgnoreCase(c) ||
		       "GBP".equalsIgnoreCase(c) ||
		       "CHF".equalsIgnoreCase(c) ||
		       "RSD".equalsIgnoreCase(c);
	}
}