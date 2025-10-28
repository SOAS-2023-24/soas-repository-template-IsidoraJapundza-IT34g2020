package usersService.implementation;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import api.dto.UserRequestDto;
import api.dto.UserResponseDto;
import api.services.UsersService;
import feign.FeignException;
import jakarta.validation.Valid;
import usersService.model.UserModel;
import usersService.repository.UsersServiceRepository;
import api.dto.BankAccountDto;
import api.dto.CryptoWalletDto;
import api.feignProxies.BankAccountProxy;
import api.feignProxies.CryptoWalletProxy;

@RestController
public class UserServiceImplementation implements UsersService{

	private final UsersServiceRepository repo;
	private final BankAccountProxy bankAccountProxy;
	private CryptoWalletProxy cryptoWalletProxy;
	
	@Autowired
	public UserServiceImplementation(UsersServiceRepository repo, BankAccountProxy bankAccountProxy, CryptoWalletProxy cryptoWalletProxy) {
		this.repo = repo;
		this.bankAccountProxy = bankAccountProxy;
		this.cryptoWalletProxy = cryptoWalletProxy;
	}

	@Override
	public List<UserResponseDto> getUsers(@RequestHeader("Authorization")String authorizationHeader) {
		var caller = authenticate(authorizationHeader);
		var role = caller.getRole();
		
		if(!"OWNER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "USER nema pristup ovom servisu");
		}
		List<UserModel> allUser = repo.findAll();
		List<UserResponseDto> out = new ArrayList<>(allUser.size());
		for (UserModel m : allUser) out.add(toResponse(m));
		return out;
	}

	@Override
	public ResponseEntity<?> createUser(@Valid @RequestBody UserRequestDto dto, @RequestHeader("Authorization") String authorizationHeader) {
		
		var caller = authenticate(authorizationHeader); 
		var callerRole = caller.getRole();
		
	    if (dto.getRole() == null) {
	        return ResponseEntity.badRequest().body("Role is required.");
	    }
	    String targetRole = dto.getRole().trim().toUpperCase();
	    if (!targetRole.equals("USER") && !targetRole.equals("ADMIN") && !targetRole.equals("OWNER")) {
	        return ResponseEntity.badRequest().body("Unsupported role: " + dto.getRole());
	    }
	    dto.setRole(targetRole); 
		
		
		if ("USER".equalsIgnoreCase(callerRole)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this service.");
		}
		
		if ("ADMIN".equalsIgnoreCase(callerRole) && !"USER".equalsIgnoreCase(targetRole)) {
			// admin moze da kreira samo USER
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin can only create users with role 'USER'.");
		}
		
		// OWNER moze USER ili ADMIN
		
		// unique email
		if (repo.existsByEmail(dto.getEmail())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("User with email " + dto.getEmail() + " already exists.");
		}
		
		// samo 1 owner
		if ("OWNER".equalsIgnoreCase(targetRole) && repo.existsByRole("OWNER")) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("A user with role 'OWNER' already exists.");
		}
		
		UserModel created = repo.save(convertDtoToModel(dto));
		
		// ako se pravi USER, napravi i racun
		boolean bankCreated = false;
		boolean walletCreated = false;
	    //if ("ADMIN".equalsIgnoreCase(callerRole) && "USER".equalsIgnoreCase(targetRole)) {
	    if ("USER".equalsIgnoreCase(targetRole)) {  
			try {
	            BankAccountDto ba = new BankAccountDto();
	            ba.setEmail(created.getEmail());

	            ResponseEntity<?> resp = bankAccountProxy.createBankAccount(ba, authorizationHeader);
	            bankCreated = (resp != null && resp.getStatusCode().is2xxSuccessful());
	            /*if (resp == null || !resp.getStatusCode().is2xxSuccessful()) {
	                repo.deleteById(created.getId());
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                        .body("Failed to create bank account for user.");
	            }*/
	        } catch (Exception ex) {
	        	bankCreated = false; // ne rusimo kreiranje usera
	            /*repo.deleteById(created.getId());
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("Error creating bank account: " + ex.getMessage());*/
	        }
	        
	        try {
	        	CryptoWalletDto cw = new CryptoWalletDto();
	        	cw.setEmail(created.getEmail());
	        	ResponseEntity<?> respW = cryptoWalletProxy.createWallet(cw, authorizationHeader);
	        	walletCreated = (respW != null && respW.getStatusCode().is2xxSuccessful());
	        } catch (Exception ex) {
	        	walletCreated = false;
	        }
	    }
				
