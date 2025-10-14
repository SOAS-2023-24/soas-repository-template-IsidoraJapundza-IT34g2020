package api.dto;

public class CryptoConversionResponseDto {

	private CryptoWalletDto wallet;
	private String result;
	
	public CryptoConversionResponseDto() {
		
	}
	
	public CryptoConversionResponseDto(CryptoWalletDto wallet, String result) {
		this.wallet = wallet;
		this.result = result;
	}

	public CryptoWalletDto getWallet() {
		return wallet;
	}

	public void setWallet(CryptoWalletDto wallet) {
		this.wallet = wallet;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}
