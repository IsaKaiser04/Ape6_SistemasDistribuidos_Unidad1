package com.fleet.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API Gateway: unico punto de entrada para los clientes.
 *
 *   /api/fleet/**      -> storage-service    (consultas de telemetria y alertas)
 *   /api/processor/**  -> processor-service  (estadisticas del procesamiento)
 *   /gateway/health    -> estado de los servicios internos
 *
 * Los servicios internos conservan el mismo path, asi que el gateway solo reenvia.
 */
@RestController
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final String storageUrl;
    private final String processorUrl;
    private final Map<String, String> routes;
    private final RestClient client;

    public GatewayController(@Value("${gateway.storage-url}") String storageUrl,
                             @Value("${gateway.processor-url}") String processorUrl) {
        this.storageUrl = storageUrl;
        this.processorUrl = processorUrl;
        this.routes = Map.of("fleet", storageUrl, "processor", processorUrl);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @GetMapping("/api/{service}/**")
    public ResponseEntity<byte[]> forward(@PathVariable String service, HttpServletRequest request) {
        String base = routes.get(service);
        if (base == null) {
            return json(HttpStatus.NOT_FOUND, "{\"error\":\"ruta desconocida: " + service + "\"}");
        }
        String query = request.getQueryString();
        String target = base + request.getRequestURI() + (query != null ? "?" + query : "");
        log.info("GET {} -> {}", request.getRequestURI(), target);
        try {
            ResponseEntity<byte[]> resp = client.get().uri(URI.create(target))
                    .retrieve().toEntity(byte[].class);
            HttpHeaders headers = new HttpHeaders();
            MediaType type = resp.getHeaders().getContentType();
            if (type != null) {
                headers.setContentType(type);
            }
            return new ResponseEntity<>(resp.getBody(), headers, resp.getStatusCode());
        } catch (RestClientResponseException e) {
            // el servicio interno respondio 4xx/5xx: se propaga tal cual
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(e.getResponseBodyAsByteArray(), headers, e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Servicio no disponible ({}): {}", service, e.getMessage());
            return json(HttpStatus.SERVICE_UNAVAILABLE,
                    "{\"error\":\"servicio no disponible: " + service + "\"}");
        }
    }

    @GetMapping("/gateway/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gateway", "UP");
        result.put("storage-service", check(storageUrl + "/actuator/health"));
        result.put("processor-service", check(processorUrl + "/actuator/health"));
        result.put("timestamp", Instant.now().toString());
        return result;
    }

    private String check(String url) {
        try {
            client.get().uri(URI.create(url)).retrieve().toBodilessEntity();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private ResponseEntity<byte[]> json(HttpStatus status, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(body.getBytes(StandardCharsets.UTF_8), headers, status);
    }
}
