package ru.homework.provider;

import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.nodes.PersistentNode;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import org.springframework.context.event.ContextClosedEvent;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "discovery.enabled", havingValue = "true", matchIfMissing = true)
public class ServiceRegistration {
    @Value("${discovery.address}")
    private String zooKeeperAddress;
    @Value("${discovery.path}")
    private String path;
    @Value("${discovery.advertised-host}")
    private String host;

    private CuratorFramework zooKeeper;
    private PersistentNode registration;

    @EventListener
    public void register(WebServerInitializedEvent event) throws Exception {
        zooKeeper = CuratorFrameworkFactory.newClient(zooKeeperAddress,
                10000, 5000, new ExponentialBackoffRetry(500, 3));
        zooKeeper.start();
        if (!zooKeeper.blockUntilConnected(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Не удалось подключиться к ZooKeeper");
        }
        String address = "http://" + host + ":" + event.getWebServer().getPort() + "/rpc";
        registration = new PersistentNode(zooKeeper, CreateMode.EPHEMERAL_SEQUENTIAL,
                false, path + "/server-", address.getBytes(StandardCharsets.UTF_8));
        registration.start();
        if (!registration.waitForInitialCreate(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Не удалось зарегистрировать сервер");
        }
        System.out.println("Сервер зарегистрирован: " + address);
    }

    // ContextClosedEvent приходит до остановки HTTP-сервера.
    @EventListener(ContextClosedEvent.class)
    public void unregister() {
        try {
            if (registration != null) {
                registration.close();
                registration = null;
                LoggerFactory.getLogger(ServiceRegistration.class).info("Service removed from ZooKeeper; draining requests");
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(ServiceRegistration.class).warn("Cannot remove registration; closing session", e);
            if (zooKeeper != null) zooKeeper.close();
        }
    }

    @PreDestroy
    public void close() throws IOException {
        try {
            if (registration != null) registration.close();
        } finally {
            if (zooKeeper != null) zooKeeper.close();
        }
    }
}
