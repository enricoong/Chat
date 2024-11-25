package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.*;
import java.util.Scanner;

public class Client implements Runnable{

    private static final Logger log = LogManager.getLogger(Client.class);
    private static BigInteger privateKey;   //chiave privata di questo client
    private BigInteger ky;                  //chiave da scambiare al pubblico
    private static BigInteger G, P;                // numeri primi
    private Scanner kbInput = new Scanner(System.in);

    @Override
    public void run() {
        log.debug("IP di questa macchina: {}", getIpOfCurrentMachine());
        log.warn("Inserisci IP della macchina a cui connettersi >");
        String userInsertedIP = kbInput.nextLine().trim();

        connectToClient(userInsertedIP, 12345);
        runDiffieHellmanAlgorithm();
        //roba
    }

    /**
     * Si connette a una macchina e ci comunica
     *
     * @param machineIP IP macchina
     * @param port Porta di connessione
     */
    private static void connectToClient(String machineIP, int port){
        while (true){   //ciclo di ascolto
            try {
                log.info("In attesa di connessione...");                //log attesa di connessione
                Socket otherClient = new ServerSocket(port).accept();   //attendo richiesta di connessione, bloccante
                log.info("Client connesso!");                           //log connessione del client

                BufferedReader bR = new BufferedReader(new InputStreamReader(otherClient.getInputStream()));    //input stream
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

                bR.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gestisce un messaggio ricevuto da un Socket
     *
     * @param message Messagio
     * @param socket Socket
     */
    private static void manageMessage(String message, Socket socket){
        switch (message.substring(0, 3)){
            case "P--" -> {
                setP(new BigInteger(message.substring(3).getBytes()));
            }

            case "G--" -> {
                setG(new BigInteger(message.substring(3).getBytes()));
            }
        }
    }

    /**
     * Scrive nell'output stream di un Socket
     *
     * @param client Socket
     * @param message Messaggio da scrivere
     * @throws IOException Cagati addosso
     */
    private void sendMessageToSocket(Socket client, String message) throws IOException {
        PrintWriter pW = new PrintWriter(client.getOutputStream(), true);  //writer per scrivere
        pW.println(message);        //invio conferma ricezione
        pW.close();                 //chiudo stream
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
     * Metodo che richiama tutti i sotto-metodi che eseguono l'algoritmo di Diffie-Hellman
     */
    private static void runDiffieHellmanAlgorithm(){
        //get from server G and P

        BigInteger anotherKey = generateIntermediateKey(G, privateKey, P);     //genera chiave x

        //now send key to other client
    }

    /**
     * Genera una chiave necessaria per l'algoritmo di Diffie-Hellman
     *
     * @param x Base
     * @param y Potenza
     * @param P Modulo
     * @return Risultato
     */
    private static BigInteger generateIntermediateKey(BigInteger x, BigInteger y, BigInteger P){
        return x.modPow(y, P);
    }

    private static BigInteger generatePrivateKey(BigInteger y){
        return y.modPow(privateKey, P);
    }

    public static void setP(BigInteger p) {
        P = p;
    }

    public static void setG(BigInteger g) {
        G = g;
    }
}