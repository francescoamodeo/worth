package com.fram3.worth.server;

import com.fram3.worth.client.Client;
import com.fram3.worth.Worth.ResponseType;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Server è l'interfaccia remota usata dal client per la registrazione al servizio
 * e alle callbacks tramite RMI sull'istanza del server esportata
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public interface Server extends Remote {

    /**
     * registra l'utente al servizio con nickname e password forniti
     *
     * @param nickname nome utente da registrare
     * @param password password da associare all'utente
     * @return responso per l'operazione richiesta
     * @throws RemoteException -
     */
    ResponseType register(String nickname, String password) throws RemoteException;

    /**
     * registra il client per le callbacks
     *
     * @param clientStub stub/proxy corrispondente al riferimento remoto dell'oggetto client
     *                   utilizzato dal server per le callbacks
     * @throws RemoteException -
     */
    void registerForCallback(Client clientStub) throws RemoteException;

    /**
     * deregistra il client per le callbacks
     *
     * @param clientStub stub/proxy corrispondente al riferimento remoto dell'oggetto client
     *                   utilizzato dal server per le callbacks
     * @throws RemoteException -
     */
    void unregisterForCallback(Client clientStub) throws RemoteException;
    
}
