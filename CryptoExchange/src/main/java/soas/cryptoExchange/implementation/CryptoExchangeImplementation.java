package soas.cryptoExchange.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import api.dto.CryptoExchangeDto;
import api.services.CryptoExchangeService;
import api.types.Crypto;
import soas.cryptoExchange.model.CryptoExchangeModel;
import soas.cryptoExchange.repository.CryptoExchangeRepository;
import util.exceptions.NoDataFoundException;

@RestController
public class CryptoExchangeImplementation implements CryptoExchangeService {

	@Autowired
	private CryptoExchangeRepository repo;

	@Override
	public ResponseEntity<CryptoExchangeDto> cryptoExchange(String from, String to) {
		
		String f = normalizeCrypto(from);
		String t = normalizeCrypto(to);
		
		if (f.equals(t)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' and 'to' must differ");
		}
		
		CryptoExchangeModel model = repo.findByFromAndTo(f, t);
		
		if (model == null) {
			throw new NoDataFoundException("Crypto exchange rate not found for: " + f + " to " + t);
			// return ResponseEntity.status(404).body(null);
		}
		
		return ResponseEntity.ok(new CryptoExchangeDto(f, t, model.getExchangeValue()));
	}
	
	// helper
	private String normalizeCrypto(String code) {
		if (code == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'from'/'to'");
	    }
		
        try {
            return Crypto.from(code.trim().toUpperCase()).code();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported crypto: " + code);
        }
    }
	
	public CryptoExchangeDto convertModelToDto(CryptoExchangeModel model) {
		CryptoExchangeDto dto = new CryptoExchangeDto(model.getFrom(), model.getTo(), model.getExchangeValue());
		return dto;
	}
	
	
}
