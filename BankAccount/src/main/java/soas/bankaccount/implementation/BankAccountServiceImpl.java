package soas.bankaccount.implementation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import api.services.BankAccountService;
import api.dto.BankAccountDto;
import api.dto.FiatBalanceDto;
import api.types.Fiat;

import api.feignProxies.UsersProxy;
import soas.bankaccount.model.BankAccountModel;
import soas.bankaccount.model.FiatBalanceModel;
import soas.bankaccount.repository.BankAccountRepository;

@RestController
public class BankAccountServiceImpl implements BankAccountService {

		private final BankAccountRepository repository;
		private final UsersProxy usersProxy;

	    @Autowired
	    public BankAccountServiceImpl(BankAccountRepository repository, UsersProxy usersProxy) {
	        this.repository = repository;
	        this.usersProxy = usersProxy;
	    }

	    // getAllAccounts
		@Override
		public ResponseEntity<List<BankAccountDto>> getAllAccounts(@RequestHeader("Authorization") String authorizationHeader) {
			String role = usersProxy.getCurrentUserRole(authorizationHeader);
		    if (!"ADMIN".equalsIgnoreCase(role)) {
		        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		    }
			
		    var models = repository.findAll();
		    var dtos = models.stream().map(this::convertToDto).collect(Collectors.toList());
		    return ResponseEntity.ok(dtos);
		}

		// getBankAccountByEmail
		@Override
		public BankAccountDto getBankAccountByEmail(String email) {
			BankAccountModel bankAccount = repository.findByEmail(email);
			
			if (bankAccount == null) {
				return null;
			}
			
			return convertToDto(bankAccount);
		}

		// getBankAccountForUser
		@Override
		public BankAccountDto getBankAccountForUser(@RequestHeader("Authorization") String authorizationHeader) {
			String role = usersProxy.getCurrentUserRole(authorizationHeader);
			
			if (!"USER".equalsIgnoreCase(role)) return null;
			
			String email = usersProxy.getCurrentUserEmail(authorizationHeader);
			return getBankAccountByEmail(email);
		}

		// createBankAccount
		@Override
		public ResponseEntity<?> createBankAccount(BankAccountDto dto, String authorizationHeader) {
			String role = usersProxy.getCurrentUserRole(authorizationHeader);

			if (!"ADMIN".equalsIgnoreCase(role)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg("Only ADMIN can create bank account."));
			}
			
			if (dto == null || dto.getEmail() == null || dto.getEmail().isBlank()) {
				return ResponseEntity.badRequest().body(msg("Email is required."));
			}
			
