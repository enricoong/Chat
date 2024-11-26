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

    @Override
    public void run() {
        int connecitonResult;
        boolean stop = false;

        do {
            System.out.print("Inserisci IP della macchina a cui connettersi ['X' per annullare] >");
            String userInsertedIP = kbInput.nextLine().trim();  //acquisisco input

            if (userInsertedIP.equals("X")){                                            //se utente ha scritto X
                stop = true;                                                            //dichiaro stop = true
            } else {                                                                    //altrimenti proseguo normalmente
                log.info("Tentativo di connessione a {}...", userInsertedIP);
            }

            connecitonResult = connectToServer(userInsertedIP, 12345);              //mi connetto al client
        } while (connecitonResult == 1 && !stop);    //se ritorna codice 1 allora errore e ritento connessione, oppure esco se utente ha deciso di uscire

        if (stop){
            Thread.currentThread().interrupt();     //ferma il Thread attuale
        }

        rsa = new RSA();    //costruisco RSA

        runDiffieHellmanAlgorithm();    //algoritmo Diffie-Hellman
        //roba
    }

    /**
     * Si connette a una macchina e ci comunica
     *
     * @param machineIP IP macchina
     * @param port Porta di connessione
     * @return Codice stato (0 - Errore / 1 - OK)
     */
    private static int connectToServer(String machineIP, int port){
        Socket otherClient;

        try {
            otherClient = new Socket(machineIP, port);   //tento connessione a server
        } catch (UnknownHostException unknownHostException){
            log.warn("Il server non esiste");                   //log errore server inesistente
            return 0;                                           //errore
        } catch (IOException ioException){
            log.warn("Errore I/O durante la connessione");      //log errore connessione
            return 0;                                           //errore
        }
        log.info("Connesso al server!");                        //log connessione del client

        BufferedReader bR;
        try {
            bR = new BufferedReader(new InputStreamReader(otherClient.getInputStream()));    //input stream
        } catch (IOException ioException){
            log.warn("Errore durante la creazione dell'input stream");
            return 0;
        }

        {
            boolean stop = false;
            while (!stop){  //fermo il ciclo solo in caso di richiesta dall'altro client
                String otherClientMessage;

                try {
                    otherClientMessage = bR.readLine();      //ricevo e salvo messaggio da altro client
                } catch (IOException ioException){
                    log.warn("Errore durante la lettura dell'input stream");
                    return 0;
                }
                log.debug("Messaggio ricevuto: {}", otherClientMessage);    //log ricezione

                if (otherClientMessage.equals("END_CONNECTION")){   //altro client richiede terminazione connessione
                    stop = true;
                }
            }
        }
        return 1;   //OK
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