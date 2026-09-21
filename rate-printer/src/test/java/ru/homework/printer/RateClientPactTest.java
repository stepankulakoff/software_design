package ru.homework.printer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@PactConsumerTest
@PactTestFor(providerName = "rate-provider", pactVersion = PactSpecVersion.V3)
public class RateClientPactTest {
    @Pact(consumer = "rate-printer", provider = "rate-provider")
    public RequestResponsePact rateContract(PactDslWithProvider builder) {
        return builder
                .uponReceiving("getRate returns a numeric USD/RUB rate")
                .path("/rpc").method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body("{\"jsonrpc\":\"2.0\",\"method\":\"getRate\",\"id\":1}")
                .willRespondWith().status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringValue("jsonrpc", "2.0")
                        .numberValue("id", 1)
                        .numberType("result", 89.42))
                .toPact();
    }

    @Test
    void realClientReadsRate(MockServer server) {
        // Используем тот же клиент, который вызывается приложением по расписанию.
        BigDecimal rate = new RateClient().getRate(server.getUrl() + "/rpc");
        assertThat(rate).isEqualByComparingTo("89.42");
    }
}
