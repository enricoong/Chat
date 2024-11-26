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

            String msg = bR.readLine();

            handleMessage(msg);

            bR.close();
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
     */
    private void handleMessage(String message){
        switch (message.substring(0, 3)){
            case "P--" -> {
                setP(new BigInteger(message.substring(3).getBytes()));
            }

            case "G--" -> {
                setG(new BigInteger(message.substring(3).getBytes()));
            }
        }
    }

    public static void setP(BigInteger p) {
        P = p;
    }

    public static void setG(BigInteger g) {
        G = g;
    }
}
