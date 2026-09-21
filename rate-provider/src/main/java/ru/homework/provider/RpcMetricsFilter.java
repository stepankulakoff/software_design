package ru.homework.provider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RpcMetricsFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RpcMetricsFilter.class);
    private final MeterRegistry meters;
    private final Set<String> clients;

    public RpcMetricsFilter(MeterRegistry meters, @Value("${metrics.clients}") String clients) {
        this.meters = meters;
        this.clients = Arrays.stream(clients.split(",")).map(String::trim).collect(Collectors.toSet());
        meters.counter("currency.rpc.server.errors", "status", "500");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/rpc".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        String suppliedClient = request.getHeader("X-Client-Id");
        String client = suppliedClient != null && clients.contains(suppliedClient) ? suppliedClient : "other";
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || !requestId.matches("[a-zA-Z0-9-]{1,64}")) requestId = UUID.randomUUID().toString();
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, 8192);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        cachedResponse.setHeader("X-Request-Id", requestId);
        Timer.Sample sample = Timer.start(meters);
        boolean failed = false;
        log.info("RPC request id={} client={} method={} path=/rpc", requestId, client, request.getMethod());
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } catch (ServletException | IOException | RuntimeException e) {
            failed = true;
            log.error("RPC failure id={} client={}", requestId, client, e);
            throw e;
        } finally {
            int status = failed ? 500 : cachedResponse.getStatus();
            sample.stop(Timer.builder("currency.rpc.server.requests")
                    .description("HTTP requests to /rpc, including failures")
                    .tags("client", client, "status", Integer.toString(status))
                    .publishPercentileHistogram()
                    .minimumExpectedValue(Duration.ofNanos(1000))
                    .maximumExpectedValue(Duration.ofSeconds(10))
                    .register(meters));
            if (status == 500) meters.counter("currency.rpc.server.errors", "status", "500").increment();
            log.info("RPC request body id={} body={}", requestId, text(cachedRequest.getContentAsByteArray()));
            log.info("RPC response id={} client={} status={} body={}", requestId, client, status,
                    text(cachedResponse.getContentAsByteArray()));
            cachedResponse.copyBodyToResponse();
        }
    }

    private String text(byte[] bytes) {
        return new String(bytes, 0, Math.min(bytes.length, 8192), StandardCharsets.UTF_8)
                .replace('\n', ' ').replace('\r', ' ');
    }
}
