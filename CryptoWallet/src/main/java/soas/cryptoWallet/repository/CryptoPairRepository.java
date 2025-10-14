package soas.cryptoWallet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import soas.cryptoWallet.model.CryptoPairModel;

public interface CryptoPairRepository extends JpaRepository<CryptoPairModel, Long>{
	
	List<CryptoPairModel> findByCryptoWallet_WalletId(Long walletId);
}
