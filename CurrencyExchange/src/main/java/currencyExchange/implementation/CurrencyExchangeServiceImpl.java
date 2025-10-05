package currencyExchange.implementation;

import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import api.dto.CurrencyExchangeDto;
import api.services.CurrencyExchangeService;
import currencyExchange.model.CurrencyExchangeModel;
import currencyExchange.repository.CurrencyExchangeRepository;
import util.exceptions.NoDataFoundException;

@RestController //implementira service koji sadrzi GetMapping, implementacija tih metoda ce se iyvrsiti kada se posalje yahev endpoint u intefesju
public class CurrencyExchangeServiceImpl implements CurrencyExchangeService {

	private final CurrencyExchangeRepository repo;  
	
	private final Environment environment;
	
	@Autowired
	public CurrencyExchangeServiceImpl(CurrencyExchangeRepository repo, Environment environment) {
		super();
		this.repo = repo;
		this.environment = environment;
	}

	@Override
	public ResponseEntity<CurrencyExchangeDto> getExchange(String from, String to) {
		if (!isSupportedFiatCurrency(from) || !isSupportedFiatCurrency(to)) {
			throw new NoDataFoundException("Fiat currency from request not found.");
        }
		
		CurrencyExchangeModel model = repo.findByFromAndTo(from, to);
		
		if (model == null) {
			return ResponseEntity.status(404).build(); //"Rate not found for %s -> %s".formatted(from, to);
		}
		return ResponseEntity.ok(convertFromModelToDto(model));
	}

	public CurrencyExchangeDto convertFromModelToDto(CurrencyExchangeModel model) {
		//model sadrzi id, a dto ne sadrzi. Kod dto-a mozemo da odlucimo sta vracamo kao odgovor, a modl sve sto je u tabeli
		CurrencyExchangeDto dto = 
				new CurrencyExchangeDto
				(model.getFrom(), model.getTo(), model.getExchangeValue());
		
		dto.setInstancePort(environment.getProperty("local.server.port"));
		return dto;
	}
	
	private boolean isSupportedFiatCurrency(String currency) {
        return "EUR".equals(currency) || 
               "USD".equals(currency) || 
               "RSD".equals(currency) || 
               "CHF".equals(currency) || 
               "GBP".equals(currency);
    }
}