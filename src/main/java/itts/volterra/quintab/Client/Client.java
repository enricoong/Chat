package itts.volterra.quintab.Client;

import itts.volterra.quintab.Features.AES;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.*;
import java.security.SecureRandom;
import java.util.Scanner;

public class Client implements Runnable {

   private static final Logger log = LogManager.getLogger(Client.class);
   private BufferedReader in;
   private PrintWriter out;
   private Scanner kbInput = new Scanner(System.in);
   private Socket server;

   //parametri Diffie-Hellman
   private BigInteger P, G;
   private BigInteger clientPrivateKey;
   private BigInteger clientPublicKey;
   private BigInteger sharedKey;
   private SecretKey AESKey;

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
         System.out.print("[CLIENT] Inserisci IP della macchina a cui connettersi ['X' per annullare] > ");
         String userInsertedIP = kbInput.nextLine().trim();  //acquisisco input

         if (userInsertedIP.equalsIgnoreCase("X")) {                                            //se utente ha scritto X
            log.debug("L'utente ha scelto di annullare");
            stop = true;                                                            //dichiaro stop = true
         } else {                                                                    //altrimenti proseguo normalmente
            log.info("Tentativo di connessione a {}...", userInsertedIP);
            connecitonResult = connectToServer(userInsertedIP, 12345);              //mi connetto al client
         }
      } while (connecitonResult == 0 && !stop);    //se ritorna codice 0 allora errore e ritento connessione, oppure esco se utente ha deciso di uscire

      if (!stop) {
         try {
            runDiffieHellmanAlgorithm();
            AESKey = AES.generateKeyForAES(sharedKey);
            log.debug("Chiave AES: {}", AESKey.hashCode());

            sendMessageToSocket(server, "PROVA");

            //continua
         } catch (IOException e) {
            log.error("Errore durante lo scambio Diffie-Hellman", e);
         }
      }

      kbInput.close();                    //chiudo scanner
      Thread.currentThread().interrupt(); //fermo thread
   }

   /**
    * Si connette a una macchina e ci comunica
    *
    * @param machineIP IP macchina
    * @param port      Porta di connessione
    * @return Codice di stato (0 - Errore / 1 - OK)
    */
   private int connectToServer(String machineIP, int port) {
      try {
         server = new Socket(machineIP, port);
         in = initializeReader(server);
         out = initializeWriter(server);
         log.info("Connesso al server!");
         return 1;
      } catch (UnknownHostException unknownHostException) {
         log.warn("Il server non esiste");                   //log errore server inesistente
         return 0;                                           //errore
      } catch (IOException ioException) {
         log.warn("Errore I/O durante la connessione");      //log errore connessione
         return 0;                                           //errore
      }
   }

   private void sendMessageToSocket(Socket socket, String message) {
      PrintWriter pW = null;                                                     //writer per scrivere
      boolean initialized = false;

      while (!initialized) {
         try {
            pW = new PrintWriter(socket.getOutputStream(), true);
            initialized = true;
         } catch (IOException e) {
            log.warn("Errore durante la creazione dell'output stream");
         }
      }

      if (AESKey.isDestroyed()){
         log.warn("Non è stato possibile inviare il messaggio perché la chiave AES è stata distrutta");
         return;
      }

      try {
         String encryptedMessage = AES.encrypt(message, AESKey);  //cripto messaggio
      } catch (Exception e){
         log.warn("Errore durante la criptazione del messaggio, non è stato possibile inviare il messaggio");
         return;
      }

      pW.println(message);    //invio conferma ricezione
      pW.flush();             //forzo invio messaggio
      pW.close();             //chiudo stream
   }

   private BufferedReader initializeReader(Socket socket) throws IOException {
      return new BufferedReader(new InputStreamReader(socket.getInputStream()));
   }

   private PrintWriter initializeWriter(Socket socket) throws IOException {
      return new PrintWriter(socket.getOutputStream(), true);
   }

   /**
    * Listener di messaggi dal server, quando riceve 'END_CONNECTION' termina di ascoltare
    *
    * @return Codice di stato (0 - Errore / 1 - OK / 2 - TERM_LISTEN)
    */
   private String listenForServerMessage() {
      BufferedReader bR;
      try {
         bR = new BufferedReader(new InputStreamReader(server.getInputStream()));    //input stream
      } catch (IOException ioException) {
         log.warn("Errore durante la creazione dell'input stream");
         return "0";                                       //fermo tentativo ascolto
      }

      boolean stop = false;
      while (!stop) {                                      //fermo il ciclo solo in caso di richiesta dall'altro client
         String message;

         try {
            message = bR.readLine();         //ricevo e salvo messaggio da altro client
         } catch (IOException ioException) {
            log.warn("Errore durante la lettura dell'input stream");
            return "0";                                   //fermo tentativo ascolto
         }

         log.debug("Messaggio ricevuto: {}", message);    //log ricezione

         return message;
      }

      try {
         bR.close();
      } catch (IOException e) {
         log.warn("Errore durante la chiusura dell'input stream");
      }
      return "2";
   }

   /**
    * Metodo che richiama tutti i sotto-metodi che eseguono l'algoritmo di Diffie-Hellman
    */
   private void runDiffieHellmanAlgorithm() throws IOException {
      //attendo parametri in input dal server
      String line;

      while ((line = in.readLine()) != null) {
         if (line.startsWith("DH-P--")) {                         //se inizia con DH-P-
            P = new BigInteger(line.substring(6));   //salvo P
            log.debug("Valore P: {}", P);
         } else if (line.startsWith("DH-G--")) {                  //se inizia con DH-G-
            G = new BigInteger(line.substring(6));   //salvo G
            log.debug("Valore G: {}", G);
         } else if (line.startsWith("DH-SERVER_PUBLIC--")) {
            //decripta la chiave pubblica del server
            BigInteger serverPublicKey = new BigInteger(line.substring(18));
            //BigInteger serverPublicKey = rsa.decrypt(encryptedServerPublicKey);

            clientPrivateKey = new BigInteger(1024, new SecureRandom()); //genero chiave privata del client

            clientPublicKey = G.modPow(clientPrivateKey, P);                    //calcolo chiave pubblica del client

            //BigInteger encryptedClientPublicKey = rsa.encrypt(clientPublicKey); //cripto  la chiave pubblica con RSA

            out.println("DH-CLIENT_PUBLIC--" + clientPublicKey);        //invio chiave pubblica al server

            sharedKey = serverPublicKey.modPow(clientPrivateKey, P);    //calcola chiave condivisa
            log.info("Chiave condivisa calcolata: {}", sharedKey);
            break;                                                              //esco dall'if
         } else if (line.equals("DH-COMPLETE")) {
            log.info("Scambio Diffie-Hellman completato");
            break;
         }
      }
   }
}