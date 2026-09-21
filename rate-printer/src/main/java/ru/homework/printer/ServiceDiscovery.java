package ru.homework.printer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ServiceDiscovery {
    private final CuratorFramework zooKeeper;
    private final String path;

    public ServiceDiscovery(@Value("${discovery.address}") String address,
                            @Value("${discovery.path}") String path) {
        this.path = path;
        zooKeeper = CuratorFrameworkFactory.newClient(address,
                10000, 5000, new ExponentialBackoffRetry(500, 3));
    }

    @PostConstruct
    public void connect() throws InterruptedException {
        zooKeeper.start();
        if (!zooKeeper.blockUntilConnected(10, TimeUnit.SECONDS)) {
            zooKeeper.close();
            throw new IllegalStateException("Не удалось подключиться к ZooKeeper");
        }
    }

    public List<String> addresses() throws Exception {
        List<String> nodes;
        try {
            nodes = zooKeeper.getChildren().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            return List.of();
        }
        Collections.sort(nodes);
        List<String> addresses = new ArrayList<>();
        for (String node : nodes) {
            try {
                byte[] data = zooKeeper.getData().forPath(path + "/" + node);
                addresses.add(new String(data, StandardCharsets.UTF_8));
            } catch (KeeperException.NoNodeException e) {
                // Сервер мог остановиться между чтением списка и его адреса.
            }
        }
        return addresses;
    }

    @PreDestroy
    public void close() {
        zooKeeper.close();
    }
}
