package com.fram3.worth;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.fram3.worth.server.ServerImpl;
import com.fram3.worth.utils.Message;
import com.fram3.worth.utils.SecurePassword;

/**
 * WorthImpl è l'implementazione dell'interfaccia Worth e modella
 * la logica delle funzionalità del servizio worth
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public class WorthImpl implements Worth {

    /** server che gestisce il servizio */
    private final ServerImpl server;

    /** ultimo indirizzo di chat assegnato */
    private String multicastAddress;

    /** ultima porta per il servizio di multicast assegnata */
    private int multicastPort;

    /** lista di utenti registrati */
    private final ArrayList<User> registeredUsers;

    /** lista dei progetti creati */
    private final ArrayList<Project> createdProjects;

    public WorthImpl(ServerImpl server) {
        this.server = server;
        //indirizzo di partenza per le chat multicast
        //ad ogni nuovo indirizzo assegnato aggiorniamo questa stringa
        //la quale rappresenterà l'ultimo indirizzo di multicast assegnato
        multicastAddress = "239.0.0.0";
        //porta di partenza per servizio di multicast
        //ad ogni nuova porta assegnato aggiorniamo questo valore
        //il quale rappresenterà l'ultima porta per multicast assegnata
        multicastPort = 10000;
        registeredUsers = new ArrayList<>();
        createdProjects = new ArrayList<>();
    }


    /**
     *
     * @return la lista degli utenti registrati
     */
    public ArrayList<User> getRegisteredUsers() {
        return registeredUsers;
    }

    /**
     *
     * @return la lista dei progetti creati
     */
    public ArrayList<Project> getCreatedProjects() {
        return createdProjects;
    }

    /**
     * effettua il login dell'utente
     *
     * @param nickname nome utente che ha richiesto il login
     * @param password password fornita per accedere
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    @Override
    public Message login(String nickname, String password) {
        Message message = new Message();
        User tmp = new User(nickname, password);
        int index = registeredUsers.indexOf(tmp);
        //utente non registrato
        if (index == -1) {
            message.setResponse(ResponseType.NOT_REGISTERED);
        } else
            try {   //check password
                if (!SecurePassword.check(password, registeredUsers.get(index).getPassword())) {
                    message.setResponse(ResponseType.WRONG_PASSW);
                } else if (registeredUsers.get(index).isOnline()) {
                    message.setResponse(ResponseType.ALREADY_LOGGED);
                } else {
                    //sincronizzazione sulla lista perchè può essere modificata concorrentemente
                    //dai thread che eseguono i task RequestHandler
                    synchronized (registeredUsers) {
                        registeredUsers.get(index).setOnline(true);
                    }
                    User user = registeredUsers.get(index);
                    message.setUser(user);
                    message.setResponse(ResponseType.OK);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        return message;
    }


    /**
     * effettua il logout dell'utente
     *
     * @param nickname nome utente che ha richiesto il logout
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    @Override 
    public Message logout(String nickname) {
        Message message = new Message();
        int index = registeredUsers.indexOf(new User(nickname, null));
        if (index != -1) {
            synchronized (registeredUsers) {
                registeredUsers.get(index).setOnline(false);
            }
            message.setResponse(ResponseType.OK);
            return message;
        } 
        message.setResponse(ResponseType.UNKNOWN_ERROR);
        return message;
    }


    /**
     * costruisce la lista dei progetti di cui l'utente fa parte
     *
     * @param nickname nome utente che ha richiesto la lista dei progetti
     * @return messaggio da inviare al client contenente la lista dei progetti di cui fa parte
     */
    @Override 
    public Message listProjects(String nickname) {
        Message message = new Message();
        //costruiamo la lista dei progetti dell'utente
        ArrayList<Project> userProjects = new ArrayList<>();
        for (Project project : createdProjects) {
            if (project.getMembers().contains(nickname))
                userProjects.add(project);
        }
        message.setResponse(ResponseType.OK);
        message.setProjects(userProjects);
        return message;
    }


    /**
     * crea un nuovo progetto
     * e invia un messaggio nella chat di progetto indicando l'operazione eseguita
     *
     * @param nickname nome utente che ha richiesto la creazione del progetto
     * @param projectName nome progetto da creare
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    @Override 
    public Message createProject(String nickname, String projectName) {
        Message message = new Message();
        Project project = new Project(projectName, nickname);
        if (!bindChatAddress(project)) {
            message.setResponse(ResponseType.UNABLE_CREATE_PROJECT);
            return message;
        }
        // controllo e modifica atomici
        synchronized (createdProjects) {
            if (createdProjects.contains(project)) {
                message.setResponse(ResponseType.PROJECT_EXISTS);
            } else {
                //aggiorno la lista di tutti i progetti lato server
                createdProjects.add(project);
                message.setResponse(ResponseType.OK);
                server.updateClientChats();
                sendChatMsg(project, nickname + " ha creato il progetto " + projectName);
            }
        }
        return message;
    }


    /**
     * aggiunge un nuovo membro al progetto indicato
     * e invia un messaggio nella chat di progetto indicando l'operazione eseguita
     *
     * @param nickname nome utente che ha richiesto l'aggiunta di un nuovo membro
     * @param projectName nome progetto a cui aggiungere il nuovo membro
     * @param nickNewMember nome utente del membro da aggiungere
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    @Override 
    public Message addMember(String nickname, String projectName, String nickNewMember) {
        Message message = new Message();
        int projectIndex = createdProjects.indexOf(new Project(projectName, null));
        // controllo esistenza del progetto
        if (projectIndex == -1) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        Project project = createdProjects.get(projectIndex);
        // controllo appartenenza dell'utente al progetto
        if (!project.getMembers().contains(nickname)) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        // controllo esistenza negli utenti registrati del nuovo membro
        int newMemberIndex = registeredUsers.indexOf(new User(nickNewMember, null));
        if (newMemberIndex == -1) {
            message.setResponse(ResponseType.NOT_REGISTERED);
            return message;
        }
        synchronized (createdProjects) {
            // controllo che il nuovo membro non sia già membro del progetto
            if (project.getMembers().contains(nickNewMember)) {
                message.setResponse(ResponseType.MEMBER_EXISTS);
                return message;
            }
            // modifico nella lista createdProject (aggiungo il nuovo membro al progetto)
            project.getMembers().add(nickNewMember);
        }
        message.setResponse(ResponseType.OK);
        server.updateClientChats();
        sendChatMsg(project, nickname + " ha aggiunto un nuovo membro: " + nickNewMember);
        return message;
    }


    /**
     * recupera la lista dei membri del progetto
     *
     * @param nickname nome utente che ha richiesto la lista dei membri del progetto
     * @param projectName nome progetto del quale è stata richiesta la lista dei membri
     * @return messaggio da inviare al client contenente la lista dei membri del progetto
     */
    @Override 
    public Message showMembers(String nickname, String projectName) {
        Message message = new Message();
        int projectIndex = createdProjects.indexOf(new Project(projectName, null));
        // controllo esistenza del progetto
        if (projectIndex == -1) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        Project project = createdProjects.get(projectIndex);
        // controllo appartenenza dell'utente al progetto
        if (!project.getMembers().contains(nickname)) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        message.setResponse(ResponseType.OK);
        message.setMembers(project.getMembers());
        return message;
    }


    /**
     * recupera la lista di cards del progetto
     *
     * @param nickname nome utente che ha richiesto la lista di cards del progetto
     * @param projectName nome progetto del quale e' stata richiesta la lita di cards
     * @return messaggio da inviare al client contenente la lista delle cards del progetto
     */
    @Override 
    public Message showCards(String nickname, String projectName) {
        Message message = new Message();
        int projectIndex = createdProjects.indexOf(new Project(projectName, null));
        // controllo esistenza progetto
        if (projectIndex == -1) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        Project project = createdProjects.get(projectIndex);
        // controllo appartenenza dell'utente al progetto
        if (!project.getMembers().contains(nickname)) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }

        ArrayList<Card> cards = new ArrayList<>(project.getCards());
        message.setResponse(ResponseType.OK);
        message.setCards(cards);
        return message;
    }


    /**
     * recupera la card richiesta
     *
     * @param nickname nome utente che ha richiesto la card
     * @param projectName nome progetto a cui appartiene la card
     * @param cardName nome card richiesta
     * @return messaggio da inviare al client contenente la card richiesta
     */
    @Override 
    public Message showCard(String nickname, String projectName, String cardName) {
        Message message = new Message();
        int projectIndex = createdProjects.indexOf(new Project(projectName, null));
        // controllo esistenza progetto
        if (projectIndex == -1) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        Project project = createdProjects.get(projectIndex);
        // controllo appartenenza dell'utente al progetto
        if (!project.getMembers().contains(nickname)) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        int cardIndex = project.getCards().indexOf(new Card(cardName, null));
        // controllo esistenza della carta nel progetto
        if (cardIndex == -1) {
            message.setResponse(ResponseType.NONEXISTENT_CARD);
            return message;
        }
        // scrivo la carta nel messaggio
        message.setResponse(ResponseType.OK);
        message.setCard(project.getCards().get(cardIndex));
        return message;
    }


    /**
     * aggiunge la card con i dettagli forniti al progetto
     * e invia un messaggio nella chat di progetto indicando l'operazione eseguita
     *
     * @param nickname nome utente che ha richiesto l'aggiunta della card
     * @param projectName nome progetto al quale bisogna aggiungere la card
     * @param cardName nome card da aggiungere
     * @param description descrizione card da aggiungere
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    @Override 
    public Message addCard(String nickname, String projectName, String cardName, String description) {
        Message message = new Message();
        int projectIndex = createdProjects.indexOf(new Project(projectName, null));
        // controllo esistenza progetto
        if (projectIndex == -1) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        Project project = createdProjects.get(projectIndex);
        // controllo appartenenza dell'utente al progetto
        if (!project.getMembers().contains(nickname)) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        synchronized (createdProjects) {
            // controllo se la carta esiste già
            Card card = new Card(cardName, description);
            if (project.getCards().contains(card)) {
                message.setResponse(ResponseType.CARD_EXISTS);
                return message;
            }
            // la aggiungo al progetto (nella lista delle carte totali e nella lista to_do)
            project.getCards().add(card);
            project.getToDo().add(card);
        }
        message.setResponse(ResponseType.OK);
        sendChatMsg(project, nickname + " ha aggiunto la carta " + cardName);
        return message;
    }


    /**
     *  sposta la card, se consentito, da una lista di partenza a una di destinazione
     *  e invia un messaggio nella chat di progetto indicando l'operazione eseguita
     *
     * @param nickname nome utente che ha richiesto lo spostamento della card
     * @param projectName nome progetto di cui fa parte la card
     * @param cardName nome card da spostare
     * @param sourceList lista di partenza da cui spostare la card
     * @param destList lista di destinazione in cui spostare la card
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    @Override 
    public Message moveCard(String nickname, String projectName, String cardName, String sourceList, String destList) {
        Message message = new Message();
        int projectIndex = createdProjects.indexOf(new Project(projectName, null));
        // controllo esistenza progetto
        if (projectIndex == -1) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        Project project = createdProjects.get(projectIndex);
        // controllo appartenenza dell'utente al progetto
        if (!project.getMembers().contains(nickname)) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        // prendo il riferimento alla lista di partenza
        ArrayList<Card> sList;
        switch (sourceList) {
            case "todo" : sList = createdProjects.get(projectIndex).getToDo(); break;
            case "inprogress" : sList = createdProjects.get(projectIndex).getInProgress(); break;
            case "toberevised" : sList = createdProjects.get(projectIndex).getToBeRevised(); break;
            case "done" : sList = createdProjects.get(projectIndex).getDone(); break;
            default :
                message.setResponse(ResponseType.NONEXISTENT_LIST);
                return message;
        }
        // prendo il riferimento alla lista di destinazione
        ArrayList<Card> dList;
        switch (destList) {
            case "todo" : dList = createdProjects.get(projectIndex).getToDo(); break;
            case "inprogress" : dList = createdProjects.get(projectIndex).getInProgress(); break;
            case "toberevised" : dList = createdProjects.get(projectIndex).getToBeRevised(); break;
            case "done" : dList = createdProjects.get(projectIndex).getDone(); break;
            default :
                message.setResponse(ResponseType.NONEXISTENT_LIST);
                return message;
        }
        // controllo che lista di partenza e di destinazione non siano uguali
        if (sourceList.equals(destList)) {
            message.setResponse(ResponseType.CARD_EXISTS);
            return message;
        }
        // controllo che siano rispettati i vincoli sullo spostamento
        switch (destList) {
            case "todo" :
                message.setResponse(ResponseType.MOVE_FORBIDDEN);
                return message;
                
            case "inprogress" :
                if (sourceList.equals("done")) {
                    message.setResponse(ResponseType.MOVE_FORBIDDEN);
                    return message;
                }
                break;
            case "toberevised" :
                if (!sourceList.equals("inprogress")) {
                    message.setResponse(ResponseType.MOVE_FORBIDDEN);
                    return message;
                }
                break;
            case "done" :
                if (sourceList.equals("todo")){
                    message.setResponse(ResponseType.MOVE_FORBIDDEN);
                    return message;
                }
                break;
            default :
        }
        synchronized (createdProjects) {
            // controllo che la carta da spostare sia effettivamente nella lista di partenza
            int cardIndex = sList.indexOf(new Card(cardName, null));
            if (cardIndex == -1) {
                message.setResponse(ResponseType.NONEXISTENT_CARD);
                return message;
            }
            // sposto la carta da sourceList a destList, e aggiorno la sua history
            Card card = sList.remove(cardIndex);
            card.updateHistory(destList);
            dList.add(card);
            // aggiorno anche nella lista di tutte le carte create
            int cardIndex2 = project.getCards().indexOf(card);
            project.getCards().set(cardIndex2, card);
        }
        // ritorno il messaggio per il client
        message.setResponse(ResponseType.OK);
        sendChatMsg(project, nickname + " ha spostato la carta " + cardName + 
                    " dalla lista " + sourceList + " alla lista " + destList + ".");
        return message;
    }


    /**
     * cancella il progetto, controllando che tutte le card siano in done
     * e invia un messaggio nella chat di progetto indicando l'operazione eseguita
     *
     * @param nickname nome utente che ha richiesto la cancellazione del progetto
     * @param projectName nome progetto da cancellare
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    @Override 
    public Message cancelProject(String nickname, String projectName) {
        Message message = new Message();
        int projectIndex = createdProjects.indexOf(new Project(projectName, null));
        // controllo esistenza progetto
        if (projectIndex == -1) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        Project project = createdProjects.get(projectIndex);
        // controllo appartenenza dell'utente al progetto
        if (!project.getMembers().contains(nickname)) {
            message.setResponse(ResponseType.NONEXISTENT_PROJECT);
            return message;
        }
        // controllo che tutte le carte siano nella lista DONE
        boolean ok = true;
        for (Card card : project.getCards()) {
            if (!card.getLocation().equals("DONE")) {
                ok = false;
                break;
            }
        }
        if (!ok) {
            message.setResponse(ResponseType.CANCEL_FORBIDDEN);
            return message;
        }
        // cancello il progetto
        createdProjects.remove(project);
        message.setResponse(ResponseType.OK);
        server.updateClientChats();
        return message;
    }

    /**
     * associa al progetto un indirizzo multicast per la chat
     *
     * @param project nome progetto da associare all'indirizzo della chat
     * @return true se riesce ad associare l'indirizzo, altrimenti false
     */
    public synchronized boolean bindChatAddress(Project project) {
        //split di "\\." perche' "." splitta con tutti i caratteri
        String[] strAddress = multicastAddress.split("\\.");
        int[] address = new int[strAddress.length];
        for (int i = 0; i < address.length; i++)
            address[i] = Integer.parseInt(strAddress[i]);
        //incremento indirizzo a partire dall'ultimo indirizzo assegnato
        if (address[3] < 255) {
            address[3]++;
        } else if (address[2] < 255) {
            address[2]++;
            address[3] = 0;
        } else if (address[1] < 255) {
            address[1]++;
            address[2] = 0;
            address[3] = 0;
        }
        else return false; //indirizzi nel range da 239.0.0.0 a 239.255.255.255 tutti assegnati

        StringBuilder chatAddress = new StringBuilder();
        for (int i = 0; i < address.length - 1; i++) {
            chatAddress.append(address[i]).append(".");
        }
        chatAddress.append(address[address.length - 1]);

        try {
            project.setChatAddress(InetAddress.getByName(chatAddress.toString()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        //aggiornamento ultime assegnazioni
        project.setChatPort(multicastPort++);
        multicastAddress = chatAddress.toString();

        return true;
    }

    /**
     * metodo privato utilizzato per l'invio di un messaggio da parte del servizio
     * alla chat di progetto, in seguito ad un'operazione che ha modificato lo stato del progetto
     *
     * @param project nome progetto della chat in cui inviare il messaggio
     * @param message messaggio da inviare
     */
    private void sendChatMsg(Project project, String message) {
        String chatMsg = "Messaggio da WORTH: " + "\"" + message + "\"";
        byte[] buf = chatMsg.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, project.getChatAddress(), project.getChatPort());
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
