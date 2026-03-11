package com.eshop.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Filter that writes a static response and does not forward. Used for /, /actuator/health, /actuator/info
 * so these routes don't 404 (gateway only runs filters when a route matches, so we need routes + this filter).
 */
@Component
public class StaticResponseGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final String ROOT_BODY =
        "E-Shop API Gateway is running.\n\n" +
        "  GET /test             - test page (check all services from here)\n" +
        "  GET /actuator/health  - status\n  GET /actuator/info  - info\n" +
        "  API: /api/auth/register, /api/auth/login, /api/users/me, /api/products, ...\n" +
        "Eureka: http://localhost:8761";

    private static final String HEALTH_JSON = "{\"status\":\"UP\"}";
    private static final String INFO_JSON = "{\"gateway\":\"E-Shop API Gateway\",\"status\":\"running\"}";

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String body;
            MediaType contentType;

            if ("/".equals(path)) {
                body = ROOT_BODY;
                contentType = MediaType.TEXT_PLAIN;
            } else if ("/actuator/health".equals(path)) {
                body = HEALTH_JSON;
                contentType = MediaType.APPLICATION_JSON;
            } else if ("/actuator/info".equals(path)) {
                body = INFO_JSON;
                contentType = MediaType.APPLICATION_JSON;
            } else {
                return chain.filter(exchange);
            }

            exchange.getResponse().setStatusCode(HttpStatus.OK);
            exchange.getResponse().getHeaders().setContentType(contentType);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        };
    }
}
