package ru.homework.printer;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class RateClient {
    private static final Logger log = LoggerFactory.getLogger(RateClient.class);
    private final RestClient http;
    private final String clientId;

    public RateClient() {
        this(RestClient.builder(), "rate-printer");
    }

    @Autowired
    public RateClient(RestClient.Builder builder, @Value("${client.id}") String clientId) {
        this.clientId = clientId;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(3));
        http = builder.requestFactory(factory).build();
    }

    public BigDecimal getRate(String address) {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of("jsonrpc", "2.0", "method", "getRate", "id", 1);
        log.info("RPC request id={} client={} address={} body={}", requestId, clientId, address, body);
        JsonNode response;
        try {
            var entity = http.post().uri(address)
                    .header("X-Client-Id", clientId).header("X-Request-Id", requestId)
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().toEntity(JsonNode.class);
            response = entity.getBody();
            log.info("RPC response id={} status={} body={}", requestId, entity.getStatusCode().value(), response);
        } catch (RestClientResponseException e) {
            log.warn("RPC response id={} status={} body={}", requestId, e.getStatusCode().value(),
                    e.getResponseBodyAsString().replace('\n', ' ').replace('\r', ' '));
            throw e;
        } catch (RuntimeException e) {
            log.warn("RPC request failed id={} address={} error={}", requestId, address, e.toString());
            throw e;
        }
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
