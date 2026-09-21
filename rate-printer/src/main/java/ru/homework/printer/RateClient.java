package ru.homework.printer;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class RateClient {
    private final RestClient http;

    public RateClient() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(3));
        http = RestClient.builder().requestFactory(factory).build();
    }

    public BigDecimal getRate(String address) {
        JsonNode response = http.post().uri(address)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("jsonrpc", "2.0", "method", "getRate", "id", 1))
                .retrieve().body(JsonNode.class);
        if (response == null || !"2.0".equals(response.path("jsonrpc").asText())
                || !response.path("id").isIntegralNumber() || response.path("id").asLong() != 1) {
            throw new IllegalStateException("Некорректный ответ JSON-RPC");
        }
        if (response.has("error")) {
            throw new IllegalStateException("Ошибка сервера: " + response.get("error"));
        }
        if (!response.path("result").isNumber()) {
            throw new IllegalStateException("Курс должен быть числом");
        }
        return response.get("result").decimalValue();
    }
}
