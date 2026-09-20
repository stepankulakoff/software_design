package ru.homework;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;

import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
    private static final String SERVICE_PATH = "/services/currency";

    private static CuratorFramework zooKeeper;
    private static final AtomicInteger nextServer = new AtomicInteger(0);
    public static void main(String[] args) throws InterruptedException {
        zooKeeper = CuratorFrameworkFactory.newClient(
                "localhost:2181",
                new ExponentialBackoffRetry(1000, 3)
        );
        zooKeeper.start();
        Runtime.getRuntime().addShutdownHook(new Thread(zooKeeper::close));

        if (!zooKeeper.blockUntilConnected(10, TimeUnit.SECONDS)) {
            zooKeeper.close();
            throw new IllegalStateException("Не удалось подключиться к ZooKeeper");
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(Client::printRate, 0, 5, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdownNow));
    }

    private static void printRate() {
        try {
            List<String> children = zooKeeper.getChildren().forPath(SERVICE_PATH);
            if (children.isEmpty()) {
                System.out.println("Нет доступных инстансов сервиса. Повторим через 5 секунд.");
                return;
            }
            Collections.sort(children);

            int index = Math.floorMod(nextServer.getAndIncrement(), children.size());
            String node = children.get(index);
            byte[] data = zooKeeper.getData().forPath(SERVICE_PATH + "/" + node);
            String address = new String(data, StandardCharsets.UTF_8);
            String[] hostPort = address.split(":");

            Registry registry = LocateRegistry.getRegistry(hostPort[0], Integer.parseInt(hostPort[1]));
            CurrencyRateService service = (CurrencyRateService)
                    registry.lookup("CurrencyRateService");

            double rate = service.getRate();
            System.out.printf("[%s] USD/RUB: %.2f%n", address, rate);
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Сервер недоступен или сервис не найден. Повторим через 5 секунд.");
        } catch (KeeperException.NoNodeException e) {
            System.out.println("Список серверов изменился или пока пуст. Повторим через 5 секунд.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Ошибка обращения к ZooKeeper: " + e.getMessage());
        }
    }
}
