package itts.volterra.quintab.Server;

import itts.volterra.quintab.Features.AES;
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
    private static int threadCounter = 0;
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

    /**
     * Costruttore
     *
     * @param client Socket del client da gestire
     */
    public ClientHandler(Socket client) {
        this.socket = client;
        Thread.currentThread().setName("CltHnd-" + threadCounter);
        threadCounter++;

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
                log.debug("Hash della chiave AES: {}", AESKey.hashCode());

                while (isRunning && !socket.isClosed()) {
                    String message = "EMPTY_MESSAGE";
                    boolean stop = false;
                    do {
                        message = in.readLine();
                        if (message != null){
                            boolean messageOk = true;
                            String decryptedMessage = null;

                            log.debug("Messaggio ricevuto: {}", message);

                            //decifra il messaggio
                            try {
                                decryptedMessage = AES.decrypt(message, AESKey);
                            } catch (Exception e) {
                                //se errore durante decriptazione
                                messageOk = false;
                                log.warn("Errore durante la decriptazione del messaggio ricevuto dal client: {}", e);
                                log.error("Messaggio che ha causato l'errore: {}", message);
                            }

                            //se non ci sono stati errori durante la decriptazione
                            if (messageOk) {
                                log.info("Messaggio ricevuto dal client: {}", decryptedMessage);

                                //elabora il messaggio e invia risposta
                                //processMessage(decryptedMessage);

                                //temporaneo:
                                if (decryptedMessage.equalsIgnoreCase("STOP")){
                                    stop = true;
                                    closeConnection();
                                }
                            }
                        }
                    } while (!stop);
                }
            } catch (IOException e) {
                log.error("Errore durante la comunicazione", e);
            } finally {
                if (isRunning){
                    closeConnection();
                }
            }
        }
    }

    private void closeConnection() {
        isRunning = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            log.info("Connessione chiusa correttamente");
        } catch (IOException e) {
            log.error("Errore durante la chiusura della connessione: ", e);
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
            log.info("Chiave condivisa calcolata: {}", sharedKey);

            // Invia conferma
            out.println("DH-COMPLETE");
        }
    }

    /**
     * Scrive nell'output stream di un Socket
     *
     * @param socket  Socket
     * @param message Messaggio da scrivere
     * @throws IOException Cagati addosso
     */
    private void sendMessageToSocket(Socket socket, String message) {
        PrintWriter pW = null;                                                     //writer per scrivere
        boolean initialized = false;

        while (!initialized) {
            try {
                pW = new PrintWriter(socket.getOutputStream(), true);
                initialized = true;
            } catch (IOException e) {
                log.warn("Errore durante la creazione dell'output stream");
            }
        }

        pW.println(message);                                                //invio conferma ricezione
        //pW.close();                                                         //chiudo stream
    }
}
