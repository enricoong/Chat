package itts.volterra.quintab.Server;

import itts.volterra.quintab.Features.AES;
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
    private Socket socket;
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
        this.socket = client;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

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

                while (isRunning && !socket.isClosed()) {
                    String message = "EMPTY_MESSAGE";
                    boolean stop = false;
                    do {
                        message = in.readLine();
                        if (message != null){
                            boolean messageOk = true;
                            String decryptedMessage = null;

                            log.debug("Messaggio ricevuto (grezzo): {}", message);

                            //decifra il messaggio
                            try {
                                decryptedMessage = AES.decrypt(message, AESKey);
                            } catch (Exception e) {
                                //se errore durante decriptazione
                                messageOk = false;
                                log.warn("Errore durante la decriptazione del messaggio ricevuto dal client: {}", e.getMessage());
                                log.error("Messaggio che ha causato l'errore: {}", message);
                            }

                            //se non ci sono stati errori durante la decriptazione
                            if (messageOk) {
                                log.debug("Messaggio ricevuto: {}", decryptedMessage);

                                if (decryptedMessage.equalsIgnoreCase("STOP")){
                                    stop = true;
                                    closeConnection();
                                } else {
                                    //elabora il messaggio e invia risposta
                                    processMessage(decryptedMessage);
                                }
                            }
                        }
                    } while (!stop);
                }
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
     * Chiude la connessione Server-Client, con i dovuti controlli
     */
    private void closeConnection() {
        if (isRunning){
            isRunning = false;
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();

                //rRimuove questo client dalla lista dei client attivi nel server
                if (server != null) {
                    server.removeClient(this);
                }

                log.info("Connessione chiusa correttamente");
            } catch (IOException e) {
                log.error("Errore durante la chiusura della connessione: ", e);
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
            }
        } else {
            //c'è già un utente loggato

            //temp:
            log.info("Messaggio ricevuto: {}", message);

            //TODO, temp:

            // Gestione dei messaggi per utenti loggati
            if (message.startsWith("BROADCAST-")) {
                // Gestione dei messaggi di broadcast dagli utenti
                String broadcastContent = message.substring(10);

                // Prepara il messaggio di broadcast con lo username del mittente
                String broadcastMessage = "MSG-" + currentUser + "-" + broadcastContent;

                // Invia il messaggio a tutti gli altri client
                if (server != null) {
                    server.broadcastMessage(broadcastMessage, true, this);
                    log.info("Broadcast da '{}': {}", currentUser, broadcastContent);
                }
            } else if (message.startsWith("PRIVATE-")) {
                // Esempio di gestione di messaggi privati (formato: "PRIVATE-destinatario-contenuto")
                // Questa è una funzionalità che potrebbe essere implementata in futuro
                log.info("Messaggio privato ricevuto da {}: {}", currentUser, message);
                sendMessageToClient("SYSTEM-PRIVATE_NOT_IMPLEMENTED");
            } else {
                // Altri messaggi non riconosciuti
                log.info("Messaggio non riconosciuto da '{}': {}", currentUser, message);
                sendMessageToClient("SYSTEM-UNKNOWN_COMMAND");
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
              String encryptedMessage = AES.encrypt(message, AESKey);   //cripto il messaggio
              out.println(encryptedMessage);        //invio il messaggio al client
           } catch (Exception e) {
              log.error("Errore durante l'invio di un messaggio", e);
              log.debug("Errore durante l'invio del seguente messaggio: {}", message, e);
           }
        } else {
            log.error("Errore durante l'invio di un messaggio al client: la connessione è terminata");
        }
    }
}
