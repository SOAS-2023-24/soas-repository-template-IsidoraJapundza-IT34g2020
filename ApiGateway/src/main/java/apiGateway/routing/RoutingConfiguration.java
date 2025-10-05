package apiGateway.routing;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfiguration {

	@Bean
	RouteLocator gatewayRouter(RouteLocatorBuilder builder) {
		return builder.routes()
			.route(p -> p.path("/currency-exchange").uri("lb://currency-exchange"))
			.route(p -> p.path("/currency-conversion-feign").uri("lb://currency-conversion"))
			.route(p -> p.path("/currency-conversion/**").filters
					(f -> f.rewritePath("/currency-conversion/(?<segment>.*)", "/currency-conversion-feign/${segment}"))
					.uri("lb://currency-conversion"))
			.route(p -> p.path("/users/**").uri("lb://users-service"))
			.route(p -> p.path("/bank-accounts/**").uri("lb://bank-account"))
			.route(p -> p.path("/bank-account/user").uri("lb://bank-account"))
			.build();
	}
}