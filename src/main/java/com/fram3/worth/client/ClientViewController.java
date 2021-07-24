package com.fram3.worth.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.fram3.worth.User;
import com.fram3.worth.Card;
import com.fram3.worth.Project;

import java.io.IOException;


/**
 * ClientViewController modella l'interazione con l'utente.
 * Riceve i comandi e sulla base di questi invoca le operazioni
 * fornite dal client, infine mostra il risultato all'utente
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
class ClientViewController {

    /** logica del client */
    private final ClientImpl worth;

    private final BufferedReader bufferedReader;
    private String command;
    private String[] words;
    private boolean firstExecution;
    

    ClientViewController(ClientImpl worth) {
        this.worth = worth;
        InputStreamReader streamReader = new InputStreamReader(System.in);
        bufferedReader = new BufferedReader(streamReader);
        firstExecution = true;
    }


    /**
     * gestisce la prima fase dell'input, nel quale si possono usare solamente
     * i comandi "register", "login", "help" e "exit"
     *
     * @return true se è stato ricevuto il comando "exit"
     * @throws IOException -
     */
    boolean firstInputController() throws IOException {
        if(firstExecution)
            printWelcomeMessage();
        String nickname, password, response;
        System.out.print("\n> ");
        command = bufferedReader.readLine();
        words = command.split(" ");

        switch (words[0]) {
            case "help" : 
                if (words.length != 1) {
                    System.out.println("< Il comando help non richiede argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                System.out.print("< ");
                printInitialCommands();
                break;
            
            case "register" : 
                if (words.length != 3) {
                    System.out.println("< Il comando register deve avere due argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                nickname = words[1];
                password = words[2];
                response = worth.register(nickname, password);
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                else System.out.println("< Utente " + nickname + " registrato con successo");
                break;
            case "login" : 
                if (words.length != 3) {
                    System.out.println("< Il comando login deve avere due argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                nickname = words[1];
                password = words[2];
                response = worth.login(nickname, password);
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                else {
                    System.out.println("< Accesso riuscito");
                    System.out.println("< Ciao "+worth.getUser().getNickname()+ ", benvenuto nella tua area riservata!");
                    System.out.println("< In qualsiasi momento, digita \"help\" per rivedere la lista dei comandi disponibili");
                    System.out.print("< ");
                    printCommands();
                }
                break;
            case "exit" :
                if (words.length != 1) {
                    System.out.println("< Il comando exit non richiede argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                System.out.println("< Grazie per aver utilizzato WORTH. A presto!"); 
                return true; 
            default : 
                System.out.println("< Comando non disponibile");
                System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
        }
        firstExecution = false;
        return false;
    }

    /**
     * gestisce la seconda fase dell'input, nel quale si possono tutte le funzionalità del servizio WORTH
     *
     * @return true se è stato ricevuto il comando "exit"
     * @throws IOException -
     */
    boolean secondInputController() throws IOException {
        String projectName;
        String nickname;
        String cardName;
        StringBuilder description;
        String sourceList;
        String destList;
        StringBuilder message;
        String response;
        System.out.print("\n> ");
        command = bufferedReader.readLine();
        words = command.split(" ");
        switch (words[0]) {
            case "help" :
                if (words.length != 1) {
                    System.out.println("< Il comando help non richiede argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                System.out.print("< ");
                printCommands();
                break;
            case "logout" :
                if (words.length != 2) {
                    System.out.println("< Il comando logout deve avere un argomento");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                response = worth.logout(words[1]);
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                else {
                    System.out.println("< Disconnessione riuscita");
                    System.out.println("< Digita \"exit\" per chiudere WORTH");
                    System.out.println("< Oppure \"help\" per rivedere la lista dei comandi disponibili e continuare ad usare il servizio");
                }
                break;
            case "list_users" :
                if (words.length != 1) {
                    System.out.println("< Il comando list_users non richiede argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                response = worth.listUsers();
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                break;
            case "list_online_users" :
                if (words.length != 1) {
                    System.out.println("< Il comando list_online_users non richiede argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                response = worth.listOnlineUsers();
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                break;

            case "list_projects" :
                if (words.length != 1) {
                    System.out.println("< Il comando list_projects non richiede argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                response = worth.listProjects();
                if (!response.equals("ok"))
                    System.out.println("< " + response);
                break;
            case "create_project" :
                if (words.length != 2) {
                    System.out.println("< Il comando create_project deve avere un argomento");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                response = worth.createProject(projectName);
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                else System.out.println("< Progetto creato ");
                break;
            case "add_member" :
                if (words.length != 3) {
                    System.out.println("< Il comando add_member deve avere due argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                nickname = words[2];
                response = worth.addMember(projectName, nickname);
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                else System.out.println("< Membro aggiunto ");
                break;
            case "show_members" :
                if (words.length != 2) {
                    System.out.println("< Il comando show_members deve avere un argomento");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                response = worth.showMembers(projectName);
                if (!response.equals("ok"))
                    System.out.println("< " + response);
                break;
            case "show_cards" :
                if (words.length != 2) {
                    System.out.println("< Il comando show_cards deve avere un argomento");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                response = worth.showCards(projectName);
                if (!response.equals("ok"))
                    System.out.println("< " + response);
                break;
            case "show_card" :
                if (words.length != 3) {
                    System.out.println("< Il comando show_card deve avere due argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                cardName = words[2];
                response = worth.showCard(projectName, cardName);
                if (!response.equals("ok"))
                    System.out.println("< " + response);
                break;
            case "add_card" :
                if (words.length < 4) {
                    System.out.println("< Il comando add_card deve avere tre argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                cardName = words[2];
                description = new StringBuilder();
                for (int i = 3; i < words.length; i++) {
                    description.append(words[i]);
                    if (i < words.length - 1)
                        description.append(" ");
                }
                response = worth.addCard(projectName, cardName, description.toString());
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                else System.out.println("< Card aggiunta ");
                break;
            case "move_card" :
                if (words.length != 5) {
                    System.out.println("< Il comando move_card deve avere quattro argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                cardName = words[2];
                sourceList = words[3];
                destList = words[4];
                response = worth.moveCard(projectName, cardName, sourceList, destList);
                if(!response.equals("ok"))
                    System.out.println("< " + response);    
                else System.out.println("< Card spostata ");
                break;
            case "get_card_history" :
                if (words.length != 3) {
                    System.out.println("< Il comando get_card_history deve avere due argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                cardName = words[2];
                response = worth.getCardHistory(projectName, cardName);
                if (!response.equals("ok"))
                    System.out.println("< " + response);
                break;
            case "send" :
                if (words.length < 3) {
                    System.out.println("< Il comando send deve avere come argomenti il nome del progetto "
                            + "seguito dal messaggio");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                message = new StringBuilder();
                for (int i = 2; i < words.length; i++) {
                    message.append(words[i]);
                    if (i < words.length - 1)
                        message.append(" ");
                }
                response = worth.sendChatMsg(projectName, message.toString());
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                else System.out.println("< Messaggio inviato ");
                break;
            case "receive" :
                if (words.length != 2) {
                    System.out.println("< Il comando receive deve avere un argomento");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                response = worth.readChat(projectName);
                if (!response.equals("ok"))
                    System.out.println("< " + response);
                break;
            case "cancel_project" :
                if (words.length != 2) {
                    System.out.println("< Il comando cancel_project deve avere un argomento");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                projectName = words[1];
                response = worth.cancelProject(projectName);
                if(!response.equals("ok"))
                    System.out.println("< " + response);
                else System.out.println("< Progetto cancellato");
                break;
            case "exit" : 
                if (words.length != 1) {
                    System.out.println("< Il comando exit non richiede argomenti");
                    System.out.println("< Digita \"help\" per rivedere la lista dei comandi disponibili");
                    break;
                }
                System.out.println("< Grazie per aver utilizzato WORTH. A presto!");
                return true; 
            default : 
                System.out.println("< Comando non disponibile");
                System.out.println("< Digita \"help\" per avere una lista dei comandi disponibili");
        }
        return false;
    }

    /**
     * stampa una lista di progetti formattata includendo un messaggio
     * @param projects lista dei progetti da stampare
     * @param message messaggio da includere nella stampa
     */
    static void printFormattedProjects(ArrayList<Project> projects, String message) {
        int maxNameLength = 0;
        for (Project project : projects) {
            if(project.getName().length() > maxNameLength)
                maxNameLength = project.getName().length();
        }
        
        System.out.println("< " + message + ":");
        for (Project project : projects) {
            String indent = " ";
            int distance = maxNameLength - project.getName().length();
            indent = indent.repeat(distance);
            System.out.println("    Project: " + project.getName() + indent + "     Members: "+ project.getMembers().size() + 
                "     Cards: "+ project.getCards().size() +
                "     TODO: " + project.getToDo().size()+ "     INPROGRESS: " + project.getInProgress().size() + 
                "     TOBEREVISED: "+ project.getToBeRevised().size() + "     DONE : " + project.getDone().size());
        }
    }

    /**
     * stampa una lista di utenti formattata includendo un messaggio
     * @param users lista di utenti da stampare
     * @param message messaggio da includere nella stampa
     */
    static void printFormattedUsers(ArrayList<User> users, String message) {
        
        int maxNameLength = 0;
        for (User user : users) {
            if(user.getNickname().length() > maxNameLength)
                maxNameLength = user.getNickname().length();
        }

        System.out.println("< " + message + ":");
        for (User member : users) {
            String indent = " ";
            int distance = maxNameLength - member.getNickname().length();
            indent = indent.repeat(distance);
            System.out.println("    Nickname: " + member.getNickname() + " " + 
                indent + " " + "    Status: " + (member.isOnline() ? "online" : "offline"));
        }
    }

    /**
     * stampa una lista di cards di un progetto formattata includendo un messaggio
     * @param cards lista di cards da stampare
     * @param message messaggio da includere nella stampa
     */
    static void printFormattedCards(ArrayList<Card> cards, String message) {
        int maxNameLength = 0;
        for (Card card : cards) {
            if(card.getName().length() > maxNameLength)
                maxNameLength = card.getName().length();
        }
        
        System.out.println("< " + message + ":");
        for (Card card : cards) {
            String indent = " ";
            int distance = maxNameLength - card.getName().length();
            indent = indent.repeat(distance);
            System.out.println("    Card: " + card.getName() + " " + indent + "    Status: " + card.getLocation());
        }
    }

    /**
     * stampa il dettaglio di una card
     * @param card card da stampare
     */
    static void printCard(Card card){
        System.out.println("< Dettaglio card:");
        System.out.println(card);
    }

    /**
     * stampa lo storico di una card
     * @param card card di cui stampare lo storico
     */
    static void printCardHistory(Card card){
        System.out.println("< Sequenza spostamenti card:");
        System.out.println("    " + card.getHistory());
    }

    /**
     * stampa il messaggio di benvenuto
     */
    private void printWelcomeMessage() {
        System.out.println("  _      ______  ___  ________ __\n" +
                        " | | /| / / __ \\/ _ \\/_  __/ // /\n" +
                        " | |/ |/ / /_/ / , _/ / / / _  / \n" +
                        " |__/|__/\\____/_/|_| /_/ /_//_/ ");
        System.out.println("\nWORTH 1.0 Command Line Interface");
        System.out.println("Francesco Amodeo 560628");
        System.out.println("Usage: command [args]");
        printInitialCommands();
        System.out.println("\nAccedi con il tuo account oppure registrati per iniziare ad utilizzare il servizio");
        System.out.println("Digita \"help\" in qualsiasi momento, per rivedere la lista dei comandi disponibili");
        System.out.println("Digita \"exit\" in qualsiasi momento, per chiudere WORTH");
    }

    /**
     * stampa i comandi della prima fase di input
     */
    private void printInitialCommands() {
        System.out.println("Available Commands:");
        System.out.print("    help                                                           ");
        System.out.println("    Mostra la lista di tutti i comandi diponibili.");
        System.out.print("    exit                                                           ");
        System.out.println("    Chiude l'applicazione WORTH.");
        System.out.print("    register            <nickname> <password>                      ");
        System.out.println("    Registra l'utente con il nickname e la password forniti.");
        System.out.print("    login               <nickname> <password>                      ");
        System.out.println("    Effettua il login dell'utente.");
    }

    /**
     * stampa tutti i comandi
     */
    private void printCommands() {
        System.out.println("Available Commands:");
        System.out.print("    help                                                           ");
        System.out.println("    Mostra la lista di tutti i comandi diponibili.");
        System.out.print("    exit                                                           ");
        System.out.println("    Chiude l'applicazione WORTH.");
        System.out.print("    list_users                                                     ");
        System.out.println("    Mostra la lista di tutti gli utenti registrati.");
        System.out.print("    list_online_users                                              ");
        System.out.println("    Mostra la lista degli utenti online.");
        System.out.print("    list_projects                                                  ");
        System.out.println("    Mostra la lista dei progetti dell'utente.");
        System.out.print("    create_project      <project_name>                             ");
        System.out.println("    Crea un nuovo progetto.");
        System.out.print("    add_member          <project_name> <nickname>                  ");
        System.out.println("    Aggiunge un membro al progetto.");
        System.out.print("    show_members        <project_name>                             ");
        System.out.println("    Mostra la lista dei membri del progetto.");
        System.out.print("    show_cards          <project_name>                             ");
        System.out.println("    Mostra la lista delle card del progetto.");
        System.out.print("    show_card           <project_name> <card_name>                 ");
        System.out.println("    Mostra in dettaglio la card.");
        System.out.print("    add_card            <project_name> <card_name> <description>   ");
        System.out.println("    Aggiunge la card al progetto. ");
        System.out.print("    move_card           <project_name> <card_name> <source> <dest> ");
        System.out.println("    Sposta la card nella lista di destinazione del progetto.");
        System.out.print("    get_card_history    <project_name> <card_name>                 ");
        System.out.println("    Mostra la sequenza degli spostamenti della card.");
        System.out.print("    send                <project_name> <message>                   ");
        System.out.println("    Invia il messaggio nella chat del progetto.");
        System.out.print("    receive             <project_name>                             ");
        System.out.println("    Visualizza i messaggi della chat del progetto.");
        System.out.print("    cancel_project      <project_name>                             ");
        System.out.println("    Cancella il progetto.");
        System.out.print("    logout              <nickname>                                 ");
        System.out.println("    Effettua il logout dell'utente.");
    }

}
