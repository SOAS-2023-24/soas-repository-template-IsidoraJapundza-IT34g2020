package api.dto;

import java.math.BigDecimal;

public class CurrencyConversionDto {

	private CurrencyExchangeDto exchange;
	private BigDecimal quantity;
	private ConversionResult result;
	
	public CurrencyConversionDto() {
		
	}
	
	public CurrencyConversionDto(CurrencyExchangeDto exchange, BigDecimal quantity, BigDecimal result, String currencyTo) {
		this.exchange = exchange;
		this.quantity = quantity;
		CurrencyConversionDto.ConversionResult resultHolder = this.new ConversionResult(result, currencyTo);
		this.result = resultHolder;
	}
	
	
	
	public CurrencyExchangeDto getExchange() {
		return exchange;
	}

	public void setExchange(CurrencyExchangeDto exchange) {
		this.exchange = exchange;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public ConversionResult getResult() {
		return result;
	}

	public void setResult(ConversionResult result) {
		this.result = result;
	}



	private class ConversionResult{ // klasa u klasi jer izvan ove klase nema nikakav znacaj
		private BigDecimal result;
		private String currencyTo;
				
		public ConversionResult() {
			super();
		}

		public ConversionResult(BigDecimal result, String currencyTo) {
			super();
			this.result = result;
			this.currencyTo = currencyTo;
		}

		public BigDecimal getResult() {
			return result;
		}

		public void setResult(BigDecimal result) {
			this.result = result;
		}

		public String getCurrencyTo() {
			return currencyTo;
		}

		public void setCurrencyTo(String currencyTo) {
			this.currencyTo = currencyTo;
		}		
	}
	
}
