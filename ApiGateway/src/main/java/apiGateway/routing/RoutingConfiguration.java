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
			.route(p -> p.path("/currency-conversion")
					.filters(f -> f.rewritePath("/currency-conversion"
							, "/currency-conversion-feign")).uri("lb://currency-conversion"))
			.build();
	}
}