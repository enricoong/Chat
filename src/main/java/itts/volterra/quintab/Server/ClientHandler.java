package itts.volterra.quintab.Server;

import itts.volterra.quintab.Encryption.AES;
import itts.volterra.quintab.MessageSerialization.JsonHandler;
import itts.volterra.quintab.MessageSerialization.Message;
import itts.volterra.quintab.Server.Database.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;

//importo G e P dal Server
import static itts.volterra.quintab.Server.Server.DEFAULT_G;
import static itts.volterra.quintab.Server.Server.DEFAULT_P;

/**
 * Gestisce le connessioni in arrivo
 */
public class ClientHandler implements Runnable {
    public boolean isObjectCreated;
    private final Logger log = LogManager.getLogger(ClientHandler.class);
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private BigInteger serverPrivateKey;
    private BigInteger serverPublicKey;
    private BigInteger sharedKey;
    private SecretKey AESKey;
    private volatile boolean isRunning = true;
    private String tempUsername;    //salvo temporaneamente lo username con cui il client sta cercando di accedere
    private String currentUser = null;  //salvo il nome dello username loggato (se presente)
    private Server server;  //riferimento al server

    /**
     * Costruttore
     *
     * @param client Socket del client da gestire
     */
    public ClientHandler(Socket client) {
        this.client = client;

        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            //genera chiave privata del server
            serverPrivateKey = generatePrivateKey();

            //set the flag to true if everything goes successfully (it won't)
            this.isObjectCreated = true;
        } catch (IOException e) {
            log.error("Errore durante l'inizializzazione del gestore client", e);
            //set the flag to false if initialization fails (as always does)
            this.isObjectCreated = false;
        } finally {
            log.debug("Oggetto CH creato: {}", isObjectCreated);
        }
    }

    @Override
    public void run() {
        if (!isObjectCreated) {      //se il costruttore non è stato eseguito
            log.warn("L’oggetto ClientHandler non è stato costruito con successo");
        } else {
            try {
                //gestisce Diffie-Hellman
                runDiffieHellmanAlgorithm();

                log.debug("Shared key bytes length: {}", sharedKey.toByteArray().length);
                AESKey = AES.generateKeyForAES(sharedKey);
                log.debug("AES key format: {}", AESKey.getFormat());
                log.debug("AES key encoded length: {}", AESKey.getEncoded().length);
                log.debug("Chiave AES: {}", java.util.Arrays.hashCode(AESKey.getEncoded()));
                log.info("Chiave AES creata con successo");

                boolean stop = false;
                do {
                    Message deserializedMessage = waitAndDecryptClientMessage();

                    if (deserializedMessage != null){
                        log.debug("Messaggio ricevuto da '{}': '{}'", deserializedMessage.getUsername(), deserializedMessage.getMessage());

                        if (deserializedMessage.getMessage() != null) {
                            if (deserializedMessage.getMessage().equalsIgnoreCase("STOP")){
                                stop = true;
                                closeConnection();
                            } else {
                                //elabora il messaggio e invia risposta
                                processMessage(deserializedMessage.getMessage());
                            }
                        } else {
                            log.warn("Il messaggio ricevuto è 'null'");
                        }
                    } else {
                       log.warn("Messaggio ricevuto vuoto, o errore durante decriptazione/deserializzazione");
                    }
                } while (!stop && isRunning && !client.isClosed());
            } catch (IOException e) {
                log.error("Errore durante la comunicazione: {}", e.getMessage());
            } finally {
                if (isRunning){
                    closeConnection();
                }
            }
        }
    }

    /**
     * Imposta il riferimento al server
     *
     * @param server L'istanza del server
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Restituisce lo username dell'utente corrente
     *
     * @return Username dell'utente corrente o null se non autenticato
     */
    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Chiude la connessione Server-Client, con i dovuti controlli
     */
    protected void closeConnection() {
        if (isRunning){
            isRunning = false;
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (client != null && !client.isClosed()) client.close();

                //rimuove questo client dalla lista dei client attivi nel server
                if (server != null) {
                    server.removeClient(this);
                }

                log.info("Connessione chiusa correttamente");
            } catch (IOException e) {
                log.error("Errore durante la chiusura della connessione: {}", e.getMessage());
            }
        } else {
            log.warn("Si è tentato di chiudere la connessione, ma essa è già chiusa");
        }
    }

    /**
     * Genero la chiave privata
     *
     * @return Chiave privata
     */
    private BigInteger generatePrivateKey() {
        return new BigInteger(1024, new SecureRandom());
    }

    /**
     * Metodo che esegue l'algoritmo di Diffie-Hellman
     */
    private void runDiffieHellmanAlgorithm() throws IOException {
        /*
        Nel messaggio, i primi 3 char servono per capire cosa si vuole fare: DH = si sta eseguendo Diffie-Hellman,
        poi si usa '--' come carattere di separazione
         */

        //invio parametri pubblici al client
        out.println("DH-P--" + DEFAULT_P);
        out.println("DH-G--" + DEFAULT_G);

        //calcolo chiave pubblica del server
        serverPublicKey = DEFAULT_G.modPow(serverPrivateKey, DEFAULT_P);

        //cripto la chiave pubblica con RSA
        //BigInteger encryptedServerPublicKey = rsa.encrypt(serverPublicKey);
        out.println("DH-SERVER_PUBLIC--" + serverPublicKey);

        //attendo la chiave pubblica del client
        String clientPublicKeyStr = null;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("DH-CLIENT_PUBLIC--")) {
                clientPublicKeyStr = line.substring(18);
                break;
            }
        }

        if (clientPublicKeyStr != null) {
            //decripto la chiave pubblica del client
            BigInteger clientPublicKey = new BigInteger(clientPublicKeyStr);
            //BigInteger clientPublicKey = rsa.decrypt(encryptedClientPublicKey);

            //calcolo la chiave condivisa
            sharedKey = clientPublicKey.modPow(serverPrivateKey, DEFAULT_P);
            log.debug("Chiave condivisa calcolata: {}", sharedKey);

            // Invia conferma
            out.println("DH-COMPLETE");
        }
    }

    /**
     * Handles the message received from a Client
     *
     * @param message Message received
     */
    private void processMessage(String message) {
        if (currentUser == null) {
            //al momento non è loggato alcun utente

            if (message.startsWith("USRNM-")) {
                //il client ha inviato uno username
                if (Database.usernameExists(message.substring(6))){
                    tempUsername = message.substring(6); //salvo temporaneamente lo username
                    log.info("Cercato username '{}': trovato nel database", message.substring(6));
                    sendMessageToClient("USERNAME-OK");  //invio messaggio ok al client
                } else {
                    log.info("Cercato username '{}': non è presente nel database", message.substring(6));
                    sendMessageToClient("USERNAME-NOTFOUND");    //invio messaggio notfound al client
                }
            } else if (message.startsWith("PSSWD-")) {
                String pwHash = Database.getPasswordHash(tempUsername);

                //controllo se la password inserita corrisponde a quella dell'utente selezionato
                if (pwHash != null && pwHash.equals(message.substring(6))){
                    //la password ricevuta è corretta
                    log.info("La password ricevuta dal client è corretta");
                    sendMessageToClient("PASSWORD-OK");
                    currentUser = tempUsername; //imposto utente loggato
                    tempUsername = null;    //azzero utente temporaneo
                } else {
                    //la password non è corretta
                    log.info("La password ricevuta dal client è errata");
                    sendMessageToClient("PASSWORD-WRONG");
                }
            } else {
                log.error("Si sta tentando di comunicare senza aver effettuato l'autenticazione, messaggio causa: '{}'", message);
            }
        } else {
            //c'è già un utente loggato
            //TODO: gestione comandi

            //mi salvo tutti i prefissi
            String  msgPrefix = "msg",
                    broadcastPrefix = "broadcast";

            //gestisco tutte le possibilità dei prefissi, e per ognuna mi calcolo
            // il 'postSpaceContent' sommando lunghezza del prefisso e 2 (ovvero i caratteri '/' e lo spazio post prefix)
            if (message.startsWith('/' + msgPrefix + ' ')){
                String postSpaceContent = message.substring(2 + msgPrefix.length());

                if (server != null){
                    //ora devo capire per chi è il messaggio
                    //TODO fixare controlli di presenza spazi
                    String receiverUsername = postSpaceContent.substring(0, postSpaceContent.indexOf(' '));   //substring da inizio content a prossimo spazio
                    String messageToSend = postSpaceContent.substring(postSpaceContent.indexOf(' ') + 1);
                    log.debug("Messaggio da inviare al client '{}': '{}'", receiverUsername, messageToSend);

                    server.sendMessageToClient(receiverUsername, messageToSend);
                } else {
                    log.warn("Si è tentato di inviare un messaggio privato, ma il server non è stato collegato");
                }
            } else if (message.startsWith('/' + broadcastPrefix + ' ')) {
                String postSpaceContent = message.substring(2 + broadcastPrefix.length());

                if (server != null){
                    server.broadcastMessage(postSpaceContent, false, this);
                } else {
                    log.warn("Si è tentato di inviare un messaggio broadcast, ma il server non è stato collegato");
                }
            }
        }
    }

    /**
     * Scrive nell'output stream di un Socket
     *
     * @param message Messaggio da scrivere
     */
    public void sendMessageToClient(String message) {
        if (isRunning){
           try {
              //serializzo il messaggio
              String serializedMessage = JsonHandler.serialize(new Message("SERVER", message, System.currentTimeMillis()));
              //cripto il messaggio
              String encryptedMessage = AES.encrypt(serializedMessage, AESKey);
              //invio il messaggio al client
              out.println(encryptedMessage);
           } catch (Exception e) {
               log.error("Errore durante l'invio di un messaggio, {}", e.getMessage());
               log.debug("Errore durante l'invio del seguente messaggio: {}", message);
           }
        } else {
            log.error("Errore durante l'invio di un messaggio al client: la connessione è terminata");
        }
    }

    private Message waitAndDecryptClientMessage() {
        if (!isRunning){
            log.error("Si sta attendendo un messaggio dal client, ma la connessione è già stata terminata");
            closeConnection();
            return null;
        } else {
            String encryptedMessage = null;

            try {
                while ((encryptedMessage = in.readLine()) == null){
                    /*wait for a message todo: better thread management*/
                    try {
                        Thread.sleep(100);  //per evitare il busy-waiting
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Thread interrotto durante l'attesa del messaggio", e);
                    }
                }
            } catch (IOException e) {
                log.error("Errore durante l'attesa del messaggio: {}. La connessione verrà terminata.", e.getMessage());
                closeConnection();
            }

            log.debug("Messaggio ricevuto (grezzo): {}", encryptedMessage);

            String decryptedMessage = null;
            try {
                decryptedMessage = AES.decrypt(encryptedMessage, AESKey);
            } catch (Exception e) {
                log.error("Errore durante la decriptazione del messaggio: {}", e.getMessage());
                return null;
            }

            log.debug("Messaggio ricevuto (decriptato, pre-deserializzazione): {}", decryptedMessage);

            try {
                return JsonHandler.deserialize(decryptedMessage);
            } catch (IOException e) {
                log.error("Errore durante la deserializzazione del messaggio: {}", e.getMessage());
            }

            return null;
        }
    }
}
