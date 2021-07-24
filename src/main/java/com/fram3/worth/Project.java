package com.fram3.worth;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Project modella un progetto del servizio
 * 
 * @author Francesco Amodeo
 * @version 1.0
 */
public class Project implements Serializable {
    private static final long serialVersionUID = -368385639859655551L;
    
    /** nome progetto */
    private final String name;
    
    /** lista di cards nello stato di TODO */
    private final ArrayList<Card> toDo;

    /** lista di cards nello stato di INPROGRESS */
    private final ArrayList<Card> inProgress;

    /** lista di cards nello stato di TOBEREVISED */
    private final ArrayList<Card> toBeRevised;

    /** lista di cards nello stato di DONE */
    private final ArrayList<Card> done;
    
    /** lista di tutte le cards del progetto */
    private final ArrayList<Card> cards;
    
    /** lista di tutti i membri del progetto */
    private final ArrayList<String> members;
    
    /** indirizzo multicast della chat di progetto */
    private InetAddress chatAddress;
    
    /** porta del servizio multicast per la chat di progetto */
    private int chatPort;

    public Project(String name) {
        this.name = name;
        this.toDo = new ArrayList<>();
        this.inProgress = new ArrayList<>();
        this.toBeRevised = new ArrayList<>();
        this.done = new ArrayList<>();
        this.cards = new ArrayList<>();
        this.members = new ArrayList<>();
    }

    /**
     * 
     * @param name nome progetto
     * @param nickFirstMember utente che crea il progetto
     */
    public Project(String name, String nickFirstMember) {
        this.name = name;
        this.toDo = new ArrayList<>();
        this.inProgress = new ArrayList<>();
        this.toBeRevised = new ArrayList<>();
        this.done = new ArrayList<>();
        this.cards = new ArrayList<>();
        this.members = new ArrayList<>(); 
        this.members.add(nickFirstMember);
    }



    /**
     * 
     * @return nome progetto
     */
    public String getName() {
        return this.name;
    }

    /**
     * 
     * @return lista di membri del progetto
     */
    public ArrayList<String> getMembers() {
        return members;
    }

    /**
     * 
     * @return lista di cards del progetto
     */
    public ArrayList<Card> getCards() {
        return cards;
    }

    /**
     *
     * @return lista TODO
     */
    public ArrayList<Card> getToDo() {
        return toDo;
    }

    /**
     *
     * @return lista INPROGRESS
     */
    public ArrayList<Card> getInProgress() {
        return inProgress;
    }

    /**
     *
     * @return lista TOBEREVISED
     */
    public ArrayList<Card> getToBeRevised() {
        return toBeRevised;
    }

    /**
     *
     * @return lista DONE
     */
    public ArrayList<Card> getDone() {
        return done;
    }

    /**
     *
     * @return indirizzo della chat di progetto
     */
    public InetAddress getChatAddress() {
        return chatAddress;
    }

    /**
     *
     * @return porta del servizio di multicast della chat di progetto
     */
    public int getChatPort() {
        return chatPort;
    }

    /**
     *
     * @param chatAddress indirizzo della chat di progetto
     */
    public void setChatAddress(InetAddress chatAddress) {
        this.chatAddress = chatAddress;
    }

    /**
     *
     * @param chatPort porta del servizio di multicast della chat di progetto
     */
    public void setChatPort(int chatPort) {
        this.chatPort = chatPort;
    }

    /**
     * effettua il parsing del nome della lista ritornando la lista effettiva
     * @param list nome della lista da parsare
     * @return lista richiesta
     */
    public ArrayList<Card> parseList(String list) {
        ArrayList<Card> parsedList;
        switch (list.toUpperCase()) {
            case "TODO" : parsedList = this.getToDo(); break;
            case "INPROGRESS" : parsedList = this.getInProgress(); break;
            case "TOBEREVISED" : parsedList = this.getToBeRevised(); break;
            case "DONE" : parsedList = this.getDone(); break;
            default : parsedList = null;
        }
        return parsedList;
    }

    /**
     *
     * @return stringa che rappresenta il progetto (tramite il nome)
     */
    @Override
    public String toString() {
        return this.name;
    }

    /**
     *
     * @param obj oggetto da confrontare con this
     * @return true se i due oggetti sono uguali, false altrimenti
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Project))
            return false;
        return this.name.equals(((Project) obj).getName());
    }

}
