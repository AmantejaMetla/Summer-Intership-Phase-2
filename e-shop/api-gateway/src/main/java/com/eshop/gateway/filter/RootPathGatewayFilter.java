package com.eshop.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Serves /, /actuator/health, and /actuator/info so the gateway doesn't show Whitelabel 404.
 * (Gateway routing runs before Spring Actuator, so we handle these paths here.)
 */
@Component
public class RootPathGatewayFilter implements GlobalFilter, Ordered {

    private static final String ROOT_BODY =
        "E-Shop API Gateway is running.\n\n" +
        "Check gateway is up (no need to remember service ports):\n" +
        "  GET  /actuator/health  - Gateway status\n" +
        "  GET  /actuator/info    - Gateway info\n\n" +
        "API paths (all via this port):\n" +
        "  POST /api/auth/register   POST /api/auth/login   GET  /api/users/me\n" +
        "  GET  /api/products        POST /api/cart/items   GET  /api/orders/{id}\n" +
        "Eureka: http://localhost:8761";

    private static final String HEALTH_JSON = "{\"status\":\"UP\"}";
    private static final String INFO_JSON = "{\"gateway\":\"E-Shop API Gateway\",\"status\":\"running\"}";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
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
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
