package api.feignProxies;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import api.dto.CryptoExchangeDto;

@FeignClient("crypto-exchange")
public interface CryptoExchangeProxy {

	@GetMapping("/crypto-exchange")
	ResponseEntity<CryptoExchangeDto> cryptoExchange
			(@RequestParam(value = "from") String from, 
					@RequestParam(value = "to") String to);
}
