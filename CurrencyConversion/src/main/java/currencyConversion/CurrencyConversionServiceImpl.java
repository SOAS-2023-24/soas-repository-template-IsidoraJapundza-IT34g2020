package currencyConversion;

import java.math.BigDecimal;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import api.dto.CurrencyConversionDto;
import api.dto.CurrencyExchangeDto;
import api.feignProxies.CurrencyExchangeProxy;
import api.services.CurrencyConversionService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import util.exceptions.NoDataFoundException;

@RestController
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

	@Autowired
	private CurrencyExchangeProxy proxy;
	
	private RestTemplate template = new RestTemplate();
	
	Retry retry;
	CurrencyExchangeDto response;
	
	public CurrencyConversionServiceImpl(RetryRegistry registry) {
		this.retry = registry.retry("default");
	}
	
	@Override
	public ResponseEntity<?> getConversion(String from, String to, BigDecimal quantity) {
		HashMap<String, String> uriVariables = new HashMap<String,String>();
		uriVariables.put("from", from);
		uriVariables.put("to", to);
		
		CurrencyExchangeDto exchange;
		
		try {
			ResponseEntity<CurrencyExchangeDto> response = template.getForEntity("http://localhost:8000/currency-exchange?from={from}&to={to}", CurrencyExchangeDto.class, uriVariables);
			// u trenutku izvrsavanja metode vrednosti u {} ce biti yamenjene vrednostima iz hashmape
			exchange = response.getBody();
		}
		catch (HttpClientErrorException e){
			throw new NoDataFoundException(e.getMessage());
		}
		
		return ResponseEntity.ok(exchangeToConversion(exchange, quantity));
	}
	
	@Override
	@CircuitBreaker(name = "cb", fallbackMethod = "fallback")
	public ResponseEntity<?> getConversionFeign(String from, String to, BigDecimal quantity) {
		retry.executeSupplier( () -> response = aquireExchange(from, to) );
		return ResponseEntity.ok(exchangeToConversion(response, quantity));
	}
	
	public CurrencyExchangeDto aquireExchange(String from, String to) {
		return proxy.getExchange(from, to).getBody();
	}
	
	public ResponseEntity<?> fallback(CallNotPermittedException ex){
		return ResponseEntity.status(503).body(ex.getMessage());
	}
	
	public CurrencyConversionDto exchangeToConversion(CurrencyExchangeDto dto, BigDecimal quantity) {
		return new CurrencyConversionDto(dto, quantity, quantity.multiply(dto.getExchangeValue()), dto.getTo());
	}
}
