package ru.homework.provider;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CurrencyRateProvider {
    public BigDecimal getRate() {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(8500, 9500), 2);
    }
}
