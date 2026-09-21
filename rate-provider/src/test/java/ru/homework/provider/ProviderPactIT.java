package ru.homework.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

@Provider("rate-provider")
@PactBroker(url = "${pactbroker.url}")
public class ProviderPactIT {
    private static ServletWebServerApplicationContext application;

    @BeforeAll
    static void startServer() {
        application = (ServletWebServerApplicationContext) new SpringApplicationBuilder(ProviderApplication.class)
                .run("--server.port=0", "--discovery.enabled=false");
    }

    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder contracts() {
        return new SelectorBuilder().branch(System.getProperty("pact.branch", "hw3"));
    }

    @BeforeEach
    void target(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", application.getWebServer().getPort()));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyContractFromBroker(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @AfterAll
    static void stopServer() {
        if (application != null) application.close();
    }
}
