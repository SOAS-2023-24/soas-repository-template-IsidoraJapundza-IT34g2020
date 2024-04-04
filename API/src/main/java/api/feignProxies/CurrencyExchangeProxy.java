package api.feignProxies;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import api.dto.CurrencyExchangeDto;

@FeignClient("currency-exchange")
public interface CurrencyExchangeProxy {

	@GetMapping("/currency-exchange")
	ResponseEntity<CurrencyExchangeDto> getExchange
			(@RequestParam String from, @RequestParam String to);
}