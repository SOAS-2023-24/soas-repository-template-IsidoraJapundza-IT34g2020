package currencyExchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import currencyExchange.model.CurrencyExchangeModel;

public interface CurrencyExchangeRepository extends JpaRepository<CurrencyExchangeModel, Integer>{
	// TE koji izvlaci tip primarnog kljuca
	CurrencyExchangeModel findByFromAndTo(String from, String to);
}

