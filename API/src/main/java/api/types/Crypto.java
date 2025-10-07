package api.types;

public enum Crypto {

	BTC, ETH, LTC;
	
	public static Crypto from(String v) {
		if (v == null) throw new IllegalArgumentException("null");
		return Crypto.valueOf(v.trim().toUpperCase());
	}
	
	public String code() {
		return name();
	}
}
