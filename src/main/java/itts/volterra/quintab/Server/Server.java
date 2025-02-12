package itts.volterra.quintab.Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Ciclo infinito in attesa di connessioni in entrata, poi inviate a gestire alla classe ClientHandler
 */
public class Server implements Runnable {
    private final Logger log = LogManager.getLogger(Server.class);
    private static final int PORT =  12345;
    private ServerSocket srvSocket;
    private static int threadCounter = 0;

    //variabili per DH
    static final String DP = "fca682ce8e12caba26efccf7110e526db078b05edecbcd1eb4a208f3ae1617ae01f35b91a47e6df63413c5e12ed0899bcd132acd50d99151bdc43ee737592e17";
    static final String DG = "678471b27a9cf44ee91a49c5147db1a9aaf244f05a434d6486931d2d14271b9e35030b71fd73da179069b32e2935630e1c2062354d0da20a6c416e50be794ca42";
    static final BigInteger DEFAULT_P = new BigInteger(DP, 16);  //P
    static final BigInteger DEFAULT_G = new BigInteger(DG, 16);  //G

    public Server() throws IOException {
        srvSocket = new ServerSocket(PORT);
    }

    @Override
    public void run() {
        log.info("IP di questa macchina: {}", getIpOfCurrentMachine());

        while (true){   //ciclo di ascolto
            try {
                log.info("In attesa di connessione...");                        //log attesa di connessione
                Socket otherClient = srvSocket.accept();                        //attendo richiesta di connessione
                log.info("Client connesso!");                                   //log connessione del client

                ClientHandler clientHandler = new ClientHandler(otherClient);   //Passo il Socket
                Thread thr = new Thread(clientHandler);                         //Avvio il Thread
                thr.setName("CltHnd-" + String.format("%02d", threadCounter));  //do' un nome al thread e il suo counter lo formatto dandogli 2 cifre
                thr.start();
                threadCounter++;
            } catch (IOException e) {
                log.error("Errore durante l'accettazione della connessione", e);
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
