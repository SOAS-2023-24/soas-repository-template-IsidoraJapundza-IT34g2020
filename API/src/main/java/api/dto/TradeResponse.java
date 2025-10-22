package api.dto;

public record TradeResponse(String message, BankAccountDto bank, CryptoWalletDto wallet) {

}
