package soas.bankaccount.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import soas.bankaccount.model.BankAccountModel;

public interface BankAccountRepository extends JpaRepository<BankAccountModel, Long>{
	BankAccountModel findByEmail(String email);
	
	boolean existsByEmail(String email);
	int deleteByEmail(String email);
}
