package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.BindingAdminRequestDTO;
import com.pedidos360.bff.dto.ExchangeAdminRequestDTO;
import com.pedidos360.bff.dto.QueueAdminRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RabbitAdminClient {

    private final RestTemplate restTemplate;
    private final String rabbitAdminUrl;

    public RabbitAdminClient(@Value("${clients.rabbit-admin-url:http://localhost:8083}") String rabbitAdminUrl) {
        this.restTemplate = new RestTemplate();
        this.rabbitAdminUrl = rabbitAdminUrl;
    }

    public ResponseEntity<Map<String, String>> createQueue(QueueAdminRequestDTO dto) {
        return restTemplate.postForEntity(
                rabbitAdminUrl + "/api/rabbit-admin/queues",
                dto,
                (Class<Map<String, String>>) (Class<?>) Map.class
        );
    }

    public ResponseEntity<Map<String, String>> deleteQueue(String name) {
        // {name} se expande como variable de URI para codificar correctamente caracteres especiales
        return restTemplate.exchange(
                rabbitAdminUrl + "/api/rabbit-admin/queues/{name}",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, String>>() {},
                name
        );
    }

    public ResponseEntity<Map<String, String>> createExchange(ExchangeAdminRequestDTO dto) {
        return restTemplate.postForEntity(
                rabbitAdminUrl + "/api/rabbit-admin/exchanges",
                dto,
                (Class<Map<String, String>>) (Class<?>) Map.class
        );
    }

    public ResponseEntity<Map<String, String>> deleteExchange(String name) {
        return restTemplate.exchange(
                rabbitAdminUrl + "/api/rabbit-admin/exchanges/{name}",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, String>>() {},
                name
        );
    }

    public ResponseEntity<Map<String, String>> createBinding(BindingAdminRequestDTO dto) {
        return restTemplate.postForEntity(
                rabbitAdminUrl + "/api/rabbit-admin/bindings",
                dto,
                (Class<Map<String, String>>) (Class<?>) Map.class
        );
    }

    public ResponseEntity<Map<String, String>> deleteBinding(String queueName, String exchangeName, String routingKey) {
        return restTemplate.exchange(
                rabbitAdminUrl + "/api/rabbit-admin/bindings?queueName={q}&exchangeName={e}&routingKey={r}",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<Map<String, String>>() {},
                queueName, exchangeName, routingKey
        );
    }
}