		var body = Map.of(
				"id", created.getId(),
		        "email", created.getEmail(),
		        "role", created.getRole(),
		        "bankAccountCreated", bankCreated,
		        "cryptoWalletCreated", walletCreated,
		        "note", bankCreated ? "Bank account created by ADMIN." : "Bank account not created (requires ADMIN)."    		
		);
		// 201
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@Override
	public ResponseEntity<?> updateUser(@PathVariable int id, @Valid @RequestBody UserRequestDto dto, @RequestHeader("Authorization") String authorizationHeader) {
		
		String callerRole = roleFrom(authorizationHeader);
		
		if ("USER".equalsIgnoreCase(callerRole)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this service.");
		}
		
		// user
		UserModel user = repo.findById(id).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with ID " + id + " is not found.");
		}
		
		// admin moze samo da azurira korisnike koji su trenutno USER
		if ("ADMIN".equalsIgnoreCase(callerRole)) {
			if (!"USER".equalsIgnoreCase(user.getRole())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin can update only users with current role 'USER'.");
			}
			// admin ne sme da promeni rolu u nesto drugo osim user
			if (dto.getRole() != null && !"USER".equalsIgnoreCase(dto.getRole())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin can not change role to anything other than 'USER'.");
			}
		}
		
		// unique email
		if (dto.getEmail() != null
				&& !dto.getEmail().equalsIgnoreCase(user.getEmail())
				&& repo.existsByEmail(dto.getEmail())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Email '" + dto.getEmail() + "' is already in use.");
		}
		
