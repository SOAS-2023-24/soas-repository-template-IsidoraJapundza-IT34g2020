package api.services;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

public interface TradeService {

	@GetMapping("/trade-service")
	public ResponseEntity<?> trade(@RequestParam String from,
								   @RequestParam String to,
								   @RequestParam("quantity") BigDecimal amount,
								   @RequestHeader("Authorization") String authorizationHeader);
}
