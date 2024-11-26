package itts.volterra.quintab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Socket;

/**
 * Gestisce le connessioni in arrivo
 */
public class ClientHandler implements Runnable{
    private Socket socket;
    private static BigInteger G, P; // numeri primi

    @Override
    public void run() {
        try {
            BufferedReader bR = new BufferedReader(new InputStreamReader(socket.getInputStream()));    //input stream

            //TODO ERRORE QUI - il msg è null

            String msg;
            do {
                msg = bR.readLine();            //leggo messaggio da client
            } while (handleMessage(msg) != 1);  //gestisco msg, se errore esco e chiudo connessione

            bR.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ClientHandler(Socket client) {
        this.socket = client;
    }

    /**
     * Gestisco il messaggio
     *
     * @param message Messaggio
     * @return Stato (0 - Errore / 1 - OK)
     */
    private int handleMessage(String message){
        switch (message.substring(0, 3)){
            case "P--" -> {
                setP(new BigInteger(message.substring(3).getBytes()));
                return 1;
            }

            case "G--" -> {
               setG(new BigInteger(message.substring(3).getBytes()));
                return 1;
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
