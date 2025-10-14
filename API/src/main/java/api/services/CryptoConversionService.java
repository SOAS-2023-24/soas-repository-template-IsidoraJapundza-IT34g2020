package api.services;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import api.dto.CryptoConversionResponseDto;

public interface CryptoConversionService {

	@GetMapping("/crypto-conversion")
	ResponseEntity<CryptoConversionResponseDto> getConversion(@RequestParam String from, @RequestParam String to, @RequestParam BigDecimal quantity, @RequestHeader("Authorization") String authorizationHeader);
}
