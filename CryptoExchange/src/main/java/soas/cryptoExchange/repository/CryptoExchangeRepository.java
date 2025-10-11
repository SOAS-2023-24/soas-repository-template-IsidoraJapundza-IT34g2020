package soas.cryptoExchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import soas.cryptoExchange.model.CryptoExchangeModel;

public interface CryptoExchangeRepository extends JpaRepository<CryptoExchangeModel, Integer>{

	CryptoExchangeModel findByFromAndTo(String from, String to);
}
