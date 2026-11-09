package com.fleet.storage.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Solo declara lo que storage necesita (exchange + 2 colas + bindings). Las declaraciones
 * son identicas a las del processor, asi que el orden de arranque no importa.
 */
@Configuration
public class RabbitMQConfig {

    public static final String STORAGE_GPS_QUEUE = "cola.storage.gps";
    public static final String STORAGE_ALERT_QUEUE = "cola.storage.alertas";
    public static final String FLEET_EXCHANGE = "exchange.fleet";
    public static final String RK_STORAGE_GPS = "storage.gps";
    public static final String RK_STORAGE_ALERT = "storage.alert";

    /** Convierte el JSON entrante al tipo del parametro del @RabbitListener. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public DirectExchange fleetExchange() {
        return new DirectExchange(FLEET_EXCHANGE);
    }

    @Bean
    public Queue storageGpsQueue() {
        return new Queue(STORAGE_GPS_QUEUE, true);
    }

    @Bean
    public Queue storageAlertQueue() {
        return new Queue(STORAGE_ALERT_QUEUE, true);
    }

    @Bean
    public Binding storageGpsBinding(Queue storageGpsQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(storageGpsQueue).to(fleetExchange).with(RK_STORAGE_GPS);
    }

    @Bean
    public Binding storageAlertBinding(Queue storageAlertQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(storageAlertQueue).to(fleetExchange).with(RK_STORAGE_ALERT);
    }
}
