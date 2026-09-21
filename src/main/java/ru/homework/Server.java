package ru.homework;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.nodes.PersistentNode;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;

public class Server {
    private static CurrencyRateProvider provider;
    private static Registry registry; // реестр сервиосв
    private static CuratorFramework zooKeeper;
    private static PersistentNode registration;
    
    public static void main(String[] args) throws RemoteException, InterruptedException
    {
        if (args.length != 2) {
            throw new IllegalArgumentException("Укажи два порта, например: Server 1099 1100");
        }
        int registryPort = Integer.parseInt(args[0]);
        int servicePort = Integer.parseInt(args[1]);
        if (registryPort < 1 || registryPort > 65535 || servicePort < 1
                || servicePort > 65535 || registryPort == servicePort) {
            throw new IllegalArgumentException("Нужны два разных порта от 1 до 65535");
        }
        System.setProperty("java.rmi.server.hostname", "localhost");
        zooKeeper = CuratorFrameworkFactory.newClient(
                "localhost:2181",
                new ExponentialBackoffRetry(1000, 3)
        );
        // зуупкиерр в докере
        zooKeeper.start();
        Runtime.getRuntime().addShutdownHook(new Thread(zooKeeper::close));

        if (!zooKeeper.blockUntilConnected(10, TimeUnit.SECONDS)) {
            zooKeeper.close();
            throw new IllegalStateException("Не удалось подключиться к ZooKeeper");
        }

        provider = new CurrencyRateProvider();
        CurrencyRateService stub = (CurrencyRateService)
        UnicastRemoteObject.exportObject(provider, servicePort);

        registry = LocateRegistry.createRegistry(registryPort);
        //реестр дотспных на порту 1099
        registry.rebind("CurrencyRateService", stub);

        String address = "localhost:" + registryPort;
        registration = new PersistentNode(
                zooKeeper,
                CreateMode.EPHEMERAL_SEQUENTIAL,
                false,
                "/services/currency/server-",
                address.getBytes(StandardCharsets.UTF_8)
        );
        registration.start();

        if (!registration.waitForInitialCreate(10, TimeUnit.SECONDS)) {
            zooKeeper.close();
            UnicastRemoteObject.unexportObject(registry, true);
            UnicastRemoteObject.unexportObject(provider, true);
            throw new IllegalStateException("Не удалось зарегистрировать сервер");
        }

        System.out.println("Сервер запущен. Порты: "
        + registryPort + ", " + servicePort);
    }
}
