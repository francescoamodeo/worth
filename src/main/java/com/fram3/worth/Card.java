package com.fram3.worth;

import java.io.Serializable;

/**
 * Card modella una card del servizio
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public class Card implements Serializable {
    private static final long serialVersionUID = 6639799316549652319L;

    /** nome card */
    private final String name;

    /** descrizione card */
    private final String description;

    /** storico spostamenti card */
    private String history;

    public Card(String name, String description){
        this.name = name;
        this.description = description;
        history = "TODO";
    }

    /**
     *
     * @return nome card
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return storico spostamenti card
     */
    public String getHistory() {
        return history;
    }

    /**
     * aggiorna lo storico degli spostamenti della card concatenando
     * alla stringa delle liste in cui è stata la nuova lista in cui si trova
     *
     * @param newList nuova lista in cui è stata inserita la card
     */
    public void updateHistory(String newList){
        this.history += " -> " + newList.toUpperCase();
    }

    /**
     * recupera la lista in cui si trova la card in quel momento prendendo
     * l'ultima stringa concatenata alla history
     *
     * @return stringa che indica il nome della lista nella quale si trova la card in quel momento
     */
    public String getLocation(){
        String[] locations = history.split(" -> ");
        return locations[locations.length - 1];
    }

    /**
     *
     * @return stringa rappresentante la card
     */
    @Override
    public String toString() {
        return "    Name:        " + name + "\n" +
               "    Status:      " + this.getLocation() + "\n" +
               "    Description: " + description;
    }

    /**
     *
     * @param obj oggetto da confrontare con this
     * @return true se i due oggetti sono uguali, false altrimenti
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Card))
            return false;
        return this.name.equals(((Card) obj).getName());
    }
}
