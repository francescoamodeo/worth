package com.fram3.worth.server;

import com.fram3.worth.User;
import com.fram3.worth.WorthImpl;
import com.fram3.worth.client.Client;
import com.fram3.worth.Worth.ResponseType;
import com.fram3.worth.utils.SecurePassword;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ServerImpl implementa l'interfaccia remota Server
 * e modella la logica del server per il servizio WORTH
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public class ServerImpl extends RemoteServer implements Server {
    private static final long serialVersionUID = 4864357596811527790L;

    /** funzionalità del servizio worth */
    private final WorthImpl worth;

    /** gestore persistenza degli utenti e dei progetti */
    private final PersistenceManager persistence;

    /** lista degli stub dei clients registrati per le callbacks */
    private final ArrayList<Client> clientsRegisteredForCallback;

    /** threadpool usato per l'elaborazione delle richieste dei clients */
    private final ThreadPoolExecutor requestPool;

    /** porta servizio di registry */
    private final int registryPort;

    /** porta server socket */
    private final int serverSocketPort;

    public ServerImpl() {
        worth = new WorthImpl(this);
        persistence = new PersistenceManager(worth, "users", "members");
        clientsRegisteredForCallback = new ArrayList<>();
        requestPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        registryPort = 9876;
        serverSocketPort = 6789;
    }

    /**
     * registra l'utente al servizio con nickname e password forniti
     *
     * @param nickname nome utente da registrare
     * @param password password da associare all'utente
     * @return responso per l'operazione richiesta
     * @throws RemoteException -
     */
    @Override
    public ResponseType register(String nickname, String password) throws RemoteException {
        User user = null;
        try {
            //creo il nuovo utente da registrare facendo l'hash della password fornita
            user = new User(nickname, SecurePassword.getSaltedHash(password));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //sincronizzo sulla lista di utenti registrati perchè in un dato momento ci possono essere
        //diversi thread RequestHandler che per soddisfare le richieste del client possono invocare metodi
        //della classe WorthImpl i quali modificano lo stato di questa lista
        synchronized (worth.getRegisteredUsers()) {
            //l'utente esiste già
            if (worth.getRegisteredUsers().contains(user)) {
                return ResponseType.USER_EXISTS;
            }
            //aggiungo l'utente alla lista degli utenti registrati
            worth.getRegisteredUsers().add(user);
        }
        //arrivati qui c'è stato un cambiamento di stato degli utenti registrati al servizio,
        //faccio la update per innescare le callbacks ai clients registrati
        updateClientUsers();
        return ResponseType.OK;
    }

    /**
     * registra il client per le callbacks
     *
     * @param clientStub stub/proxy corrispondente al riferimento remoto dell'oggetto client
     *                   utilizzato dal server per le callbacks
     * @throws RemoteException -
     */
    @Override
    public synchronized void registerForCallback(Client clientStub) throws RemoteException {
        if (!clientsRegisteredForCallback.contains(clientStub)) {
            clientsRegisteredForCallback.add(clientStub);
            //faccio le callback perchè questo metodo viene invocato da remoto
            //dal client subito dopo la procedura di login
            //in questo modo tutti gli utenti riceveranno l'aggiornamento che un utente è online
            //l'utente che ha fatto il login riceverà la lista di tutti gli utenti registrati
            //e la lista delle chat dei progetti di cui è membro
            updateClientUsers();
            updateClientChats();
        }
    }

    /**
     * deregistra il client per le callbacks
     *
     * @param clientStub stub/proxy corrispondente al riferimento remoto dell'oggetto client
     *                   utilizzato dal server per le callbacks
     * @throws RemoteException -
     */
    @Override
    public synchronized void unregisterForCallback(Client clientStub) throws RemoteException {
        clientsRegisteredForCallback.remove(clientStub);
        //questo metodo viene invocato da remoto dal client subito dopo la procedura di logout
        //pertanto per notificare tutti gli utenti del cambiamento di stato dell'utente che ha
        //appena fatto logout facciamo le callbacks sulle liste degli utenti registrati
        updateClientUsers();
    }

    /**
     * avvia il server.
     * Il server è gestito tramite un selettore che riceve le richieste di connessione,
     * lettura e scrittura dai socket channel registrati. Un canale viene registrato sul selettore
     * con una chiave che contiene le operazioni di interesse e pronte sul quel canale.
     * Nel momento in cui viene accettata una connessione, il socket channel risultante viene registrato
     * con operazione di interesse di lettura. Appena verrà scritto qualcosa su quel canale, la relativa chiave
     * diventerà pronta per un'operazione di lettura. Il selettore selezionerà la chiave tramite la .select() e la richiesta
     * verrà letta e incaricata ad un thread del pool requestPool che esegue il task RequestHandler per l'elaborazione
     * della richiesta. Al termine dell'elaborazione il task assegna alla chiave l' operazione di interesse di
     * scrittura e finalmente quando il selettore selezionerà di nuovo la chiave verrà scritto sul relativo canale il
     * responso dell'operazione.
     */
    public void start() {
        //prepara un thread che viene avviato quando la jvm viene interrotta con ctrl+C
        //che salva lo stato degli utenti e dei progetti prima che termini del tutto
        Runtime.getRuntime().addShutdownHook(new Thread(persistence::saveResources));

        //carico lo stato dall'ultima esecuzione del server
        persistence.loadResources();
        try {
            //esporto l'oggetto this per l'invocazione dei metodi remoti da parte del client
            Server stub = (Server) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.createRegistry(registryPort);
            registry.rebind("WORTH", stub);
            System.out.println("Server: servizio di registry pronto sulla porta " + registryPort);
            
            //apro il canale tcp
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), serverSocketPort));
            System.out.println("Server: in ascolto sulla porta "+serverSocketPort);
            serverSocketChannel.configureBlocking(false);
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            //noinspection InfiniteLoopStatement
            while (true) {
                if (selector.select() == 0) continue;
                //insieme delle chiavi dei canali pronti per un'operazione
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = readyKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(selector, key);

                    } else if (key.isReadable()) {
                        readRequest(key);

                    } else if (key.isWritable()) {
                        writeResponse(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //entro qui solamente se viene lanciata un'eccezione, 
            //altrimenti il server termina con lo shutdown hook dopo il ctrl+C
            persistence.saveResources();
        }
    }

    /**
     * accetta la connessione e registra l'interesse per l'operazione di READ.
     * Prepara e inserisce nell'attachment l'array di ByteBuffer da riempire con
     * due read successive, in modo da leggere dimensione messaggio e messaggio stesso
     *
     * @param selector selettore a cui bisogna registrare il canale
     * @param key chiave relativa al canale che è acceptable
     * @throws IOException errore di I/O
     */
    private void acceptConnection(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        //accettazione connessione, ritorna il socket per la comunicazione con il client
        SocketChannel client = server.accept();
        System.out.println("Server: connessione ricevuta");
        client.configureBlocking(false);

        //preparo i buffer per ricevere i dati sul canale
        //il primo buffer conterrà un intero corrispondente alla dimensione del dato inviato sul canale
        ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        //sapremo di che dimensione allocare il buffer dati solo dopo la prima read nel primo buffer
        ByteBuffer dataBuffer = null;
        //registro il canale per lettura e metto nell'attachment un array di ByteBuffer
        client.register(selector, SelectionKey.OP_READ, new ByteBuffer[] { sizeBuffer, dataBuffer});
    }

    /**
     * legge la richiesta del client effettuando due read successive:
     * la prima riempe il primo buffer dell'attachment, allocato della dimensione di
     * un intero, per contenere il valore della dimensione del messaggio da leggere;
     * la seconda read riempie il secondo buffer con il messaggio effettivo, quest'ultimo
     * buffer viene allocato in modo preciso grazie al valore del primo buffer letto con la prima read.
     * A questo punto la richiesta contenuta nel secondo buffer viene incaricata ad un thread
     * del pool che eseguirà il task RequestHandler. La richiesta viene deserializzata
     * e risolta. Alla fine dell'elaborazione l'handler inserisce il response come
     * attachment della key e configura l'interesse per la scrittura su quel canale
     *
     * @param key chiave selezionata
     * @throws IOException errore di I/O
     */
    private void readRequest(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        //prendo i buffer dall'attachment inseriti nella registrazione per lettura
        ByteBuffer[] buffers = (ByteBuffer[]) key.attachment();
        client.read(buffers[0]);
        if (!buffers[0].hasRemaining()) { //abbiamo letto tutto dal canale
            buffers[0].flip();
            //dimensione del dato da leggere nel secondo buffer
            int size = buffers[0].getInt();

            //alloco il buffer della dimensione esatta
            buffers[1] = ByteBuffer.allocate(size);
            client.read(buffers[1]);
            if (buffers[1].position() == size) { //abbiamo letto esattamente size bytes
                buffers[1].flip();
                //assegno la richiesta al task
                RequestHandler task = new RequestHandler(worth, buffers[1], key);
                requestPool.execute(task);
            }
        }
    }

    /**
     * scrive sul canale il response che il RequestHandler aveva inserito nell'attachment.
     * La write è di tipo gathering, cioè scrive una sequenza di buffer sul canale con un'unica
     * invocazione, inviando i due buffer con dimensione messaggio e messaggio
     * @param key chiave selezionata
     * @throws IOException errore di I/O
     */
    private void writeResponse(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        //prendo l'attachment settato dal task
        ByteBuffer[] buffers = (ByteBuffer[]) key.attachment();
        client.write(buffers);
        if (!buffers[0].hasRemaining() && !buffers[1].hasRemaining()){
            //se ho finito di scrivere entrambi i buffer posso ripulirli e riutilizzarli
            buffers[0].clear(); buffers[1].clear();
            //reimposto l'interesse di lettura sul canale
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    /**
     *  aggiorna le liste locali di chat degli users/clients registrati per le callbacks,
     *  in seguito ad un'operazione che ha cambiato lo stato dei progetti dell'utente.
     *  Cambiamenti di stato dei progetti dell'utente sono: l'utente viene
     *  aggiunto ad un progetto oppure gli viene cancellato un progetto in seguito
     *  all'operazione cancel_project.
     *  Il metodo utilizza la lista degli stub e su ognuno invoca i metodi dell'interfaccia
     *  remota Client che lo nificano dei cambiamenti
     */
    public void updateClientChats() {
        for (Client client : clientsRegisteredForCallback) {
            try {
                client.notifyChatsEvent(worth.getCreatedProjects());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * aggiorna le liste locali di utenti registati degli users/clients registrati per le
     * callbacks, in seguito ad un'operazione che ha cambiato lo stato degli utenti registrati al servizio.
     * I cambiamenti di stato degli utenti registrati sono: registrazione, login e logout di un utente.
     * Il metodo utilizza la lista degli stub e su ognuno invoca i metodi dell'interfaccia
     * remota Client che lo nificano dei cambiamenti
     */
    private void updateClientUsers() {
        for (Client client : clientsRegisteredForCallback) {
            try {
                client.notifyUserEvent(worth.getRegisteredUsers());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
