package ru.homework.printer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RatePrinter {
    private final ServiceDiscovery discovery;
    private final RateClient client;
    private int nextServer;

    public RatePrinter(ServiceDiscovery discovery, RateClient client) {
        this.discovery = discovery;
        this.client = client;
    }

    @Scheduled(fixedRate = 5000)
    public void printRate() {
        try {
            List<String> addresses = discovery.addresses();
            if (addresses.isEmpty()) {
                System.out.println("Нет доступных серверов. Повторим через 5 секунд.");
                return;
            }
            int index = Math.floorMod(nextServer++, addresses.size());
            String address = addresses.get(index);
            BigDecimal rate = client.getRate(address);
            System.out.printf("[%s] USD/RUB: %.2f%n", address, rate);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Не удалось получить курс: " + e.getMessage());
        }
    }
}
