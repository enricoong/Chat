package itts.volterra.quintab.Client;

import itts.volterra.quintab.Features.AES;
import itts.volterra.quintab.Features.SHA256;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Scanner;

public class Client implements Runnable {

   private static final Logger log = LogManager.getLogger(Client.class);
   private BufferedReader in;
   private PrintWriter out;
   private final Scanner kbInput = new Scanner(System.in);
   private Socket server;

   //parametri Diffie-Hellman
   private BigInteger P, G;
   private BigInteger clientPrivateKey;
   private BigInteger clientPublicKey;
   private BigInteger sharedKey;
   private SecretKey AESKey;

   //autenticazione
   String loggedUser = null;

   @Override
   public void run() {
      int connecitonResult = 0;
      boolean stop = false;

      do {
         System.out.print("Inserisci IP della macchina a cui connettersi ['X' per annullare] > ");
         String userInsertedIP = kbInput.nextLine().trim();  //acquisisco input

         if (userInsertedIP.equalsIgnoreCase("X")) {                                            //se utente ha scritto X
            log.debug("L'utente ha scelto di annullare");
            stop = true;                                                            //dichiaro stop = true
         } else {                                                                    //altrimenti proseguo normalmente
            log.info("Tentativo di connessione a {}...", userInsertedIP);
            connecitonResult = connectToServer(userInsertedIP, 12345);              //mi connetto al client
         }
      } while (connecitonResult == 0 && !stop);    //se ritorna codice 0 allora errore e ritento connessione, oppure esco se utente ha deciso di uscire

      while (!stop) {
         //scambio Diffie-Hellman
         try {
            runDiffieHellmanAlgorithm();

            log.debug("Shared key bytes length: {}", sharedKey.toByteArray().length);
            AESKey = AES.generateKeyForAES(sharedKey);
            log.debug("AES key algorithm: {}", AESKey.getAlgorithm());
            log.debug("AES key format: {}", AESKey.getFormat());
            log.debug("AES key encoded length: {}", AESKey.getEncoded().length);
            log.debug("Chiave AES: {}", AESKey.hashCode());
         } catch (IOException e) {
            log.error("Errore durante lo scambio Diffie-Hellman", e);
         }

         //autenticazione
         try {
            authenticate();
         } catch (IOException e) {
            log.error("Errore durante l'autenticazione", e);
         }

         //comunicazione
         sendMessageToServer("Ciaoooo");

         //STOP e chiudo connessione
         sendMessageToServer("STOP");
         stop = true;
      }

      try {
         in.close();
      } catch (IOException e) {
         log.error("Errore durante la chiusura del Reader", e);
      }
      out.close();
      kbInput.close();                    //chiudo scanner
      Thread.currentThread().interrupt(); //fermo thread
   }

   /**
    * Il client si identifica e viene autenticato dal server
    */
   private void authenticate() throws IOException {
      boolean authenticated = false, usernameInDatabase = false;
      String username = null;
      while (!authenticated) {
         //TODO: dopo che non trova lo username o la passowrd, non torna qui
         if (!usernameInDatabase) {
            System.out.print("Inserisci il tuo username: ");
            username = kbInput.nextLine().trim();  //acquisisco input
            log.info("Username inserito: '{}'", username);
            sendMessageToServer("USRNM-" + username);   //invio username al server
         }

         String line;
         while ((line = in.readLine()) == null){/*wait for a message*/}
         log.debug("Messaggio ricevuto: {}", line);

         //todo: riorganizzo tutto con switch-case
         switch (line) {
            case "USERNAME-OK":{
               //lo username esiste nel database
               usernameInDatabase = true;
               log.info("Lo username inserito è presente nel database");

               System.out.print("Inserisci la password: ");
               String pwHash = null;
               try {
                  pwHash = SHA256.encrypt(kbInput.nextLine().trim());  //acquisisco input
               } catch (NoSuchAlgorithmException e) {
                  log.error("errore durante la criptazione della password", e);
               }

               sendMessageToServer("PSSWD-" + pwHash);

               break;
            }

            case "USERNAME-NOTFOUND":{
               log.info("Lo username inserito non è presente nel database, riprova");
               //essendo il flag 'authenticated' ancora falso, ricomincia

               break;
            }

            case "PASSWORD-OK":{
               if (usernameInDatabase) {
                  //la password era corretta
                  log.info("Password corretta");
                  authenticated = true;   //flag autenticato
               }

               usernameInDatabase = false;
               break;
            }

            case "PASSWORD-WRONG":{
               if (usernameInDatabase){
                  //la password era errata
                  log.info("Password errata, ricomincio autenticazione");
                  //essendo il flag 'authenticated' ancora falso, ricomincia il ciclo iniziale
               } else {
                  log.error("Non dovresti ricevere questo messaggio senza aver inserito prima uno username");
               }

               usernameInDatabase = false;
               break;
            }

            default: {
               log.warn("Ricevuto il seguente messaggio:{}", line);
               log.warn("Il messaggio ricevuto non è conosciuto, ricomincio autenticazione");

               break;
            }
         }
      }

      //autenticazione completata
      loggedUser = username;
   }

   /**
    * Si connette a una macchina
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

   /**
    * Invia un messaggio al server
    *
    * @param message Messaggio
    */
   private void sendMessageToServer(String message) {
      if (AESKey.isDestroyed()){
         log.warn("Non è stato possibile inviare il messaggio perché la chiave AES è stata distrutta");
         return;
      }

      String encryptedMessage;
      try {
         encryptedMessage = AES.encrypt(message, AESKey);  //cripto messaggio
      } catch (Exception e){
         log.warn("Errore durante la criptazione del messaggio, non è stato possibile inviare il messaggio");
         return;
      }

      out.println(encryptedMessage);    //invio messaggio
      out.flush();                      //forzo invio messaggio
      log.debug("Messaggio inviato, contenuto del messaggio: '{}'", message);
      //out.close();                    //chiudo stream
   }

   /**
    * Inizializza il BufferedReader per poter ricevere messaggi dal server
    *
    * @param socket Server
    * @return BufferedReader del server
    */
   private BufferedReader initializeReader(Socket socket) throws IOException {
      return new BufferedReader(new InputStreamReader(socket.getInputStream()));
   }

   /**
    * Inizializza il PrintWriter per poter ricevere messaggi dal server
    *
    * @param socket Server
    * @return PrintWriter del server
    */
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
            log.debug("Chiave condivisa calcolata: {}", sharedKey);
         } else if (line.equals("DH-COMPLETE")) {
            log.info("Scambio Diffie-Hellman completato");
            break;
         }
      }
   }
}