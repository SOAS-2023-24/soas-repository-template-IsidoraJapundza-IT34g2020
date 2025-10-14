package api.feignProxies;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import api.dto.CryptoWalletDto;

@FeignClient(name = "crypto-wallet")
public interface CryptoWalletProxy {

	@GetMapping("/crypto-wallet/user")
	CryptoWalletDto getUsersWallet(@RequestHeader("Authorization") String authorizationHeader);
	
	@GetMapping("/crypto-wallet/{email}")
    CryptoWalletDto getWalletByEmail(@PathVariable("email") String email);

    @PostMapping("/crypto-wallet")
    ResponseEntity<?> createWallet(@RequestBody CryptoWalletDto dto, @RequestHeader("Authorization") String authorizationHeader);

    @PutMapping("/crypto-wallet/{email}")
    ResponseEntity<?> updateWallet(@PathVariable("email") String email,
                                   @RequestBody CryptoWalletDto dto,
                                   @RequestHeader("Authorization") String authorizationHeader);
    
    @DeleteMapping("/crypto-wallet/{email}")
    void deleteWallet(@PathVariable("email") String email);

    @GetMapping("/crypto-wallet/{email}/{cryptoFrom}")
    BigDecimal getUserCryptoAmount(@PathVariable("email") String email, @PathVariable("cryptoFrom") String cryptoFrom);

    
}
