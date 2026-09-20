package ru.homework;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(Client::printRate, 0, 5, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdownNow));
    }

    private static void printRate() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            CurrencyRateService service = (CurrencyRateService)
                    registry.lookup("CurrencyRateService");

            double rate = service.getRate();
            System.out.printf("USD/RUB: %.2f%n", rate);
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Сервер недоступен или сервис не найден. Повторим через 5 секунд.");
        }
    }
}
