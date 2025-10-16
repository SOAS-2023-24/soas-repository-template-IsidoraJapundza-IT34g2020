package apiGateway.authentication;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import api.dto.UserRequestDto;

@Configuration
@EnableWebFluxSecurity
public class ApiGatewayAuthentication {
	
	@Bean
	SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
		http.
		csrf(csrf -> csrf.disable())
		.authorizeExchange(exchange -> exchange.pathMatchers("/currency-exchange").permitAll()
				.pathMatchers("/currency-conversion").hasRole("USER")
				.pathMatchers("/currency-conversion-feign").hasRole("USER")
				.pathMatchers("/users/**").hasAnyRole("ADMIN", "OWNER")
				.pathMatchers("/bank-accounts/**").hasAnyRole("ADMIN")
				.pathMatchers("/bank-account/user").hasRole("USER")
				.pathMatchers("/crypto-wallet/user").hasRole("USER")
			    .pathMatchers("/crypto-wallet/**").hasAnyRole("ADMIN")
				.pathMatchers("/crypto-exchange", "/crypto-exchange/**").permitAll()
				.pathMatchers("/crypto-conversion").hasRole("USER")
				.pathMatchers("/trade-service").hasRole("USER")
				.pathMatchers(HttpMethod.POST).hasRole("ADMIN"))
				.httpBasic(Customizer.withDefaults());

		return http.build();
	}
	
	@Bean
	MapReactiveUserDetailsService userDetailsService(BCryptPasswordEncoder encoder) {
		// Obratiti paznju na URL prilikom rada sa Dockerom
		ResponseEntity<List<UserRequestDto>> response =
				new RestTemplate().exchange("http://localhost:8770/users/auth-list", HttpMethod.GET,
						null, new ParameterizedTypeReference<List<UserRequestDto>>() {});
		List<UserDetails> users = new ArrayList<UserDetails>();
		for(UserRequestDto user : response.getBody()) {
			users.add(
					User.withUsername(user.getEmail())
					.password(encoder.encode(user.getPassword()))
					.roles(user.getRole())
					.build());
		}
		
		return new MapReactiveUserDetailsService(users);	
	}
	
	@Bean
	BCryptPasswordEncoder getEncoder() {
		return new BCryptPasswordEncoder();
	}
}
