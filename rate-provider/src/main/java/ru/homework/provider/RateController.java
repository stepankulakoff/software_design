package ru.homework.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateController {
    private final CurrencyRateProvider provider;
    private final ObjectMapper mapper;

    public RateController(CurrencyRateProvider provider, ObjectMapper mapper) {
        this.provider = provider;
        this.mapper = mapper;
    }

    @PostMapping(value = "/rpc", consumes = "application/json", produces = "application/json")
    public ResponseEntity<JsonNode> rpc(@RequestBody(required = false) String body) {
        JsonNode request;
        if (body == null || body.isBlank()) {
            return ResponseEntity.ok(error(NullNode.instance, -32700, "Parse error"));
        }
        try {
            request = mapper.readerFor(JsonNode.class)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(body);
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(error(NullNode.instance, -32700, "Parse error"));
        }
        JsonNode response;
        if (request != null && request.isArray()) {
            if (request.isEmpty()) {
                response = error(NullNode.instance, -32600, "Invalid Request");
            } else {
                ArrayNode batch = mapper.createArrayNode();
                for (JsonNode item : request) {
                    JsonNode result = call(item);
                    if (result != null) batch.add(result);
                }
                response = batch.isEmpty() ? null : batch;
            }
        } else {
            response = call(request);
        }
        // JSON-RPC уведомления (без id) не требуют ответа.
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    private JsonNode call(JsonNode request) {
        if (request == null || !request.isObject()
                || !request.path("jsonrpc").isTextual()
                || !"2.0".equals(request.path("jsonrpc").asText())
                || !request.path("method").isTextual()) {
            return error(NullNode.instance, -32600, "Invalid Request");
        }
        JsonNode id = request.has("id") ? request.get("id") : NullNode.instance;
        if (!id.isNull() && !id.isTextual() && !id.isNumber()) {
            return error(NullNode.instance, -32600, "Invalid Request");
        }
        JsonNode params = request.get("params");
        if (params != null && !params.isObject() && !params.isArray()) {
            return error(NullNode.instance, -32600, "Invalid Request");
        }
        boolean notification = !request.has("id");
        if (!"getRate".equals(request.get("method").asText())) {
            return notification ? null : error(id, -32601, "Method not found");
        }
        if (params != null && !params.isEmpty()) {
            return notification ? null : error(id, -32602, "Invalid params");
        }
        ObjectNode result = mapper.createObjectNode();
        result.put("jsonrpc", "2.0");
        result.set("id", id);
        result.put("result", provider.getRate());
        return notification ? null : result;
    }

    private ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.putObject("error").put("code", code).put("message", message);
        return response;
    }
}
