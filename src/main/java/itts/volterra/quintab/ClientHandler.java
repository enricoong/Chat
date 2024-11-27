package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Socket;

/**
 * Gestisce le connessioni in arrivo
 */
public class ClientHandler implements Runnable{
    public static boolean isObjectCreated;
    private static final Logger log = LogManager.getLogger(ClientHandler.class);
    private Socket socket;
    private static BigInteger G, P; // numeri primi

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
                } while (handleMessage(msg) != 1);  //gestisco msg, se errore esco e chiudo connessione

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
     * @return Stato (0 - Errore / 1 - OK)
     */
    private int handleMessage(String message){
        log.debug("Messaggio ricevuto: {}", message);

        if (message == null){
            //ricevuto null, ignoro
            log.debug("Messaggio ignorato perché 'null'");
            return 1;
        } else {    //se il messaggio non è null
            String msgSubstringAfter3 = message.substring(3);   //Contenuto del messaggio dopo le prime 3 cifre

            switch (message.substring(0, 3)){
                case "P--" -> {
                    setP(new BigInteger(msgSubstringAfter3.getBytes()));
                    return 1;
                }

                case "G--" -> {
                    setG(new BigInteger(msgSubstringAfter3.getBytes()));
                    return 1;
                }

                case "TES" ->{
                    log.debug("DAJE ROMA DAJEEEE");
                }
            }
        }

        return 0;   //error
    }

    public static void setP(BigInteger p) {
        P = p;
    }

    public static void setG(BigInteger g) {
        G = g;
    }
}
