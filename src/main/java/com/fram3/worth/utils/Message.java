package com.fram3.worth.utils;

import com.fram3.worth.User;
import com.fram3.worth.Card;
import com.fram3.worth.Project;
import com.fram3.worth.Worth.RequestType;
import com.fram3.worth.Worth.ResponseType;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Message modella i messaggi scambiati tra client e server
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 3935146958034001638L;

    /**
     * campi che si possono inserire nel messaggio
     */
    private RequestType request;
    private ResponseType response;
    private User user;
    private ArrayList<Project> projects;
    private ArrayList<String> members;
    private ArrayList<Card> cards;
    private Card card;
    private String nickname; 
    private String password;
    private String projectName;
    private String newMember;
    private String cardName;
    private String description;
    private String sourceList;
    private String destList;

    //tutti i campi sono settati in seguito alla creazione del messaggio
    //utilizzato dal server
    public Message() {}

    //utilizzato dal client che inserisce direttamente la richiesta
    //come parametro del costruttore
    public Message(RequestType request) {
        this.request = request;
    }

    /**
     *
     * @return request fatta dal client
     */
    public RequestType getRequest() {
        return this.request;
    }

    /**
     *
     * @return  response del server
     */
    public ResponseType getResponse(){
        return this.response;
    }

    /**
     *
     * @param response  risposta del server per l'operazione richiesta dal client
     */
    public void setResponse(ResponseType response){
        this.response = response;
    }

    /**
     *
     * @return  user settato dal server (a seguito operazione di login)
     */
    public User getUser(){
        return this.user;
    }


    /**
     *
     * @param user  user salvato nella lista degli utenti registrati
     */
    public void setUser(User user){
        this.user = user;
    }

    /**
     *
     * @return lista dei progetti mandati dal server
     */
    public ArrayList<Project> getProjects() {
        return projects;
    }

    /**
     *
     * @param projects  lista dei progetti che il server vuole mandare al client
     */
    public void setProjects(ArrayList<Project> projects) {
        this.projects = projects;
    }

    /**
     *
     * @return lista di membri di un progetto
     */
    public ArrayList<String> getMembers() {
        return members;
    }

    /**
     *
     * @param members  lista dei membri che il server vuole mandare al client
     */
    public void setMembers(ArrayList<String> members) {
        this.members = members;
    }

    /**
     *
     * @return lista cards di un progetto
     */
    public ArrayList<Card> getCards() {
        return cards;
    }

    /**
     *
     * @param cards  lista di cards che il server vuole mandare al client
     */
    public void setCards(ArrayList<Card> cards) {
        this.cards = cards;
    }

    /**
     *
     * @return card richiesta
     */
    public Card getCard() {
        return card;
    }

    /**
     *
     * @param card  card da spedire
     */
    public void setCard(Card card) {
        this.card = card;
    }

    /**
     *
     * @return nickname nel messaggio
     */
    public String getNickname() {
        return this.nickname;
    }

    /**
     *
     * @param nickname  nickname da spedire
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     *
     * @return password nel messaggio
     */
    public String getPassword() {
        return this.password;
    }

    /**
     *
     * @param password  password da spedire
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     *
     * @return nome progetto nel messaggio
     */
    public String getProjectName() {
        return this.projectName;
    }

    /**
     *
     * @param projectName nome progetto da spedire
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     *
     * @return nome membro nel messaggio
     */
    public String getNewMember() {
        return this.newMember;
    }

    /**
     *
     * @param newMember nome nuovo membro da spedire
     */
    public void setNewMember(String newMember) {
        this.newMember = newMember;
    }

    /**
     *
     * @return nome card nel messaggio
     */
    public String getCardName() {
        return this.cardName;
    }

    /**
     *
     * @param cardName nome card da spedire
     */
    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    /**
     *
     * @return descrizione nel messaggio
     */
    public String getDescription() {
        return this.description;
    }

    /**
     *
     * @param description descrizione da spedire
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return lista di partenza nel messaggio
     */
    public String getSourceList() {
        return this.sourceList;
    }

    /**
     *
     * @param sourceList lista di partenza da spedire
     */
    public void setSourceList(String sourceList) {
        this.sourceList = sourceList;
    }

    /**
     *
     * @return lista di destinazione nel messaggio
     */
    public String getDestList() {
        return this.destList;
    }

    /**
     *
     * @param destList lista di destinazione da spedire
     */
    public void setDestList(String destList) {
        this.destList = destList;
    }
}