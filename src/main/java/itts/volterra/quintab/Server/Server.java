package itts.volterra.quintab.Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ciclo infinito in attesa di connessioni in entrata, poi inviate a gestire alla classe ClientHandler
 */
public class Server implements Runnable {
    private static final int MAX_CLIENTS = 50;
    private final Logger log = LogManager.getLogger(Server.class);
    private static final int PORT =  12345;
    private ServerSocket srvSocket;
    private static int threadCounter = 0;

    private final ExecutorService threadPool;
    private final Set<ClientHandler> activeClients;

    //variabili per DH
    static final String DP = "fca682ce8e12caba26efccf7110e526db078b05edecbcd1eb4a208f3ae1617ae01f35b91a47e6df63413c5e12ed0899bcd132acd50d99151bdc43ee737592e17";
    static final String DG = "678471b27a9cf44ee91a49c5147db1a9aaf244f05a434d6486931d2d14271b9e35030b71fd73da179069b32e2935630e1c2062354d0da20a6c416e50be794ca42";
    static final BigInteger DEFAULT_P = new BigInteger(DP, 16);  //P
    static final BigInteger DEFAULT_G = new BigInteger(DG, 16);  //G

    public Server() throws IOException {
        srvSocket = new ServerSocket(PORT);
        //inizializzazione thread pool con numero max thread
        threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        //collezione thread-safe per client attivi
        activeClients = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void run() {
        log.info("IP di questa macchina: {}", getIpOfCurrentMachine());

        while (true){   //ciclo di ascolto
            //TODO: ping periodico per vedere se un client è inattivo, poi lo disconnetto
            try {
                log.info("In attesa di connessione...");                        //log attesa di connessione
                Socket otherClient = srvSocket.accept();                        //attendo richiesta di connessione
                log.info("Client connesso!");                                   //log connessione del client

                ClientHandler clientHandler = new ClientHandler(otherClient);   //Passo il Socket

                clientHandler.setServer(this);                                  //configura il ClientHandler per rimuoversi dalla lista quando si disconnette
                activeClients.add(clientHandler);                               //aggiungi alla lista dei client attivi
                Thread thr = new Thread(clientHandler);                         //avvio il Thread
                //thr.setName("CltHnd-" + String.format("%02d", threadCounter));  //do' un nome al thread e il suo counter lo formatto dandogli 2 cifre
                threadPool.execute(thr);                                        //aggiungi alla thread pool e avvia
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

    /**
     * Rimuove un client dalla lista dei client attivi
     *
     * @param client Il ClientHandler da rimuovere
     */
    public void removeClient(ClientHandler client) {
        activeClients.remove(client);
        log.debug("Client rimosso. Client attivi rimasti: {}", activeClients.size());
    }

    /**
     * Restituisce il numero di client attualmente connessi
     *
     * @return Numero di client attivi
     */
    public int getActiveClientCount() {
        return activeClients.size();
    }

    /**
     * Invia un messaggio a tutti i client connessi
     *
     * @param message Il messaggio da inviare
     * @param excludeSender Se true, il mittente del messaggio non lo riceverà
     * @param sender Il ClientHandler che ha inviato il messaggio (può essere null)
     */
    public void broadcastMessage(String message, boolean excludeSender, ClientHandler sender) {
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                //esclude il mittente se excludeSender è true
                if (excludeSender && client == sender) {
                    continue;   //salta l'iterazione attuale
                }

                //invia il messaggio al client
                try {
                    client.sendMessageToClient(message);
                } catch (Exception e) {
                    log.error("Errore durante l'invio del messaggio broadcast a un client", e);
                }
            }
        }
        log.info("Messaggio broadcast inviato a {} client: {}", excludeSender ? activeClients.size() - 1 : activeClients.size(), message);
    }
}
