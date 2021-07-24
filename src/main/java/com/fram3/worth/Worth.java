package com.fram3.worth;

import com.fram3.worth.utils.Message;

/**
 * Worth è l'interfaccia del servizio che include le operazioni offerte
 * ed i tipi di richieste e responsi possibili
 *
 * @author Francesco Amodeo
 * @version 1.0
 */
public interface Worth {

    /**
     * enumerazione di tipi di richieste possibili
     */
    enum RequestType {
        LOGIN,
        LOGOUT,
        LIST_PROJECTS,
        CREATE_PROJECT,
        ADD_MEMBER,
        SHOW_MEMBERS,
        SHOW_CARDS,
        SHOW_CARD,
        ADD_CARD,
        MOVE_CARD,
        CANCEL_PROJECT
    }

    /**
     * enumerazione di tipi di responsi possibili
     */
    enum ResponseType {
        OK,
        USER_EXISTS,            //register
        NOT_REGISTERED,         //login, add membro
        WRONG_PASSW,            //login
        ALREADY_LOGGED,         //login
        PROJECT_EXISTS,         //create_projects
        MEMBER_EXISTS,          //create_projects (utente già membro)
        NONEXISTENT_PROJECT,    //create_projects (progetto non esistente in quelli dell'user)
        NONEXISTENT_CARD,       //show_card, move_card
        NONEXISTENT_LIST,       //move_card
        CARD_EXISTS,            //add_card, move_card
        MOVE_FORBIDDEN,         //move_card
        UNKNOWN_ERROR,          //logout
        CANCEL_FORBIDDEN,       //cancel_project
        UNABLE_CREATE_PROJECT   //create_project (indirizzi multicast esauriti)
    }

    /**
     * effettua il login dell'utente
     *
     * @param nickname nome utente che ha richiesto il login
     * @param password password fornita per accedere
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    Message login(String nickname, String password);

    /**
     * effettua il logout dell'utente
     *
     * @param nickname nome utente che ha richiesto il logout
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    Message logout(String nickname);

    /**
     * costruisce la lista dei progetti di cui l'utente fa parte
     *
     * @param nickname nome utente che ha richiesto la lista dei progetti
     * @return messaggio da inviare al client contenente la lista dei progetti di cui fa parte
     */
    Message listProjects(String nickname);

    /**
     * crea un nuovo progetto
     * e invia un messaggio nella chat di progetto indicando l'operazione eseguita
     *
     * @param nickname nome utente che ha richiesto la creazione del progetto
     * @param projectName nome progetto da creare
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    Message createProject(String nickname, String projectName);

    /**
     * aggiunge un nuovo membro al progetto indicato
     * e invia un messaggio nella chat di progetto indicando l'operazione eseguita
     *
     * @param nickname nome utente che ha richiesto l'aggiunta di un nuovo membro
     * @param projectName nome progetto a cui aggiungere il nuovo membro
     * @param nickNewMember nome utente del membro da aggiungere
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    Message addMember(String nickname, String projectName, String nickNewMember);

    /**
     * recupera la lista dei membri del progetto
     *
     * @param nickname nome utente che ha richiesto la lista dei membri del progetto
     * @param projectName nome progetto del quale è stata richiesta la lista dei membri
     * @return messaggio da inviare al client contenente la lista dei membri del progetto
     */
    Message showMembers(String nickname, String projectName);

    /**
     * recupera la lista di cards del progetto
     *
     * @param nickname nome utente che ha richiesto la lista di cards del progetto
     * @param projectName nome progetto del quale e' stata richiesta la lita di cards
     * @return messaggio da inviare al client contenente la lista delle cards del progetto
     */
    Message showCards(String nickname, String projectName);

    /**
     * recupera la card richiesta
     *
     * @param nickname nome utente che ha richiesto la card
     * @param projectName nome progetto a cui appartiene la card
     * @param cardName nome card richiesta
     * @return messaggio da inviare al client contenente la card richiesta
     */
    Message showCard(String nickname, String projectName, String cardName);

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
    Message addCard(String nickname, String projectName, String cardName, String description);

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
    Message moveCard(String nickname, String projectName, String cardName, String sourceList, String destList);

    /**
     * cancella il progetto, controllando che tutte le card siano in done
     * e invia un messaggio nella chat di progetto indicando l'operazione eseguita
     *
     * @param nickname nome utente che ha richiesto la cancellazione del progetto
     * @param projectName nome progetto da cancellare
     * @return messaggio contentente il responso per l'operazione richiesta
     */
    Message cancelProject(String nickname, String projectName);
}
