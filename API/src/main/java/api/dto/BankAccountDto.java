package api.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BankAccountDto {

	@Email
	@NotBlank
	private String email;
	
	@NotNull
	private List<FiatBalanceDto> fiatBalances;
	
	public BankAccountDto() {
		
	}
	
	public BankAccountDto(String email, List<FiatBalanceDto> fiatBalances) {
		super();
		this.email = email;
		this.fiatBalances = fiatBalances;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public List<FiatBalanceDto> getFiatBalances() {
		return fiatBalances;
	}

	public void setFiatBalances(List<FiatBalanceDto> fiatBalances) {
		this.fiatBalances = fiatBalances;
	}
}