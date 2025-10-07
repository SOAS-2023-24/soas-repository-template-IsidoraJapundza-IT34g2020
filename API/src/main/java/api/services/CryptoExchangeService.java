package api.services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import api.dto.CryptoExchangeDto;

public interface CryptoExchangeService {

	 @GetMapping("/crypto-exchange")
	 ResponseEntity<CryptoExchangeDto> cryptoExchange(@RequestParam String from, @RequestParam String to);
}
