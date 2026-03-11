package com.eshop.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.otp.rabbit.enabled", havingValue = "true")
public class OtpRabbitConfig {

    @Bean
    public Queue otpQueue(@Value("${app.otp.rabbit.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public DirectExchange otpExchange(@Value("${app.otp.rabbit.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Binding otpBinding(
            Queue otpQueue,
            DirectExchange otpExchange,
            @Value("${app.otp.rabbit.routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(otpQueue).to(otpExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter otpJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
