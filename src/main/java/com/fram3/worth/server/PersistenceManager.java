package com.fram3.worth.server;

import com.fram3.worth.Card;
import com.fram3.worth.Project;
import com.fram3.worth.User;
import com.fram3.worth.WorthImpl;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

/**
 * PersistenceManager modella il gestore della persistenza dei dati
 * degli utenti e dei progetti del servizio
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
class PersistenceManager {

    /** funzionalità del servizio WORTH */
    private final WorthImpl worth;

    /** nome file che conserva i dati degli utenti registrati al servizio */
    private final String usersFilename;

    /** nome file che conserva i dati dei membri di un progetto */
    private final String membersFilename;

    /** dimensione per l'allocazione dei buffer */
    private final int DIM_BUFFER;

    /** stringa corrispondente al path dove salvare i dati */
    private final String root;
    
    PersistenceManager(WorthImpl worth, String usersFilename, String membersFilename){
        this.worth = worth;
        this.usersFilename = usersFilename + ".json";
        this.membersFilename = membersFilename + ".json";
        root = "src" + File.separator + "main" + File.separator + "resources";
        DIM_BUFFER = 1024 * 8;
    }

    /**
     * effettua una cleanup della directory usata per persistere i dati
     * e la ricrea scrivendo i dati degli utenti e dei progetti */
    void saveResources() {
        Path rootPath = Paths.get(root);
        try {
            if (Files.isDirectory(rootPath))
                //cleanup della directory usata oer salvare i dati
                if(!deleteDirectory(root))
                    throw new IOException("Persistence Manager: Errore cleanup della directory");
            Files.createDirectory(rootPath);
            //scrive il file degli utenti registrati
            writeUsers();
            //crea le directory dei progetti con i relativi file all'interno
            for (Project project : worth.getCreatedProjects())
                createProjectDirectory(project);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * scrive il file degli utenti registrati nella root per la persistenza dei dati
     *
     * @throws IOException propagata da writeFile()
     */
    private void writeUsers() throws IOException {
        synchronized (worth.getRegisteredUsers()) {
            for (User user : worth.getRegisteredUsers()) {
                user.setUsersList(new ArrayList<>());
                user.setChats(new ArrayList<>());
            }
            writeFile(root + File.separator + usersFilename, worth.getRegisteredUsers());
        }
    }

    /**
     * effettua il caricamento dei dati degli utenti e dei progetti
     * salvati nella directory usata per persistere i dati */
    void loadResources() {
        File rootDirectory = new File(root);
        if (!rootDirectory.isDirectory())
            return;
        try {
            loadUsers();
            String[] files = rootDirectory.list();
            assert files != null;
            for (String filename : files) {
                File file = new File(root + File.separator + filename);
                if (file.isDirectory())
                    loadProject(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * carica gli utenti dal file e li inserisce nella lista degli utenti registrati del servizio
     *
     * @throws IOException propagata da readFile()
     */
    private void loadUsers() throws IOException {
        String str = readFile(root + File.separator + usersFilename);
        Gson gson = new Gson();
        User[] users = gson.fromJson(str, User[].class);
        for (User user : users) {
            //all'avvio del server, prima di caricare un utente registrato lo mettiamo offline
            user.setOnline(false);
            worth.getRegisteredUsers().add(user);
        }
    }

    /**
     * carica i dati del progetto dalla relativa directory ricostruendo lo stato
     * del progetto ricreando tutte le card con i relativi dettagli e riempiendo la lista
     * dei membri del progetto. Infine assegna un indirizzo multicast disponibile alla chat di progetto
     *
     * @param projectDirectory directory che contiene i dati del progetto
     * @throws IOException propagata da readFile()
     */
    private void loadProject(File projectDirectory) throws IOException {
        Project project = new Project(projectDirectory.getName());
        // leggo i membri del progetto
        String projectPathName = root + File.separator + projectDirectory.getName();
        String membersPathName = projectPathName + File.separator + membersFilename;
        String str = readFile(membersPathName);
        Gson gson = new Gson();
        String[] members = gson.fromJson(str, String[].class);
        for (String member : members) {
            project.getMembers().add(member);
        }
        // leggo le card del progetto
        String[] files = projectDirectory.list();
        assert files != null;
        for (String filename : files) {
            if (!filename.equals(membersFilename)) {
                str = readFile(projectPathName + File.separator + filename);
                Card card = gson.fromJson(str, Card.class);
                //prendiamo da ogni card l'ultima lista in cui si trovava
                project.parseList(card.getLocation()).add(card);
                project.getCards().add(card);
            }
        }
        //all'avvio del server carico i progetti e assegno nuovi indirizzi di chat ad ognuno
        worth.bindChatAddress(project);
        worth.getCreatedProjects().add(project);
    }

    /**
     * crea la directory del progetto con nome del progetto, crea il file dei membri
     *
     * e un file per ogni card con il rispettivo nome
     * @param project progetto di cui creare la directory
     * @throws IOException errore nella creazione della directory oppure propagata da writeFile()
     */
    private void createProjectDirectory(Project project) throws IOException {
        // creo la directory del progetto
        Path projectPath = Paths.get(root + File.separator + project.getName());
        Files.createDirectory(projectPath);
        // creo un file con tutti i nickname dei membri del progetto
        writeFile(projectPath.toString() + File.separator + membersFilename, project.getMembers());
        // creo i file delle carte
        for (Card card : project.getCards())
            writeFile(projectPath.toString() + File.separator + card.getName() + ".json", card);
    }

    /**
     * cancella ricorsivamente una directory
     *
     * @param filename stringa corrispondente alla directory da cancellare
     * @return true se ha cancellato con successo la directory, false altrimnenti
     */
    private boolean deleteDirectory(String filename) {
        File file = new File(filename);
        if (file.isDirectory()) {
            String[] files = file.list();
            assert files != null;
            for (String newFilename : files) {
                deleteDirectory(filename + File.separator + newFilename);
            }
        }
        return file.delete();
    }

    /**
     * scrive un oggetto in un file nel path indicato, convertendolo prima in json
     *
     * @param pathName path in cui creare il file
     * @param objToWrite oggetto da convertire in json e scrivere
     * @throws IOException errore nelle operazioni di scrittura nel canale
     */
    private void writeFile(String pathName, Object objToWrite) throws IOException {
        Path path = Paths.get(pathName);
        FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        Gson gson = new Gson();
        String str = gson.toJson(objToWrite);
        ByteBuffer byteBuffer = ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8));
        while (byteBuffer.hasRemaining())
            fileChannel.write(byteBuffer);
    }

    /**
     * legge il file nel path indicato
     *
     * @param filename path in cui si trova il file da leggere
     * @return stringa letta dal file
     * @throws IOException errore nelle operazioni di lettura dal canale
     */
    private String readFile(String filename) throws IOException {
        Path path = Paths.get(filename);
        FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        ByteBuffer byteBuffer = ByteBuffer.allocate(DIM_BUFFER);
        StringBuilder stringBuilder = new StringBuilder();
        while (fileChannel.read(byteBuffer) != -1) {
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                stringBuilder.append(StandardCharsets.UTF_8.decode(byteBuffer).toString());
            }
            byteBuffer.clear();
        }
        return stringBuilder.toString();
    }

}
