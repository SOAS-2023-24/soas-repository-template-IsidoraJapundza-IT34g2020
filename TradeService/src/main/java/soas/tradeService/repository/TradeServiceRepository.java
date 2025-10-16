package soas.tradeService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import soas.tradeService.model.TradeServiceModel;

public interface TradeServiceRepository extends JpaRepository<TradeServiceModel,Long>{

	TradeServiceModel findByFromAndTo(String from, String to);
}
