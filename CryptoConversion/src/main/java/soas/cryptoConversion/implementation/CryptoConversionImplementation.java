package soas.cryptoConversion.implementation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RestController;

import api.dto.CryptoConversionResponseDto;
import api.dto.CryptoPairDto;
import api.dto.CryptoWalletDto;
import api.feignProxies.CryptoExchangeProxy;
import api.feignProxies.CryptoWalletProxy;
import api.services.CryptoConversionService;

@RestController
public class CryptoConversionImplementation implements CryptoConversionService{

	private final CryptoWalletProxy walletProxy;
	private final CryptoExchangeProxy exchangeProxy;
	
	public CryptoConversionImplementation(CryptoWalletProxy walletProxy, CryptoExchangeProxy exchangeProxy) {
		this.walletProxy = walletProxy;
		this.exchangeProxy = exchangeProxy;
	}

	@Override
	public ResponseEntity<CryptoConversionResponseDto> getConversion(String from, String to, BigDecimal quantity,
			String authorizationHeader) {
		
		String fromN = normalizeCrypto(from);
	    String toN   = normalizeCrypto(to); 
	    
		//if (from == null || to == null || from.equalsIgnoreCase(to))
	    if (fromN.equals(toN)) 
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " 'from' and 'to' must be different");
	
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be > 0");

		CryptoWalletDto wallet = walletProxy.getUsersWallet(authorizationHeader);
        if (wallet == null) 
        	throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found for user");

        BigDecimal fromBal = getBalance(wallet, fromN);
        if (fromBal.compareTo(quantity) < 0)
        	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds for " + fromN);
	
        var rateResp = exchangeProxy.cryptoExchange(fromN, toN); 
        if (rateResp == null || !rateResp.getStatusCode().is2xxSuccessful() || rateResp.getBody() == null) {
            if (rateResp != null && rateResp.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No rate for " + fromN + " -> " + toN);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Exchange rate unavailable");
        }
        
        BigDecimal rate;
        //var rateResp = exchangeProxy.cryptoExchange(from, to);
        Object body = rateResp.getBody();
        //if (body == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,  "Exchange rate unavailable");
	
        if (body instanceof BigDecimal bd) {
        	rate = bd;
        } else {
        	try {
        		rate = (BigDecimal) body.getClass().getMethod("getExchangeValue").invoke(body);
        	} catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Exchange DTO incompatible");
        	}
        }
        
        BigDecimal received = quantity.multiply(rate).setScale(8, RoundingMode.HALF_UP);
        
        updateBalance(wallet, fromN, fromBal.subtract(quantity));
        BigDecimal toBal = getBalance(wallet, toN);
        updateBalance(wallet, toN, toBal.add(received));
        
        walletProxy.updateWallet(wallet.getEmail(), wallet, authorizationHeader);
	
        String msg = String.format("Uspešno je izvršena razmena %s: %s za %s: %s (kurs=%s)",
                fromN, strip(quantity), toN, strip(received), strip(rate));

        return ResponseEntity.ok(new CryptoConversionResponseDto(wallet, msg));
	}
	
	//helpers
	private BigDecimal getBalance(CryptoWalletDto w, String crypto) {
		if (w.getPairs() == null) return BigDecimal.ZERO;
		return w.getPairs().stream()
				.filter(p -> crypto.equalsIgnoreCase(p.getCrypto()))
	            .map(CryptoPairDto::getAmount)
	            .findFirst().orElse(BigDecimal.ZERO);
	}
	
	private void updateBalance(CryptoWalletDto w, String crypto, BigDecimal value) {
		if (w.getPairs() == null) w.setPairs(new ArrayList<>());
		for (CryptoPairDto p : w.getPairs()) {
			if (crypto.equalsIgnoreCase(p.getCrypto())) {
				p.setAmount(value);
				return;
			}
		}
		w.getPairs().add(new CryptoPairDto(crypto, value));
	}
	
	private String strip(BigDecimal v) {
		return v.stripTrailingZeros().toPlainString();
	}
	
	private String normalizeCrypto(String code) {
	    if (code == null) {
	        throw new org.springframework.web.server.ResponseStatusException(
	            org.springframework.http.HttpStatus.BAD_REQUEST, "Missing 'from'/'to'");
	    }
	    try {
	        return api.types.Crypto.from(code.trim().toUpperCase()).code();
	    } catch (IllegalArgumentException e) {
	        throw new org.springframework.web.server.ResponseStatusException(
	            org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported crypto: " + code);
	    }
	}
}
