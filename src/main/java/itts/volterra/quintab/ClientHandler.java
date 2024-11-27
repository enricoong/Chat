package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Gestisce le connessioni in arrivo
 */
public class ClientHandler implements Runnable{
    public static boolean isObjectCreated;
    private static final Logger log = LogManager.getLogger(ClientHandler.class);
    private Socket socket;

    @Override
    public void run() {
        if (!isObjectCreated){      //se il costruttore non è stato eseguito
            log.warn("L’oggetto ClientHandler non è stato costruito con successo");
        } else {
            try {
                log.debug("Lettura dell'input stream dal Client in corso...");
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));    //input stream

                String msg;
                do {
                    msg = in.readLine();            //leggo messaggio da client
                } while (handleMessage(msg, socket) != 1);  //gestisco msg, se errore esco e chiudo connessione

                in.close();
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ClientHandler(Socket client) {
        isObjectCreated = true;
        this.socket = client;
    }

    /**
     * Gestisco il messaggio
     *
     * @param message Messaggio
     * @param socket Socket mittente
     * @return Stato (0 - Errore / 1 - OK)
     */
    private int handleMessage(String message, Socket socket){
        log.debug("Messaggio ricevuto: {}", message);

        if (message == null){
            //ricevuto null, ignoro
            log.debug("Messaggio ignorato perché 'null'");
            return 1;
        } else {    //se il messaggio non è null
            String msgSubstringAfter3 = message.substring(3);   //Contenuto del messaggio dopo le prime 3 cifre

            switch (message.substring(0, 3)){
                case "DH-" -> {
                    manageDiffieHellman(msgSubstringAfter3, socket);    //l'utente sta tentando di eseguire operazioni riguardanti DH
                    return 1;
                }
            }
        }

        return 0;   //error
    }

    /**
     * Se l'utente deve eseguire operazioni riguardanti DH, questo metodo viene eseguito, gestendo la richiesta utente
     *
     * @param messageContent Messaggio dell'utente (esclusa la prima parte "DH-")
     */
    private void manageDiffieHellman(String messageContent, Socket socket){
        switch (messageContent.substring(0, 5)){
            case "START" ->{        //l'utente vuole iniziare il protocollo
                sendMessageToSocket(socket, "P--" + 1234); //invio valore di P al client
                //TODO
            }

            case "STOP-" ->{
                sendMessageToSocket(socket, "STOP");
            }

            //TODO
        }
    }

    /**
     * Scrive nell'output stream di un Socket
     *
     * @param socket Socket
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
        pW.close();                                                         //chiudo stream
    }
}
