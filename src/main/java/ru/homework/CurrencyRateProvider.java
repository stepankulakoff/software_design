package ru.homework;

public class CurrencyRateProvider implements CurrencyRateService {
    @Override // реалзует
    public double getRate() {
        return 85 + Math.random() * 10;
    }
}