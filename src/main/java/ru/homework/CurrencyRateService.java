package ru.homework;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CurrencyRateService extends Remote {
    double getRate() throws RemoteException;
}