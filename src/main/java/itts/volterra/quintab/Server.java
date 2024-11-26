package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Ciclo infinito in attesa di connessioni in entrata, poi inviate a gestire alla classe ClientHandler
 */
public class Server implements Runnable {
    private static final Logger log = LogManager.getLogger(Server.class);
    private static final int PORT =  12345;
    private ServerSocket srvSocket;

    public Server() throws IOException {
        srvSocket = new ServerSocket(PORT);
    }

    @Override
    public void run() {
        log.debug("IP di questa macchina: {}", getIpOfCurrentMachine());

        while (true){   //ciclo di ascolto
            try {
                log.info("In attesa di connessione...");                        //log attesa di connessione
                Socket otherClient = srvSocket.accept();                        //attendo richiesta di connessione
                log.info("Client connesso!");                                   //log connessione del client

                ClientHandler clientHandler = new ClientHandler(otherClient);   //Passo il Socket
                new Thread(clientHandler).start();                              //Avvio il Thread
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Mi dice l'IP della macchina attuale
     *
     * @return IP della macchina attuale
     */
    private String getIpOfCurrentMachine() {
        try (final DatagramSocket datagramSocket = new DatagramSocket()){
            datagramSocket.connect(InetAddress.getByName("1.1.1.1"), 12345);
            return datagramSocket.getLocalAddress().getHostAddress();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;    //non dovrebbe arrivare qui ma ora non ho tempo per gestire bene le eccezioni
    }
}
