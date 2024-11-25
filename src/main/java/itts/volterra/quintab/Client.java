package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

public class Client implements Runnable{

    private static final Logger log = LogManager.getLogger(Client.class);
    private static BigInteger privateKey;   //chiave privata di questo client
    private BigInteger ky;                  //chiave da scambiare al pubblico
    private BigInteger G, P;                // numeri primi

    @Override
    public void run() {
        //connessione a altro client
        runDiffieHellmanAlgorithm();
        //roba
    }

    private static Socket connectToClient(String machineIP, int port){
        while (true){   //ciclo di ascolto
            try {
                log.info("In attesa di connessione...");                //log attesa di connessione
                Socket otherClient = new ServerSocket(port).accept();   //attendo richiesta di connessione, bloccante
                log.info("Client connesso!");                           //log connessione del client

                BufferedReader bR = new BufferedReader(new InputStreamReader(otherClient.getInputStream()));    //listener
                String otherClientMessage = bR.readLine();              //ricevo e salvo messaggio da altro client
                log.debug("Messaggio ricevuto: {}", otherClientMessage);

                PrintWriter pW = new PrintWriter(otherClient.getOutputStream(), true);  //writer per scrivere
                pW.println("Messaggio ricevuto!");  //invio conferma ricezione

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void runDiffieHellmanAlgorithm(){
        //get from server G and P

        BigInteger anotherKey = generateIntermediateKey(G, privateKey, P);     //genera chiave x

        //now send key to other client
    }

    private static BigInteger generateIntermediateKey(BigInteger x, BigInteger y, BigInteger P){
        return x.modPow(y, P);
    }

    private static BigInteger generatePrivateKey(BigInteger y){
        return y.modPow(privateKey, P);
    }
}