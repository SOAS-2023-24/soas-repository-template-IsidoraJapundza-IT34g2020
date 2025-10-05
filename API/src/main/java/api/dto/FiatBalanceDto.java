package api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class FiatBalanceDto {

	@NotNull
	private String currency;
	
	@NotNull
	@DecimalMin("0.00")
	private BigDecimal balance;
	
	public FiatBalanceDto() {
		
	}
	
	public FiatBalanceDto(String currency, BigDecimal balance) {
		this.currency = currency;
		this.balance = balance;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}
}