/*Fleet Procesor: Configuración de RabbitMQ, se encarga de la configuración de la topología de mensajes. */
package com.fleet.processor.config;

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
 * Topologia de RabbitMQ.
 *
 * Entrada (la llena el producer/puente Python): GPS_QUEUE, TEMP_ALERT_QUEUE, FUEL_QUEUE.
 * Salida hacia storage-service: por exchange.fleet con routing keys storage.gps / storage.alert.
 * Notificaciones: cola.notificaciones (cada alerta se reenvia aqui).
 */
@Configuration
public class RabbitMQConfig {

    public static final String GPS_QUEUE = "cola.gps.telemetria";
    public static final String TEMP_ALERT_QUEUE = "cola.alertas.temperatura";
    public static final String FUEL_QUEUE = "cola.combustible.nivel";
    public static final String NOTIFICATION_QUEUE = "cola.notificaciones";

    public static final String STORAGE_GPS_QUEUE = "cola.storage.gps";
    public static final String STORAGE_ALERT_QUEUE = "cola.storage.alertas";

    public static final String FLEET_EXCHANGE = "exchange.fleet";
    public static final String RK_STORAGE_GPS = "storage.gps";
    public static final String RK_STORAGE_ALERT = "storage.alert";

    /** Los eventos salen como JSON (content-type application/json). */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        // El receptor usa el tipo de su propio metodo, no el nombre de clase del emisor
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public DirectExchange fleetExchange() {
        return new DirectExchange(FLEET_EXCHANGE);
    }

    // ---- colas de entrada y notificaciones ----
    @Bean
    public Queue gpsQueue() {
        return new Queue(GPS_QUEUE, true);
    }

    @Bean
    public Queue tempAlertQueue() {
        return new Queue(TEMP_ALERT_QUEUE, true);
    }

    @Bean
    public Queue fuelQueue() {
        return new Queue(FUEL_QUEUE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    // ---- colas hacia storage-service (mismas declaraciones que en storage) ----
    @Bean
    public Queue storageGpsQueue() {
        return new Queue(STORAGE_GPS_QUEUE, true);
    }

    @Bean
    public Queue storageAlertQueue() {
        return new Queue(STORAGE_ALERT_QUEUE, true);
    }

    // ---- bindings de la guia (colas de entrada) ----
    @Bean
    public Binding gpsBinding(Queue gpsQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(gpsQueue).to(fleetExchange).with("gps.routing");
    }

    @Bean
    public Binding tempBinding(Queue tempAlertQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(tempAlertQueue).to(fleetExchange).with("temp.alert");
    }

    @Bean
    public Binding fuelBinding(Queue fuelQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(fuelQueue).to(fleetExchange).with("fuel.routing");
    }

    // ---- bindings hacia storage ----
    @Bean
    public Binding storageGpsBinding(Queue storageGpsQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(storageGpsQueue).to(fleetExchange).with(RK_STORAGE_GPS);
    }

    @Bean
    public Binding storageAlertBinding(Queue storageAlertQueue, DirectExchange fleetExchange) {
        return BindingBuilder.bind(storageAlertQueue).to(fleetExchange).with(RK_STORAGE_ALERT);
    }
}
