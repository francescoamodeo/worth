package com.fram3.worth.server;

import com.fram3.worth.Worth;
import com.fram3.worth.utils.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

/**
 * RequestHandler modella il task che elabora le richieste dei client incaricate dal server
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
class RequestHandler implements Runnable {

    /** interfaccia delle funzionalità del servizio worth */
    private final Worth worth;

    /** chiave relativa al socket channel del client di cui gestire la richiesta */
    private final SelectionKey client;

    /** richiesta letta dal socket channel del client da gestire */
    private final Message message;

    RequestHandler(Worth worth, ByteBuffer byteBuffer, SelectionKey client) {
        StringBuilder stringBuilder = new StringBuilder();
        while (byteBuffer.hasRemaining())
            stringBuilder.append(StandardCharsets.UTF_8.decode(byteBuffer).toString());
        Gson gson = new Gson();
        //deserializzo la richiesta del client
        this.message = gson.fromJson(stringBuilder.toString(), Message.class);
        this.worth = worth;
        this.client = client;
    }

    /**
     * verifica la corrispondenza della richiesta letta con quelle accettate dal servizio
     * e invoca le funzionalità di quest'ultimo per risolverla.
     * Una volta ricevuto il responso lo mette nell'attachment della chiave, inserisce nelle
     * operazioni di interesse quella di scrittura e risveglia il selettore.
     * Quando la richiesta è un'operazione di logout l'handler finalizza la connessione scrivendo
     * direttamente sul canale il responso e chiudendo la connessione
     */
    @Override
    public void run() {
        Message replyMessage;
        //verifico il tipo di richiesta e invoco i metodi del servizio worth
        switch (this.message.getRequest()) {
            case LOGIN:
                replyMessage = worth.login(this.message.getNickname(), this.message.getPassword());
                break;

            case LOGOUT:
                replyMessage = worth.logout(this.message.getNickname());
                finalizeConnection(replyMessage);
                return;

            case LIST_PROJECTS:
                replyMessage = worth.listProjects(this.message.getNickname());
                break;

            case CREATE_PROJECT:
                replyMessage = worth.createProject(this.message.getNickname(), this.message.getProjectName());
                break;

            case ADD_MEMBER:
                replyMessage = worth.addMember(this.message.getNickname(), this.message.getProjectName(),
                        this.message.getNewMember());
                break;

            case SHOW_MEMBERS:
                replyMessage = worth.showMembers(this.message.getNickname(), this.message.getProjectName());
                break;

            case SHOW_CARDS:
                replyMessage = worth.showCards(this.message.getNickname(), this.message.getProjectName());
                break;

            case SHOW_CARD:
                replyMessage = worth.showCard(this.message.getNickname(), this.message.getProjectName(),
                        this.message.getCardName());
                break;

            case ADD_CARD:
                replyMessage = worth.addCard(this.message.getNickname(), this.message.getProjectName(),
                        this.message.getCardName(), this.message.getDescription());
                break;

            case MOVE_CARD:
                replyMessage = worth.moveCard(this.message.getNickname(), this.message.getProjectName(),
                        this.message.getCardName(), this.message.getSourceList(),
                        this.message.getDestList());
                break;

            case CANCEL_PROJECT:
                replyMessage = worth.cancelProject(this.message.getNickname(), this.message.getProjectName());
                break;

            default:
                throw new IllegalArgumentException("Malformed request: " + this.message.getRequest());
        }

        //inserisco nell'attachment il response
        attach(replyMessage);
        //imposto sul canale del client l'interesse alla scrittura per inviare il responso
        client.interestOps(SelectionKey.OP_WRITE);
        //risveglio il selettore
        client.selector().wakeup();
    }

    /**
     * inserisce nell'attachment il responso della richiesta del client.
     * L'attachment è composto da un array di ByteBuffer, nel primo andrà
     * scritta la dimensione del messaggio spedito, nell'altro il messaggio
     * vero e proprio. Successivamente verrà scritto dal server con una
     * gathering write che scrive in una singola invocazione una sequenza di ByteBuffers
     *
     * @param replyMessage responso dell'operazione richiesta dal client
     */
    private void attach(Message replyMessage){
        Gson gson = new Gson();
        String str = gson.toJson(replyMessage);
        byte[] byteArray = str.getBytes(StandardCharsets.UTF_8);
        //prendo l'array di byte inizializzato nell'attachment durante la registrazione per READ
        ByteBuffer[] buffers = (ByteBuffer[]) client.attachment();
        //buffer dimensione messaggio
        buffers[0].clear();
        buffers[0].putInt(byteArray.length);
        buffers[0].flip();
        //buffer messaggio
        buffers[1] = ByteBuffer.wrap(byteArray);
    }

    /**
     * finalizza la connessione scrivendo direttamente il responso sul socket channel,
     * chiudendo la connessione e cancellando la chiave dal selettore
     *
     * @param replyMessage responso dell'operazione richiesta dal client
     */
    private void finalizeConnection(Message replyMessage){
        try { 
            System.out.println("RequestHandler-"+Thread.currentThread().getName()+": "+
                "chiudo la connessione con il client");
            Gson gson = new Gson();
            String str = gson.toJson(replyMessage);
            //preparo i due buffer da scrivere con una gathering write
            byte[] byteArray = str.getBytes(StandardCharsets.UTF_8);
            //buffer che conterrà la dimensione del messaggio
            ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
            sizeBuffer.putInt(byteArray.length);
            sizeBuffer.flip();
            //buffer che conterrà il messaggio
            ByteBuffer dataBuffer = ByteBuffer.wrap(byteArray);
            SocketChannel clientChannel = (SocketChannel) client.channel();
            //array di bytebuffer da scrivere
            ByteBuffer[] buffers = new ByteBuffer[] { sizeBuffer, dataBuffer};
            //completo la write finchè ci sono bytes nei buffer
            //così invio tutto per l'ultima volta e chiudio la connessione
            while(buffers[0].hasRemaining() && buffers[1].hasRemaining())
                clientChannel.write(new ByteBuffer[] {buffers[0], buffers[1]});

            //chiudo la connessione
            //close fa anche il cancel della chiave
            client.channel().close();
            //risveglio il selettore per far cancellare definitivamente la chiave
            client.selector().wakeup();
        } catch (IOException e) {
         e.printStackTrace(); }
    }
}
