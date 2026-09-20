package ru.homework;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private static CurrencyRateProvider provider;
    private static Registry registry; // реестр сервиосв
    
    public static void main(String[] args) throws RemoteException
    {
        provider = new CurrencyRateProvider();
        CurrencyRateService stub = (CurrencyRateService)
        UnicastRemoteObject.exportObject(provider, 1100);

        registry = LocateRegistry.createRegistry(1099);
        //реестр дотспных на порту 1099
        registry.rebind("CurrencyRateService", stub);

        System.out.println("Сервер запущен");
    }
}

