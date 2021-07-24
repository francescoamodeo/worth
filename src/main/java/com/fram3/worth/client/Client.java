package com.fram3.worth.client;

import com.fram3.worth.Project;
import com.fram3.worth.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Client è l'interfaccia remota usata dal server per effettuare le callbacks.
 * Tramite RMI sull'istanza del client esportata, il server notifica il client
 * in seguito a cambiamenti di stato riguardanti utenti e progetti
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public interface Client extends Remote {

    /**
     * notifica il client in seguito ad un cambiamento di stato degli utenti registrati.
     * Il server invoca il metodo sullo stub del client, ricevuto nel momento della registrazione alle
     * callbacks, passando come parametro la lista aggiornata degli utenti registrati
     *
     * @param registeredUsers lista degli utenti registrati al servizio aggiornata
     * @throws RemoteException -
     */
    void notifyUserEvent(ArrayList<User> registeredUsers) throws RemoteException;

    /**
     * notifica il client in seguito ad un cambiamento di stato dei progetti.
     * Il server invoca il metodo sullo stub del client, ricevuto nel momento della registrazione alle
     * callbacks, passando come parametro la lista aggiornata dei progetti
     *
     * @param createdProjects lista dei progetti creati nel servizio aggiornata
     * @throws RemoteException -
     */
    void notifyChatsEvent(ArrayList<Project> createdProjects) throws RemoteException;
}
