package soas.cryptoWallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import soas.cryptoWallet.model.CryptoWalletModel;

public interface CryptoWalletRepository extends JpaRepository<CryptoWalletModel, Long>{

	boolean existsByEmail(String email);
	CryptoWalletModel findByEmail(String email);
}
