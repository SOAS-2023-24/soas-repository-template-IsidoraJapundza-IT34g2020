package api.dto;

import java.util.List;

public class CryptoWalletDto {

	private List<CryptoPairDto> pairs;
	private String email;
	
	public CryptoWalletDto() {
		
	}
	
	public CryptoWalletDto(List<CryptoPairDto> pairs, String email) {
		super();
		this.pairs = pairs;
		this.email = email;
	}

	public List<CryptoPairDto> getPairs() {
		return pairs;
	}

	public void setPairs(List<CryptoPairDto> pairs) {
		this.pairs = pairs;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}	
}
