package api.types;

public enum Fiat {

	EUR, USD, GBP, CHF, RSD;
	
	public static Fiat from(String v) {
		return Fiat.valueOf(v.trim().toUpperCase());
	}
	
	public String code() {
		return name();
	}
}
