package com.eshop.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleRateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<SimpleRateLimiterGatewayFilterFactory.Config> {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_SECONDS = 60;
    private final Map<String, RequestWindow> requestCounts = new ConcurrentHashMap<>();

    public SimpleRateLimiterGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientKey = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";

            RequestWindow window = requestCounts.compute(clientKey, (k, v) -> {
                if (v == null || Instant.now().isAfter(v.resetAt)) {
                    return new RequestWindow(1, Instant.now().plusSeconds(WINDOW_SECONDS));
                }
                v.count++;
                return v;
            });

            if (window.count > MAX_REQUESTS) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                DataBuffer buffer = exchange.getResponse().bufferFactory()
                        .wrap("{\"error\":\"Too many requests\"}".getBytes());
                return exchange.getResponse().writeWith(Mono.just(buffer));
            }

            return chain.filter(exchange);
        };
    }

    private static class RequestWindow {
        int count;
        final Instant resetAt;

        RequestWindow(int count, Instant resetAt) {
            this.count = count;
            this.resetAt = resetAt;
        }
    }

    public static class Config {
    }
}