			if (Boolean.FALSE.equals(usersProxy.getUser(dto.getEmail()))) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg("User with email " + dto.getEmail() + " does not exist."));
			}
			
			if (repository.existsByEmail(dto.getEmail())) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(msg("Bank account for user " + dto.getEmail() + " already exists."));
			}
			
			BankAccountModel acc = new BankAccountModel(dto.getEmail());
			acc.setFiatBalances(zeroBalancesFor(acc)); // sve valute iz enuma sa 0
			BankAccountModel saved = repository.save(acc);
			
			return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(saved));
	    }
	
		// updateBankAccount 
		@Override
		public ResponseEntity<?> updateBankAccount(String email, BankAccountDto dto, String authorizationHeader) {

			String role = usersProxy.getCurrentUserRole(authorizationHeader);
			
			if (!"ADMIN".equalsIgnoreCase(role)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can update bank account.");
			}
			
			BankAccountModel account = repository.findByEmail(email);
			if (account == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bank account for '" + email + "' not found.");
			}
			
			if (dto == null || dto.getFiatBalances() == null) {
				return ResponseEntity.badRequest().body("fiatBalances must be provided.");
			}
			
			//obavezne valute iz enuma
			Set<String> required = Arrays.stream(Fiat.values())
					.map(api.types.Fiat::code)
					.collect(Collectors.toSet());
			
			Map<String, BigDecimal> provided = new HashMap<>();
			for (FiatBalanceDto b : dto.getFiatBalances()) {
				String cur = normalizeFiat(b.getCurrency()); 
				if (provided.put(cur, b.getBalance()) != null) {
					return ResponseEntity.badRequest()
							.body("Duplicate currency in payload: " + cur);
				}
				
				if (b.getBalance() == null || b.getBalance().compareTo(BigDecimal.ZERO) < 0) {
					return ResponseEntity.badRequest().body("Balance for " + cur + " must be >= 0.");
				}
			}
			
			Set<String> missing = new HashSet<>(required);
			missing.removeAll(provided.keySet());
			if (!missing.isEmpty()) {
				return ResponseEntity.badRequest().body("Missing required currencies: " + String.join(", ", missing));
			}
			
			for (FiatBalanceModel fb : account.getFiatBalances()) {
				String cur = normalizeFiat(fb.getCurrency());
				BigDecimal newBal = provided.get(cur);
				if (newBal != null) {
					fb.setBalance(newBal);
				}
			}
			
			BankAccountModel saved = repository.save(account);
			return ResponseEntity.ok(convertToDto(saved));
		}

		// deleteBankAccount
		@Override
		public void deleteBankAccount(String email, @RequestHeader("Authorization") String authorizationHeader) {
			String role = usersProxy.getCurrentUserRole(authorizationHeader);
			
			if (!"ADMIN".equalsIgnoreCase(role)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can delete bank accounts.");
			}
		
			repository.deleteByEmail(email);
			// dodati poruku
		}

		// getUserCurrencyAmount
		@Override
		public BigDecimal getUserCurrencyAmount(String email, String currencyFrom) {
			BankAccountModel userAccount = repository.findByEmail(email);
			if (userAccount == null) return BigDecimal.ZERO;
			
			String cur = safeNormalize(currencyFrom);
			
			FiatBalanceModel balance = findFiatBalance(userAccount.getFiatBalances(), cur);
			return balance == null ? BigDecimal.ZERO : balance.getBalance();
		}

		// updateBalances
		@Override
		public ResponseEntity<?> updateBalances(String email, String from, String to, BigDecimal quantity,
				BigDecimal totalAmount) {
			BankAccountModel bankAccount = repository.findByEmail(email);

		    if (bankAccount == null) {
		        return ResponseEntity.notFound().build();
		    }

		    List<FiatBalanceModel> balances = bankAccount.getFiatBalances();

		    if (from != null) {
		        
		        FiatBalanceModel fromBalance = findFiatBalance(balances, from);
		        if (fromBalance == null) {
		            return ResponseEntity.badRequest().body("Currency '" + from + "' not found.");
		        }
		        BigDecimal newFromBalance = fromBalance.getBalance().subtract(quantity);
		        if (newFromBalance.compareTo(BigDecimal.ZERO) < 0) {
		            return ResponseEntity.badRequest().body("Not enough " + from);
		        }
		        fromBalance.setBalance(newFromBalance);
		    }

		    if (to != null && totalAmount != null) {
		        
		        FiatBalanceModel toBalance = findFiatBalance(balances, to);
		        if (toBalance == null) {
		            return ResponseEntity.badRequest().body("Currency '" + to + "' not found.");
		        }
		        BigDecimal newToBalance = toBalance.getBalance().add(totalAmount);
		        toBalance.setBalance(newToBalance);
		    }
		  
		    BankAccountModel updatedAccount = repository.save(bankAccount);
		    BankAccountDto updatedDto = convertToDto(updatedAccount);
		    return ResponseEntity.ok(updatedDto);
		}
	
	    // HELPERS
		private BankAccountDto convertToDto(BankAccountModel model) {
			BankAccountDto dto = new BankAccountDto();
			dto.setEmail(model.getEmail());
			
			if (model.getFiatBalances() != null) {
		        List<FiatBalanceDto> fiatBalanceDtos = model.getFiatBalances().stream()
		                .map(fiatBalanceModel -> {
		                    FiatBalanceDto fiatBalanceDto = new FiatBalanceDto();
		                    fiatBalanceDto.setCurrency(fiatBalanceModel.getCurrency());
		                    fiatBalanceDto.setBalance(fiatBalanceModel.getBalance());
		                    return fiatBalanceDto;
		                })
		                .collect(Collectors.toList());
		        dto.setFiatBalances(fiatBalanceDtos);
		    }

		    return dto;
		}
		
		private String safeNormalize(String code) {
			try {
				return Fiat.from(code).code();
			} catch (Exception e) {
				return null;
			}
		}
				
		private List<FiatBalanceModel> zeroBalancesFor(BankAccountModel acc) {
			return Arrays.stream(Fiat.values())
					.map(f -> {
						FiatBalanceModel balance = new FiatBalanceModel(f.code(), BigDecimal.ZERO);
						balance.setBankAccount(acc);
						return balance;
					})
					.collect(Collectors.toList());
		}
		
		private FiatBalanceModel findFiatBalance(List<FiatBalanceModel> balances, String currency) {
			String cur = safeNormalize(currency); 
			if (cur == null) return null;
			
			return balances.stream().filter(b -> cur.equalsIgnoreCase(b.getCurrency())).findFirst().orElse(null);
					
		}
		
		private String normalizeFiat(String code) {
			try {
				return Fiat.from(code).code();
			} catch (IllegalArgumentException e) {
				throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported fiat: " + code);
			}
		}
		
		private static Map<String, String> msg(String m) {
			return Collections.singletonMap("message", m);
		}
}
