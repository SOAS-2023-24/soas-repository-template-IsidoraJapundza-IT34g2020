package api.dto;

import java.math.BigDecimal;

public class CryptoPairDto {

	private String crypto;
	
	private BigDecimal amount;

	public CryptoPairDto() {

	}

	public CryptoPairDto(String crypto, BigDecimal amount) {
		super();
		this.crypto = crypto;
		this.amount = amount;
	}

	public String getCrypto() {
		return crypto;
	}

	public void setCrypto(String crypto) {
		this.crypto = crypto;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
}
