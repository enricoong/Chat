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

    private static RSA rsa;
    private static final Logger log = LogManager.getLogger(Client.class);
    private static BigInteger privateKey;   //chiave privata di questo client
    private BigInteger ky;                  //chiave da scambiare al pubblico
    private static BigInteger G, P;                // numeri primi
    private Scanner kbInput = new Scanner(System.in);
    private Socket socket;

    @Override
    public void run() {
       try {
          Thread.sleep(500);    //sleep perché il messaggio in console appaia senza essere in mezzo ad altre righe
       } catch (InterruptedException e) {
          log.warn("Il Thread è stato interrotto durante lo sleep");
       }

       int connecitonResult = 0;
        boolean stop = false;

        do {
            System.out.print("[CLIENT] Inserisci IP della macchina a cui connettersi ['X' per annullare] >");
            String userInsertedIP = kbInput.nextLine().trim();  //acquisisco input

            if (userInsertedIP.equalsIgnoreCase("X")){                                            //se utente ha scritto X
                log.debug("L'utente ha scelto di annullare");
                stop = true;                                                            //dichiaro stop = true
            } else {                                                                    //altrimenti proseguo normalmente
                log.info("Tentativo di connessione a {}...", userInsertedIP);
                connecitonResult = connectToServer(userInsertedIP, 12345);              //mi connetto al client
            }
        } while (connecitonResult == 0 && !stop);    //se ritorna codice 0 allora errore e ritento connessione, oppure esco se utente ha deciso di uscire

        if (stop){
            Thread.currentThread().interrupt();     //ferma il Thread attuale
        } else {
            //listenForServerMessage();

            log.debug("Sto per inviare il messaggio...");
            sendMessageToSocket(socket, "TEST");

            rsa = new RSA();    //costruisco RSA

            runDiffieHellmanAlgorithm();    //algoritmo Diffie-Hellman
            //roba
        }
    }

    /**
     * Si connette a una macchina e ci comunica
     *
     * @param machineIP IP macchina
     * @param port Porta di connessione
     * @return Codice di stato (0 - Errore / 1 - OK)
     */
    private int connectToServer(String machineIP, int port){
        try {
            socket = new Socket(machineIP, port);          //tento connessione a server
        } catch (UnknownHostException unknownHostException){
            log.warn("Il server non esiste");                   //log errore server inesistente
            return 0;                                           //errore
        } catch (IOException ioException){
            log.warn("Errore I/O durante la connessione");      //log errore connessione
            return 0;                                           //errore
        }
        log.info("Connesso al server!");                        //log connessione del client

        return 1;   //OK
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
                log.warn("Errore durante la creazione dell'output stream al server");
            }
        }

        pW.println(message);                                                //invio conferma ricezione
        pW.close();                                                         //chiudo stream
    }

    /**
     * Listener di messaggi dal server, quando riceve 'END_CONNECTION' termina di ascoltare
     *
     * @return Codice di stato (0 - Errore / 1 - OK)
     */
    private int listenForServerMessage(){
        BufferedReader bR;
        try {
            bR = new BufferedReader(new InputStreamReader(socket.getInputStream()));    //input stream
        } catch (IOException ioException){
            log.warn("Errore durante la creazione dell'input stream");
            return 0;                                       //fermo tentativo ascolto
        }

        boolean stop = false;
        while (!stop){                                      //fermo il ciclo solo in caso di richiesta dall'altro client
            String otherClientMessage;

            try {
                otherClientMessage = bR.readLine();         //ricevo e salvo messaggio da altro client
            } catch (IOException ioException){
                log.warn("Errore durante la lettura dell'input stream");
                return 0;                                   //fermo tentativo ascolto
            }

            log.debug("Messaggio ricevuto: {}", otherClientMessage);    //log ricezione

            if (otherClientMessage.equals("END_CONNECTION")){   //altro client richiede terminazione connessione
                stop = true;                                    //esco dal ciclo
            }
        }

       try {
          bR.close();
       } catch (IOException e) {
          log.warn("Errore durante la chiusura dell'input stream");
       }
       return 1;
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