		// samo 1 owner
		String newRole = dto.getRole() != null ? dto.getRole() : user.getRole();
		if ("OWNER".equalsIgnoreCase(newRole)) {
			boolean alreadyOwnerExists = repo.existsByRole("OWNER");
			boolean targetAlreadyOwner = "OWNER".equalsIgnoreCase(user.getRole());
			
			if (!targetAlreadyOwner && alreadyOwnerExists) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("An 'OWNER' already exists.");
			}
		}
		
		// izmene
		if (dto.getEmail() != null) {
			user.setEmail(dto.getEmail());
		}
		
		if (dto.getPassword() != null) {
			user.setPassword(dto.getPassword());
		}
		
		if (dto.getRole() != null) {
			user.setRole(dto.getRole());
		}
		
		UserModel saved = repo.save(user);
		return ResponseEntity.ok(toResponse(saved));
	}
		
	@Override
	public ResponseEntity<?> deleteUser(@PathVariable int id,  @RequestHeader("Authorization") String authorizationHeader) {
		
		String callerRole = roleFrom(authorizationHeader);
		
		UserModel target = repo.findById(id).orElse(null);
		if (target == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with ID " + id + " is not found.");
		}
		
		// user nema pristup
		if ("USER".equalsIgnoreCase(callerRole)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this service.");
		}
		
		// admin sme brisati samo user-a
		if ("ADMIN".equalsIgnoreCase(callerRole)) {
			if (!"USER".equalsIgnoreCase(target.getRole())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin can only delete users with role 'USER'.");
			}
			
			boolean bankDeleted = false;
			boolean walletDeleted = false;
			try {
				bankAccountProxy.deleteBankAccount(target.getEmail(), authorizationHeader);
	            bankDeleted = true;
			} catch (FeignException ex) {
				// ne rusimo brisnaj eusera
				bankDeleted = false;
			}
			
			try {
				cryptoWalletProxy.deleteWallet(target.getEmail());
	            walletDeleted = true;
			} catch (FeignException ex) {
				// ne rusimo brisnaj eusera
				walletDeleted = false;
			}
			//prvo pokusaj brisanja bank accoutna
			//var err = tryDeleteBankAccount(target.getEmail(), authorizationHeader);
			//if (err != null) return err;
			
			repo.deleteById(id);
			return ResponseEntity.ok(Map.of(
					"userDeleted", true,
		            "bankAccountDeleted", bankDeleted,
		            "cryptoWalletDeleted", walletDeleted,
		            "note", bankDeleted ? "Bank account deleted by ADMIN." : "Bank account deletion skipped/failed (ADMIN only)."
		    ));
		}
		
		// owner sme brisati sve
		if ("OWNER".equalsIgnoreCase(callerRole)) {
			repo.deleteById(id);
			return ResponseEntity.ok(Map.of(
		            "userDeleted", true,
		            "bankAccountDeleted", false,
		            "note", "Bank account deletion skipped (OWNER has no access to bank-account)."
		    ));
			/*var err = tryDeleteBankAccount(target.getEmail(), authorizationHeader);
			if (err != null) return err;
			
			repo.deleteById(id);
			return ResponseEntity.ok("User with ID " + id + " has been deleted.");*/
		}
		
		// ostalo  401
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized role.");
	}

	@Override
	public String getCurrentUserRole(String authorizationHeader) {
		return roleFrom(authorizationHeader);
	}

	@Override
	public String getCurrentUserEmail(String authorizationHeader) {
		return emailFrom(authorizationHeader);
	}

	@Override
	public Boolean getUser(String email) {
		UserModel user = repo.findByEmail(email);
		return user != null;
	}
	
	public UserModel convertDtoToModel(UserRequestDto dto) {
		return new UserModel(dto.getEmail(), dto.getPassword(), dto.getRole());
	}
	
	/*private ResponseEntity<?> tryDeleteBankAccount(String email, String auth) {
	    try {
	        bankAccountProxy.deleteBankAccount(email, auth);
	        return null;
	    } catch (feign.FeignException.NotFound nf) {
	        return null; 
	    } catch (feign.FeignException fe) {
	        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
	                .body("Failed to delete bank account: " + fe.getMessage());
	    }
	}*/
	
	private record Creds(String email, String password) {}
	
	private static final java.nio.charset.Charset UTF8 = java.nio.charset.StandardCharsets.UTF_8;
	
	private Creds parseBasic(String authorizationHeader) {
	    if (authorizationHeader == null || !authorizationHeader.toLowerCase().startsWith("basic ")) {
	        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
	    }
	    String b64 = authorizationHeader.substring(6).trim();
	    String decoded = new String(Base64.getDecoder().decode(b64), UTF8);
	    int idx = decoded.indexOf(':');
	    if (idx <= 0) {
	        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Basic credentials format");
	    }
	    return new Creds(decoded.substring(0, idx), decoded.substring(idx + 1));
	}
	
	private UserModel authenticate(String authorizationHeader) {
	    Creds c = parseBasic(authorizationHeader);
	    UserModel u = repo.findByEmail(c.email());
	    if (u == null || !u.getPassword().equals(c.password())) {
	        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
	    }
	    return u;
	}

	private UserResponseDto toResponse(UserModel m) {
	    return new UserResponseDto(m.getId(), m.getEmail(), m.getRole());
	}
	
	private String roleFrom(String authorizationHeader) {
		return authenticate(authorizationHeader).getRole(); 
	} 
	
	private String emailFrom(String authorizationHeader) {
		return parseBasic(authorizationHeader).email();
	}

	@Override
	public ResponseEntity<List<UserRequestDto>> getUsersForAuth() {
		var list = repo.findAll().stream()
				.map(u -> new UserRequestDto(u.getEmail(), u.getPassword(), u.getRole()))
				.toList();
		return ResponseEntity.ok(list);
	}
	
	
}