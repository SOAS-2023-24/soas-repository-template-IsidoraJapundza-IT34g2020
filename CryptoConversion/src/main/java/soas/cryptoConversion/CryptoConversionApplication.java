package soas.cryptoConversion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {"api.feignProxies"})
public class CryptoConversionApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoConversionApplication.class, args);
	}
}
