package api.dto;

import java.math.BigDecimal;

public class CryptoExchangeDto {

	private String from;
	private String to;
	private BigDecimal exchangeValue;
	
	public CryptoExchangeDto() {
		
	}
	
	public CryptoExchangeDto(String from, String to, BigDecimal exchangeValue) {
		this.from = from;
		this.to = to;
		this.exchangeValue = exchangeValue;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public BigDecimal getExchangeValue() {
		return exchangeValue;
	}

	public void setExchangeValue(BigDecimal exchangeValue) {
		this.exchangeValue = exchangeValue;
	}	
}
