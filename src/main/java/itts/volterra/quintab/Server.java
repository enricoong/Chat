package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private static final Logger log = LogManager.getLogger(Server.class);
    private static final int PORT =  12345;
    private ServerSocket srvSocket;

    public Server() throws IOException {
        srvSocket = new ServerSocket(PORT);
    }

    @Override
    public void run() {
        while (true){   //ciclo di ascolto
            try {
                log.info("In attesa di connessione...");                //log attesa di connessione
                Socket otherClient = srvSocket.accept();   //attendo richiesta di connessione, bloccante //TODO ERRORE QUI
                log.info("Client connesso!");                           //log connessione del client

                ClientHandler clientHandler = new ClientHandler(otherClient);   //Passo il Socket
                new Thread(clientHandler).start();                              //Avvio il Thread

                {
                    boolean stop = false;
                    while (!stop){  //fermo il ciclo solo in caso di richiesta dall'altro client
                        String otherClientMessage = bR.readLine();      //ricevo e salvo messaggio da altro client
                        log.debug("Messaggio ricevuto: {}", otherClientMessage);    //log ricezione

                        if (otherClientMessage.equals("END_CONNECTION")){   //altro client richiede terminazione connessione
                            stop = true;
                        } else {
                            manageMessage(otherClientMessage, otherClient); //gestisco messaggio
                        }
                    }
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
