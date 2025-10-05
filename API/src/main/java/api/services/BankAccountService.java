package api.services;

import java.math.BigDecimal;
import java.util.List;

import api.dto.BankAccountDto;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

public interface BankAccountService {

	@GetMapping("/bank-accounts")
	ResponseEntity<List<BankAccountDto>> getAllAccounts(@RequestHeader("Authorization") String authorizationHeader);
	
	@GetMapping("/bank-accounts/{email}")
	BankAccountDto getBankAccountByEmail(@PathVariable("email") String email);
	
	@GetMapping("/bank-account/user")
	BankAccountDto getBankAccountForUser(@RequestHeader("Authorization") String authorizationHeader);

    @PostMapping("/bank-accounts")
    ResponseEntity<?> createBankAccount(@Valid @RequestBody BankAccountDto dto, @RequestHeader("Authorization") String authorizationHeader);

    @PutMapping("/bank-accounts/{email}")
    ResponseEntity<?> updateBankAccount(@PathVariable String email, @Valid @RequestBody BankAccountDto dto, @RequestHeader("Authorization") String authorizationHeader);

    @DeleteMapping("/bank-accounts/{email}")
    void deleteBankAccount(@PathVariable("email") String email, @RequestHeader("Authorization") String authorizationHeader);
    
    @GetMapping("/bank-account/{email}/{currencyFrom}")
    BigDecimal getUserCurrencyAmount(@PathVariable("email") String email, @PathVariable("currencyFrom") String currencyFrom);
    
    @PutMapping("/bank-account/account")
    ResponseEntity<?> updateBalances(@RequestParam(value = "email") String email, 
    											  @RequestParam(value = "from", required = false) String from,
    										      @RequestParam(value = "to", required = false) String to,
    										      @RequestParam(value = "quantity", required = false) BigDecimal quantity,
    										      @RequestParam(value = "totalAmount", required = false) BigDecimal totalAmount);
}
