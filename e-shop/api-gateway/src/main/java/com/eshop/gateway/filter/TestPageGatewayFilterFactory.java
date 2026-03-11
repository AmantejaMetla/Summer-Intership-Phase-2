package com.eshop.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Serves the test page at /test so you can verify all services from the gateway's own page.
 */
@Component
public class TestPageGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final byte[] testPageHtml;

    public TestPageGatewayFilterFactory() {
        try {
            testPageHtml = new ClassPathResource("static/test.html").getContentAsString(StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Could not load test.html", e);
        }
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_HTML);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(testPageHtml);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        };
    }
}
