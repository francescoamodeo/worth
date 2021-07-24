package com.fram3.worth.client;

import com.fram3.worth.Chat;
import com.fram3.worth.User;
import com.fram3.worth.Project;
import com.fram3.worth.server.Server;
import com.fram3.worth.Worth.RequestType;
import com.fram3.worth.Worth.ResponseType;
import com.fram3.worth.utils.ChatSniffer;
import com.fram3.worth.utils.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import com.google.gson.Gson;

/**
 * ClientImpl implementa l'interfaccia remota Client
 * e modella la logica del client del servizio WORTH
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public class ClientImpl extends RemoteObject implements Client {
    private static final long serialVersionUID = 4975715027275449432L;

    /** porta servizio di registry */
    private final int registryPort;

    /** porta per la connessione tcp con il server */
    private final int serverPort;

    /** utente gestito dal client */
    private User user;

    /** bool che indica se l'utente è loggato */
    private boolean loggedIn = false;

    /** lista di riferimenti dei thread che eseguono il task ChatSniffer */
    private final ArrayList<Thread> sniffers;

    /** socket channel client */
    private SocketChannel socketChannel;

    /** stub client registrato per le callbacks */
    private Client stub;

    /** definisce i metodi remoti del server */
    private Server server;

    public ClientImpl(){
        registryPort = 9876;
        serverPort = 6789;
        sniffers = new ArrayList<>();
    }

    /**
     *
     * @return utente gestito al client
     */
    public User getUser() {
        return user;
    }

    /**
     * notifica il client in seguito ad un cambiamento di stato degli utenti registrati.
     * Il server invoca il metodo sullo stub del client, ricevuto nel momento della registrazione alle
     * callbacks, passando come parametro la lista aggiornata degli utenti registrati
     *
     * @param registeredUsers lista degli utenti registrati al servizio aggiornata
     * @throws RemoteException -
     */
    @Override
    public void notifyUserEvent(ArrayList<User> registeredUsers) throws RemoteException {
        synchronized (user.getUsers()) {
            //setto la lista locale dell'user con la lista aggiornata tramite callback
            user.setUsersList(registeredUsers);
        }
    }

    /**
     * notifica il client in seguito ad un cambiamento di stato dei progetti.
     * Il server invoca il metodo sullo stub del client, ricevuto nel momento della registrazione alle
     * callbacks, passando come parametro la lista aggiornata dei progetti
     *
     * @param createdProjects lista dei progetti creati nel servizio aggiornata
     * @throws RemoteException -
     */
    @Override
    public void notifyChatsEvent(ArrayList<Project> createdProjects) throws RemoteException {
        //costruisco la lista di chat che interessano l'user
        ArrayList<Chat> userChats = new ArrayList<>();
        for (Project project : createdProjects) {
            if (project.getMembers().contains(user.getNickname()))
                userChats.add(new Chat(project.getChatAddress(), project.getChatPort(), project.getName()));
        }
        synchronized (user.getChats()) {
            //setto la lista locale delle chat dell'user con la lista aggiornata tramite callback
            user.setChats(userChats);
        }
    }

    /**
     * avvia il client.
     * Il client recupera il riferimento dello stub del server dal servizio di registry
     * per eventuali invocazioni dei metodi remoti, apre il socket channel per la comunicazione tcp
     * con il server e usa ClientViewController per l'interazione con l'utente. Tale interazione viene divisa
     * in due fasi: la prima permette solamente di registrarsi, di loggarsi oppure di uscire dal programma,
     * nella seconda fase si entra nel vero e proprio servizio worth, tramite il ClientViewController l'utente,
     * può invocare le operazioni definite nel client, che permettono l'invio di un messaggio al server per una richiesta.
     * Il client gestisce le condizioni di uscita dalle fasi e dal programma stesso, gestendo eventuali eccezioni,
     * chiusura di connessione, esportazione dell'oggetto remoto, e interruzione dei threads secondari
     */
    public void start() {
        //prepara un thread che viene avviato quando la jvm viene interrotta con ctrl+C
        //che invia una richiesta di logout prima di chiudere completamente il client
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (loggedIn) {
                try { logout(user.getNickname());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        Registry registry;
        //gestore dell'interazione con l'utente
        ClientViewController controller = new ClientViewController(this);
        try {
            //recupero il riferimento dell'oggetto remoto del server
            registry = LocateRegistry.getRegistry(registryPort);
            server = (Server) registry.lookup("WORTH");
            boolean exit = false;
            while (!exit) {
                socketChannel = SocketChannel.open();
                //l'user sarà null fin quando l'utente non si è loggato
                while (!loggedIn && !exit)
                    //prima fase (pre-login)
                    exit = controller.firstInputController();
                while (loggedIn && !exit)
                    //seconda fase (post-login)
                    exit = controller.secondInputController();
            }
            //arrivo qui in seguitop ad una "exit"
            //quindi faccio il logout dell'utente se non è gia stato fatto
            if (loggedIn)
                logout(user.getNickname());

        } catch (IOException e) {
            try { //quando il server viene chiuso prima del client viene lanciata una IOException
                  //se quest'ultimo era loggato prima di chiudere il client
                  //chiudo il canale e interrompo il thread dell'oggetto esportato
                  //e quelli degli sniffer delle chat dell'utente
                if(loggedIn) {
                    UnicastRemoteObject.unexportObject(this, false);
                    socketChannel.close();
                    interruptAllSniffers();
                    user = null;
                    loggedIn = false;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.err.println("< Siamo spiacenti, il servizio WORTH non è disponibile. Riprovare più tardi");
        } catch (NotBoundException e) {
            System.err.println("Errore nella connessione al registry del server");
            e.printStackTrace();
        }
    }

    /**
     * richiede la registrazione dell'utente al servizio,
     * utilizzando RMI sullo stub del server recuperato dal registry
     *
     * @param nickname nome utente da registrare
     * @param password password da associare all'utente da registrare
     * @return stringa contenente il responso dell'operazione richiesta
     * @throws RemoteException -
     */
    public String register(String nickname, String password) throws RemoteException {
        //invocazione del metodo remoto del server
        ResponseType response = server.register(nickname, password);
        if (response == ResponseType.OK)
            return "ok";
        else
            return "Impossibile registrarsi: l'utente " + nickname + " esiste già";
    }

    /**
     * richiede il login dell'utente
     *
     * @param nickname nome utente che ha richiesto il login
     * @param password password fornita per accedere
     * @return stringa contentente il responso per l'operazione richiesta
     */
    public String login(String nickname, String password) throws IOException {
        if (!socketChannel.isConnected())
            socketChannel.connect(new InetSocketAddress("localhost", serverPort));

        Message message = new Message(RequestType.LOGIN);
        message.setNickname(nickname);
        message.setPassword(password);
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK :
                user = receivedMsg.getUser();
                //se l'user != null l'operazione di login è andata buon fine
                //e il server ci ha mandato il riferimento all'user
                if (user != null) {
                    loggedIn = true;
                    user.setClient(this);
                    //esportazione stub client da passare al server per le callbacks
                    stub = (Client) UnicastRemoteObject.exportObject(this, 0);
                    server.registerForCallback(stub);
                    return "ok";
                }
                break;
            case NOT_REGISTERED : 
                return "L'utente non è registrato";
            case WRONG_PASSW : 
                return "Password errata";
            case ALREADY_LOGGED : 
                return "Utente già collegato";
            default : 
                return "Errore: errore nella comunicazione con il server";
        }
        return "Errore sconosciuto: errore nella fase di login";
    }


    /**
     * richiede il logout dell'utente
     *
     * @param nickname nome utente che ha richiesto il logout
     * @return stringa contentente il responso per l'operazione richiesta
     */
    public String logout(String nickname) throws IOException {
        if (!user.getNickname().equals(nickname)) 
            return "Nickname errato";

        if (user.isOnline()) {
            Message message = new Message(RequestType.LOGOUT);
            message.setNickname(nickname);
            sendToServer(message);

            Message receivedMsg = receiveFromServer();
            switch (receivedMsg.getResponse()) {
                case OK :
                    loggedIn = false;
                    server.unregisterForCallback(stub);
                    UnicastRemoteObject.unexportObject(this, false);
                    interruptAllSniffers();
                    user = null;
                    socketChannel.close();
                    return "ok";
                    
                case UNKNOWN_ERROR : 
                    return "Errore nella fase di logout";
                    
                default : return "Errore: errore nella comunicazione con il server";
            }
        }
        return "Errore sconosciuto: l'utente e' già disconnesso";
    }

    /**
     * recupera la lista locale degli utenti registrati all'interno dell'user
     * e la stampa utilizzando il metodo printFormattedUsers() di ClientViewController
     *
     * @return stringa contenente il responso per l'operazione richiesta
     */
    public String listUsers() {
        ArrayList<User> users = user.getUsers();
        if (!users.isEmpty()) {
            String msg = users.size() == 1 ? 
                "Attualmente c'è "+ users.size() +" utente registrato a WORTH":
                "Attualmente ci sono "+ users.size() +" utenti registrati a WORTH";
            ClientViewController.printFormattedUsers(users, msg);
            return "ok";
        }
        return "Errore sconosciuto: non ci sono utenti registrati";
    }

    /**
     * recupera la lista locale degli utenti online con il metodo getOnlineUser() di user
     * e la stampa utilizzando il metodo printFormattedUsers() di ClientViewController
     *
     * @return stringa contenente il responso per l'operazione richiesta
     */
    public String listOnlineUsers() {
        ArrayList<User> onlineUsers = user.getOnlineUsers();
        if (!onlineUsers.isEmpty()) {
            String msg = onlineUsers.size() == 1 ? 
                "In questo momento c'è "+ onlineUsers.size() +" utente online" :
                "In questo momento ci sono "+ onlineUsers.size() +" utenti online";
            ClientViewController.printFormattedUsers(onlineUsers, msg);
            return "ok";
        }
        //almeno l'utente che utilizza questo metodo deve essere online 
        return "Errore sconosciuto: non ci sono utenti online";
    }

    /**
     * richiede la lista dei progetti di cui l'utente fa parte e la stampa usando
     * printFormattedProjects() di ClientViewController
     *
     * @return stringa contenente il responso per l'operazione richiesta
     */
    public String listProjects() throws IOException {
        Message message = new Message(RequestType.LIST_PROJECTS);
        message.setNickname(user.getNickname());
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : 
                if(!receivedMsg.getProjects().isEmpty()) {
                    String msg = receivedMsg.getProjects().size() == 1 ? 
                        "Fai parte di "+receivedMsg.getProjects().size()+" progetto, per un totale di " + 
                            countIncompletedCards(receivedMsg.getProjects()) + " card da completare" :
                        "Fai parte di "+receivedMsg.getProjects().size()+" progetti, per un totale di " + 
                            countIncompletedCards(receivedMsg.getProjects()) + " card da completare" ;
                    ClientViewController.printFormattedProjects(receivedMsg.getProjects(), msg);
                    return "ok";
                }
                return "Non fai parte di nessun progetto";
            case UNKNOWN_ERROR : return "Errore sconosciuto nel server";  
            default : return "Errore: errore nella comunicazione con il server";
        }
    }

    /**
     * richiede la creazione di un nuovo progetto
     *
     * @param projectName nome progetto da creare
     * @return stringa contentente il responso per l'operazione richiesta
     */
    public String createProject(String projectName) throws IOException {
        
        Message message = new Message(RequestType.CREATE_PROJECT);
        message.setNickname(user.getNickname());
        message.setProjectName(projectName);
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : 
                return "ok";
                
            case UNABLE_CREATE_PROJECT : 
                return "Errore del server";
                
            case PROJECT_EXISTS : 
                return "Esiste già un progetto con questo nome";
                
            default : return "Errore: errore nella comunicazione con il server";
        }
    }

    /**
     * richiede l'aggiunta di un nuovo membro al progetto indicato
     *
     * @param projectName nome progetto a cui aggiungere il nuovo membro
     * @param nickNewMember nome utente del membro da aggiungere
     * @return stringa contentente il responso per l'operazione richiesta
     */
    public String addMember(String projectName, String nickNewMember) throws IOException {
        
        Message message = new Message(RequestType.ADD_MEMBER);
        message.setProjectName(projectName);
        message.setNickname(user.getNickname());
        message.setNewMember(nickNewMember);
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : return "ok";
            case NOT_REGISTERED : return "L'utente " + nickNewMember + " non esiste";
            case MEMBER_EXISTS : return "L'utente " + nickNewMember + " è già membro del progetto";
            case NONEXISTENT_PROJECT : return "Non sei membro di un progetto di nome " + projectName;
            default : return "Errore: errore nella comunicazione con il server";
        }
    
    }

    /**
     * richiede la lista dei membri del progetto e la stampa usando
     * printFormattedUsers() di ClientViewController
     *
     * @param projectName nome progetto del quale è stata richiesta la lista dei membri
     * @return stringa da inviare al client contenente la lista dei membri del progetto
     */
    public String showMembers(String projectName) throws IOException {
        
        Message message = new Message(RequestType.SHOW_MEMBERS);
        message.setProjectName(projectName);
        message.setNickname(user.getNickname());
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK :
                if (!receivedMsg.getMembers().isEmpty()) {
                    ArrayList<User> members = new ArrayList<>();
                    for (String memberName : receivedMsg.getMembers()) {
                        int memberIndex = user.getUsers().indexOf(new User(memberName, null));
                        members.add(user.getUsers().get(memberIndex));
                    } 
                    String msg = receivedMsg.getMembers().size() == 1 ? 
                        "Il progetto "+projectName+" è composto da " + receivedMsg.getMembers().size() + " membro" : 
                        "Il progetto "+projectName+" è composto da " + receivedMsg.getMembers().size() + " membri" ;
                    ClientViewController.printFormattedUsers(members, msg);
                    return "ok";
                }
                return "Nel progetto non e' presente nessun membro";
            case NONEXISTENT_PROJECT : 
                return "Non sei membro di un progetto di nome " + projectName;
            default : 
                return "Errore: errore nella comunicazione con il server";
        }
    
    }

    /**
     * richiede la lista di cards del progetto e la stampa usando
     * printFormattedCards() di ClientViewController
     *
     * @param projectName nome progetto del quale e' stata richiesta la lista di cards
     * @return stringa da inviare al client contenente la lista delle cards del progetto
     */
    public String showCards(String projectName) throws IOException {
        
        Message message = new Message(RequestType.SHOW_CARDS);
        message.setProjectName(projectName);
        message.setNickname(user.getNickname());
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : 
                if (!receivedMsg.getCards().isEmpty()) {
                    String msg = "Il progetto "+projectName+" è composto da " + receivedMsg.getCards().size() +" card";
                    ClientViewController.printFormattedCards(receivedMsg.getCards(), msg);
                    return "ok";
                } 
                return "Nel progetto non è presente nessuna card";  
            case NONEXISTENT_PROJECT : 
                return "Non sei membro di un progetto di nome " + projectName;
            default : 
                return "Errore: errore nella comunicazione con il server";
        }
    
    }

    /**
     * richiede la card e la stampa usando
     * printCard() di ClientViewController
     *
     * @param projectName nome progetto a cui appartiene la card
     * @param cardName nome card richiesta
     * @return stringa da inviare al client contenente la card richiesta
     */
    public String showCard(String projectName, String cardName) throws IOException {
        
        Message message = new Message(RequestType.SHOW_CARD);
        message.setProjectName(projectName);
        message.setNickname(user.getNickname());
        message.setCardName(cardName);
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : 
                ClientViewController.printCard(receivedMsg.getCard());
                return "ok";
                
            case NONEXISTENT_PROJECT : 
                return "Non sei membro di un progetto di nome " + projectName;
            case NONEXISTENT_CARD : 
                return "Non esiste nessuna carta di nome " + cardName + " nel progetto";
            default : return"Errore: errore nella comunicazione con il server";
        }
    
    }

    /**
     * richiede l'aggiunta della card con i dettagli forniti al progetto
     *
     * @param projectName nome progetto al quale bisogna aggiungere la card
     * @param cardName nome card da aggiungere
     * @param description descrizione card da aggiungere
     * @return stringa contentente il responso per l'operazione richiesta
     */
    public String addCard(String projectName, String cardName, String description) throws IOException {
        
        Message message = new Message(RequestType.ADD_CARD);
        message.setProjectName(projectName);
        message.setCardName(cardName);
        message.setDescription(description);
        message.setNickname(user.getNickname());
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : return "ok";
            case NONEXISTENT_PROJECT : return "Non sei membro di un progetto di nome " + projectName;
            case CARD_EXISTS : return "La card " + cardName + " esiste già";
            default : return "Errore: errore nella comunicazione con il server";
        }
    
    }

    /**
     *  richiede lo spostamento della card, se consentito, da una lista di partenza a una di destinazione
     *
     * @param projectName nome progetto di cui fa parte la card
     * @param cardName nome card da spostare
     * @param sourceList lista di partenza da cui spostare la card
     * @param destList lista di destinazione in cui spostare la card
     * @return stringa contentente il responso per l'operazione richiesta
     */
    public String moveCard(String projectName, String cardName, String sourceList, String destList) throws IOException {
        
        Message message = new Message(RequestType.MOVE_CARD);
        message.setProjectName(projectName);
        message.setCardName(cardName);
        message.setSourceList(sourceList);
        message.setDestList(destList);
        message.setNickname(user.getNickname());
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : return "ok";
            case NONEXISTENT_PROJECT : 
                return "Non sei membro di un progetto di nome " + projectName;
            case NONEXISTENT_LIST : 
                return "Una delle liste non esiste";
            case NONEXISTENT_CARD : 
                return "La card " + cardName + " non è presente nella lista di partenza";
            case MOVE_FORBIDDEN : 
                return "Vietato spostare la card da " + sourceList + " a " + destList;
            case CARD_EXISTS : 
                return "La card " + cardName + " è già nella lista di destinazione";
            default : return "Errore: errore nella comunicazione con il server";
        }
    }

    /**
     * richiede la card e ne stampa lo storico degli spostamenti utilizzando
     * il metodo printCardHistory() di ClientViewController
     * @param projectName nome progetto di cui fa parte la card
     * @param cardName nome card
     * @return stringa contenente il responso per l'operazione richiesta
     * @throws IOException -
     */
    public String getCardHistory(String projectName, String cardName) throws IOException {

        //chiedo al server la card e da questa mi prenderò la history
        Message message = new Message(RequestType.SHOW_CARD);
        message.setProjectName(projectName);
        message.setNickname(user.getNickname());
        message.setCardName(cardName);
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : 
                ClientViewController.printCardHistory(receivedMsg.getCard());
                return "ok";
            case NONEXISTENT_PROJECT :
                return "Non sei membro di un progetto di nome " + projectName;
            case NONEXISTENT_CARD :
                return "Non esiste nessuna carta di nome " + cardName + " nel progetto";
            default : 
                return "Errore: errore nella comunicazione con il server";
        }
    }

    /**
     * invia un messaggio sulla chat di progetto
     * @param projectName nome progetto relativo alla chat su cui inviare il messaggio
     * @param message messaggio da inviare
     * @return stringa contenente il responso per l'operazione richiesta
     */
    public String sendChatMsg(String projectName, String message) {
        if (!user.getChats().contains(new Chat(projectName))) {
            return "Non sei membro di un progetto di nome " + projectName;
        }
        user.sendChatMsg(projectName, message);
        return "ok";
    }

    /**
     * riceve i messaggi della chat di progetto non ancora letti
     * a partire dall'ultima esecuzione dello stesso metodo
     *
     * @param projectName nome progetto relativo alla chat da leggere
     * @return stringa contenente il responso per l'operazione richiesta
     */
    public String readChat(String projectName) {
        if (!user.getChats().contains(new Chat(projectName))) {
            return "Non sei membro di un progetto di nome " + projectName;
        }
        user.readChat(projectName);
        return "ok";
    }

    /**
     * richiede la cancellazione del progetto
     *
     * @param projectName nome progetto da cancellare
     * @return stringa contentente il responso per l'operazione richiesta
     */
    public String cancelProject(String projectName) throws IOException {
        
        Message message = new Message(RequestType.CANCEL_PROJECT);
        message.setProjectName(projectName);
        message.setNickname(user.getNickname());
        sendToServer(message);

        Message receivedMsg = receiveFromServer();
        switch (receivedMsg.getResponse()) {
            case OK : 
                return "ok";
            case NONEXISTENT_PROJECT :
                return "Non sei membro di un progetto di nome " + projectName;
            case CANCEL_FORBIDDEN : 
                return "Impossibile cancellare il progetto: le carte non sono tutte nella lista DONE";
            default : 
                return "Errore: errore nella comunicazione con il server";
        }
    }

    /**
     * invia al server un messaggio per una richiesta, facendo una gathering write sul
     * socket channel su cui è stata stabilita la connessione. Scrive due buffer, il primo
     * per indicare la dimensione del messaggio inviato, il secondo per il messaggio vero e proprio
     *
     * @param message messaggio da inviare
     * @throws IOException errore durante la scrittura sul canale
     */
    private void sendToServer(Message message) throws IOException {
        Gson gson = new Gson();
        String str = gson.toJson(message);
        byte[] byteArray = str.getBytes(StandardCharsets.UTF_8);
        //mando il messaggio al server come array di buffer
        //in un metto la dimensione del messaggio
        //e nell'altro il messaggio stesso
        //in questo modo chi lo riceve alloca i buffer solo con lo spazio necessario
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        sizeBuffer.putInt(byteArray.length);
        sizeBuffer.flip();
        ByteBuffer dataBuffer = ByteBuffer.wrap(byteArray);
        //gathering write
        socketChannel.write(new ByteBuffer[] { sizeBuffer, dataBuffer });
    }

    /**
     * riceve dal server un messaggio, facendo due read sul socket channel su
     * cui è stata stabilita la connessione. Legge due buffer, il primo
     * conterrà la dimensione del messaggio inviato, utile per allocare il secondo
     * buffer della dimensione esatta per contenere il messaggio vero e proprio
     *
     * @return responso in seguito ad una richiesta al server
     * @throws IOException errore nella fase di lettura sul canale
     */
    private Message receiveFromServer() throws IOException {
        //ricevo dal server il messaggio di risposta
        //so esattamente di che dimensione allocare i buffer per
        //per le due read dal canale
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        if (socketChannel.read(sizeBuffer) == -1)
            throw new IOException("Errore: server disconnesso");

        sizeBuffer.flip();
        int size = sizeBuffer.getInt();
        ByteBuffer dataBuffer = ByteBuffer.allocate(size);
        socketChannel.read(dataBuffer);
        dataBuffer.flip();
        StringBuilder stringBuilder = new StringBuilder();
        while (dataBuffer.hasRemaining()) {
            stringBuilder.append(StandardCharsets.UTF_8.decode(dataBuffer).toString());
        }
        //deserializzo il messaggio ricevuto
        String received = stringBuilder.toString();
        Gson gson = new Gson();
        return gson.fromJson(received, Message.class);
    }

    /**
     * avvia un thread che esegue il task ChatSniffer e lo aggiunge alla lista degli sniffers
     * Utilizzato nel momento in cui l'user viene aggiunto a un nuovo progetto e quindi vuole
     * iniziare a memorizzare i messaggi inviati su quella chat
     *
     * @param chat chat da sniffare
     */
    public void startSniffer(Chat chat) {
        Thread snifferThread = new Thread(new ChatSniffer(chat, user));
        snifferThread.setName(chat.getProject());
        sniffers.add(snifferThread);
        snifferThread.start();
    }

    /**
     * interrompe un thread che esegue il task ChatSniffer e lo rimuove alla lista degli sniffers
     * Utilizzato nel momento in cui viene cancellato un progetto dell'user, quindi non vorrà
     * più ricevere e memorizzare i messaggi inviati su quella chat
     * @param chat chat di cui interrompere lo sniffer
     */
    public void interruptSniffer(Chat chat) {
        for (Thread thread : sniffers) {
            if (thread.getName().equals(chat.getProject())) {
                thread.interrupt();
                sniffers.remove(thread);
                break;
            }
        }
    }

    /**
     * interrompe tutti gli sniffer, cioè smette di ricevere tutti i messaggi dalle chat di progetto
     * di cui fa parte l'utente. Utilizzato nel momento quando si deve chiudere il client, cioè in seguito
     * ad un'operazione di logout oppure dopo la disconnessione dal server
     */
    private void interruptAllSniffers() {
        for (Thread thread : sniffers) {
            thread.interrupt();
        }
    }

    /**
     * conta tutte le card nella lista di progetti che non sono state ancora spostate nella lista done
     * Utilizzata come stats nella stampa della list_project
     * @param projects lista di progetti di cui contare le card non ancora completate
     * @return numero card non ancora in done
     */
    private int countIncompletedCards(ArrayList<Project> projects){
        int cardsCount = 0;
        for (Project project : projects) {
            if (project.getDone().isEmpty())
                cardsCount += project.getCards().size();
            else
                cardsCount += project.getCards().size() - project.getDone().size();
        }
        return cardsCount;
    }
}