package com.fram3.worth;

import com.fram3.worth.client.ClientImpl;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * User modella l'utente che interagisce con il servizio
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public class User implements Serializable {
    private static final long serialVersionUID = -770658941014152791L;

    /** client che gestisce l'utente */
    private transient ClientImpl client;

    /** nome utente */
    private final String nickname;

    /** password utente */
    private final String password;

    /** status utente */
    private boolean online;

    /** lista utenti registrati al servizio (aggiornata tramite callbacks) */
    private ArrayList<User> users;

    /** lista chat dei progetti di cui fa parte l'utente */
    private ArrayList<Chat> chats;

    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
        this.online = false;
        this.users = new ArrayList<>();
        this.chats = new ArrayList<>();
    }

    /**
     *
     * @return nome utente
     */
    public String getNickname() {
        return this.nickname;
    }

    /**
     *
     * @return password utente
     */
    public String getPassword() {
        return this.password;
    }

    /**
     *
     * @return true se l'utente è online, false altrimenti
     */
    public boolean isOnline() {
        return this.online;
    }

    /**
     *
     * @return lista degli utenti registrati al servizio
     */
    public ArrayList<User> getUsers() {
        return users;
    }

    /**
     * costruisce la lista degli utenti online a partire dalla lista degli utenti registrati
     *
     * @return lista degli utenti online in quel momento
     */
    public ArrayList<User> getOnlineUsers() {
        ArrayList<User> onlineUsers = new ArrayList<>();
        for (User user : users) {
            if(user.isOnline())
                onlineUsers.add(user);
        }
        return onlineUsers;
    }

    /**
     *
     * @return lista di chat dei progetti di cui l'utente fa parte
     */
    public ArrayList<Chat> getChats() {
        return chats;
    }

    /**
     *
     * @param client client che gestisce l'utente
     */
    public void setClient(ClientImpl client) {
        this.client = client;
    }

    /**
     *
     * @param online booleano che indica lo stato con il quale si vuole impostare l'utente
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * metodo utilizzato nelle callbacks per aggiornare la lista di utenti locale dell'utente
     *
     * @param usersUpdate lista aggiornata dalla callback contenente gli utenti registrati al servizio
     */
    public void setUsersList(ArrayList<User> usersUpdate) {
        this.users = usersUpdate;
    }

    /**
     * metodo utilizzato nelle callbacks per aggiornare la lista di chats locale dell'utente
     *
     * @param chatsUpdate lista aggiornata dalla callback contenente le chats di cui fa parte l'utente
     */
    public void setChats(ArrayList<Chat> chatsUpdate) {
        //se una delle chat nella lista dell'utente non è
        //contenuta nella lista aggiornata dalla callback allora vuol
        //dire che il progetto relativo a quella chat è stato cancellato
        for (Chat chat : this.chats) {
            if (!chatsUpdate.contains(chat))
                client.interruptSniffer(chat);
        }
        ArrayList<Chat> updatedChats = new ArrayList<>();
        for (Chat chat : chatsUpdate) {
            int chatIndex = this.chats.indexOf(chat);
            //se è una chat di un progetto di cui l'utente era già membro allora lascio la chat originale
            //perchè non vogliamo perdere i messaggi sniffati e memorizzati nella chat fino a quel momento
            if (chatIndex != -1)
                updatedChats.add(this.chats.get(chatIndex));
                // altrimenti la aggiungo quella nuova e avvio lo sniffer dei messaggi
            else {
                updatedChats.add(chat);
                client.startSniffer(chat);
            }
        }
        //i progetti cancellati sono stati scartati dai due foreach
        this.chats = updatedChats;
    }

    /**
     * legge e stampa i messaggi della chat
     *
     * @param projectName nome progetto di cui si vuole leggere la chat
     */
    public void readChat(String projectName) {
        System.out.println(this.chats.get(this.chats.indexOf(new Chat(projectName))));
    }

    /**
     * invia un messaggio nella chat di progetto
     *
     * @param projectName nome progetto della chat in cui si vuole inviare il messaggio
     * @param message messaggio da inviare
     */
    public void sendChatMsg(String projectName, String message) {
        String chatMsg = this.nickname + " ha detto: " + "\"" + message + "\"";
        byte[] buf = chatMsg.getBytes(StandardCharsets.UTF_8);
        int chatIndex = this.chats.indexOf(new Chat(projectName));
        Chat chat = this.chats.get(chatIndex);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, chat.getAddress(), chat.getPort());
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param obj oggetto da confrontare
     * @return true se obj è uguale a this
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof User))
            return false;
        return this.nickname.equals(((User) obj).nickname);
    }

    /**
     * @return la stringa rappresentante l'utente
     */
    @Override
    public String toString() { 
        String state = (this.online ? "online" : "offline");
        return nickname + ": " + state;
    }
}
