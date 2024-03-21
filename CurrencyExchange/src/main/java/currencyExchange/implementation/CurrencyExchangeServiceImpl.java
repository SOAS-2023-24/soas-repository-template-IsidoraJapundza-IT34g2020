package currencyExchange.implementation;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import api.dto.CurrencyExchangeDto;
import api.services.CurrencyExchangeService;
import currencyExchange.model.CurrencyExchangeModel;
import currencyExchange.repository.CurrencyExchangeRepository;

@RestController //implementira service koji sadrzi GetMapping, implementacija tih metoda ce se iyvrsiti kada se posalje yahev endpoint u intefesju
public class CurrencyExchangeServiceImpl implements CurrencyExchangeService {

	@Autowired // dependency injection
	private CurrencyExchangeRepository repo;
	
	@Override
	public ResponseEntity<?> getExchange(String from, String to) {
		//return convertFromModelToDto(repo.findByFromAndTo(from, to));
		//return new CurrencyExchangeDto("EUR", "RSD", BigDecimal.valueOf(117.5));
		CurrencyExchangeModel model = repo.findByFromAndTo(from, to);
		if (model == null) {
			return ResponseEntity.status(404).body("Requested exchange pair [" + from + " into " + to + "] could not be found");
		}
		return ResponseEntity.ok(convertFromModelToDto(model));
	}

	public CurrencyExchangeDto convertFromModelToDto(CurrencyExchangeModel model) {
		return new CurrencyExchangeDto(model.getFrom(), model.getTo(), model.getExchangeValue());//model sadrzi id, a dto ne sadrzi. Kod dto-a mozemo da odlucimo sta vracamo kao odgovor, a modl sve sto je u tabeli
	}
}