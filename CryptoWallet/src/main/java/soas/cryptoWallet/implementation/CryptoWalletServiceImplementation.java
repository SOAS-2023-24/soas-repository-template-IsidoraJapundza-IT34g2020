package soas.cryptoWallet.implementation;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import api.dto.CryptoPairDto;
import api.dto.CryptoWalletDto;
import api.services.CryptoWalletService;
import api.types.Crypto;
import soas.cryptoWallet.model.CryptoPairModel;
import soas.cryptoWallet.model.CryptoWalletModel;
import soas.cryptoWallet.repository.CryptoPairRepository;
import soas.cryptoWallet.repository.CryptoWalletRepository;
import util.exceptions.NoDataFoundException;

@RestController
public class CryptoWalletServiceImplementation implements CryptoWalletService{

	@Autowired
	private CryptoWalletRepository walletRepo;
	@Autowired
	private CryptoPairRepository pairRepo;
	
	
	@Override
	public ResponseEntity<List<CryptoWalletDto>> getAllWallets() {
		List<CryptoWalletDto> dtos = walletRepo.findAll()
				.stream().map(this::toDto).collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}
	@Override
	public CryptoWalletDto getWalletByEmail(String email) {
		CryptoWalletModel m = walletRepo.findByEmail(email);
		if (m == null) throw new NoDataFoundException("Wallet not found for email: " + email);
		return toDto(m);
	}
	@Override
	public void deleteWallet(String email) {
		CryptoWalletModel m = walletRepo.findByEmail(email);
		if (m == null) throw new NoDataFoundException("Wallet not found for email: " + email);
		walletRepo.delete(m);
	}
	@Override
	public ResponseEntity<?> createWallet(CryptoWalletDto dto, String authorizationHeader) {
		// kreira se preko gatewaua, ovde smao validacija
		// admin kreira
		if (dto == null || dto.getEmail() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");

		
		if (walletRepo.existsByEmail(dto.getEmail()))
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Wallet already exists for email: " + dto.getEmail());
		
		CryptoWalletModel saved = saveFromDto(new CryptoWalletModel(dto.getEmail()), dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
	}
	@Override
	public ResponseEntity<?> updateWallet(String email, CryptoWalletDto dto, String authorizationHeader) {
		CryptoWalletModel m = walletRepo.findByEmail(email);
		if (m == null) throw new NoDataFoundException("Wallet not found for email: " + email);
	
		if (dto.getEmail() != null && !Objects.equals(dto.getEmail(), email))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be changed");

		CryptoWalletModel saved = saveFromDto(m, dto);
		return ResponseEntity.ok(toDto(saved));
	}
	@Override
	public CryptoWalletDto getUsersWallet(String authorizationHeader) {
		String email = extractEmailFromBasicAuth(authorizationHeader);
		return getWalletByEmail(email);
	}
	
	// ========== Dodatni endpoint potreban drugim servisima ==========
    // (matchuje CryptoWalletProxy.getUserCryptoAmount)
    @org.springframework.web.bind.annotation.GetMapping("/crypto-wallet/{email}/{cryptoFrom}")
    public BigDecimal getUserCryptoAmount(
            @org.springframework.web.bind.annotation.PathVariable String email,
            @org.springframework.web.bind.annotation.PathVariable String cryptoFrom) {

        CryptoWalletModel m = walletRepo.findByEmail(email);
        if (m == null) throw new NoDataFoundException("Wallet not found for email: " + email);

        String code = normalizeCrypto(cryptoFrom);
        return m.getPairs().stream()
                .filter(p -> p.getCrypto().equals(code))
                .map(CryptoPairModel::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
	
	//helpers
	private CryptoWalletDto toDto(CryptoWalletModel m) {
		List<CryptoPairDto> pairs = m.getPairs().stream()
				.map(p -> new CryptoPairDto(p.getCrypto(), p.getAmount()))
				.collect(Collectors.toList());
		return new CryptoWalletDto(pairs, m.getEmail());
	}
	
	private CryptoWalletModel saveFromDto(CryptoWalletModel model, CryptoWalletDto dto) {
		// brisemo stare parove i upisujemo nove
		model.getPairs().clear();
		
		if (dto.getPairs() != null) {
			for (CryptoPairDto p : dto.getPairs()) {
				String code = normalizeCrypto(p.getCrypto());
				BigDecimal amount = p.getAmount() ==  null ? BigDecimal.ZERO : p.getAmount();
				CryptoPairModel entity = new CryptoPairModel(code, amount);
				entity.setCryptoWallet(model);
				model.getPairs().add(entity);
			}
		}
		return walletRepo.save(model);
	}
	
	private String normalizeCrypto(String code) {
	
		try {
			return Crypto.from(code).code(); // koristi enum
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported crypto: " + code);
		}
	}
	
	private String extractEmailFromBasicAuth(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Basic "))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Basic Authorization header");

		String base64 = authorizationHeader.substring("Basic ".length());
		String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
		int idx = decoded.indexOf(':');
		if (idx <= 0) 
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Basic token");
        return decoded.substring(0, idx);
	}
}
