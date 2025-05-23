package itts.volterra.quintab.Client;

import itts.volterra.quintab.Encryption.AES;
import itts.volterra.quintab.Encryption.SHA256;
import itts.volterra.quintab.MessageSerialization.JsonHandler;
import itts.volterra.quintab.MessageSerialization.Message;
import itts.volterra.quintab.MessageSerialization.TimestampConverter;
import itts.volterra.quintab.Server.Database.Database;
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
import java.util.Arrays;
import java.util.Scanner;

public class Client implements Runnable {

   private static final Logger log = LogManager.getLogger(Client.class);
   private BufferedReader in;
   private PrintWriter out;
   private final Scanner kbInput = new Scanner(System.in);
   private Socket server;
   Thread receiverThread;

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

      try {
         runDiffieHellmanAlgorithm();

         log.debug("Shared key bytes length: {}", sharedKey.toByteArray().length);
         AESKey = AES.generateKeyForAES(sharedKey);
         log.debug("AES key algorithm: {}", AESKey.getAlgorithm());
         log.debug("AES key format: {}", AESKey.getFormat());
         log.debug("AES key encoded length: {}", AESKey.getEncoded().length);
         log.debug("Chiave AES: {}", Arrays.hashCode(AESKey.getEncoded()));
         log.info("Chiave AES creata con successo");
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
      log.warn("--- Scrivi 'STOP' per terminare il programma ---");
      log.warn("Scrivi un messaggio quando vuoi, premi 'Invio' per inviarlo");
      log.warn("------ Comandi ------");
      log.warn("Messaggio privato:  /msg nomeUtente messaggio");
      log.warn("Broadcast:          /broadcast messaggio");
      log.warn("------ Have fun :D ------");

      String userInput;
      //ora vorrei che il client aspettasse un messaggio del server, ma se scrivo comunque invii quello che scrivo
      receiverThread = new Thread(this::messageListener);
      receiverThread.start();

      do {
         //System.out.print("Scrivi >");
         userInput = kbInput.nextLine().trim();
         if(userInput.equals("STOP")){
            //stop
            stop = true;
            sendMessageToServer(userInput);
         } else {
            sendMessageToServer(userInput);
         }
      } while (!stop);

      stopAndDisconnect();
   }

   /**
    * Il client si identifica e viene autenticato dal server
    */
   private void authenticate() throws IOException {
      boolean authenticated = false, usernameInDatabase = false;
      String username = null;
      while (!authenticated) {
         if (!usernameInDatabase) {
            do {
               System.out.print("Inserisci il tuo username: ");
               username = kbInput.nextLine().trim();  //acquisisco input
            } while (username.trim().isEmpty() && !username.equalsIgnoreCase("server") && !username.equalsIgnoreCase("stillnotloggedin"));   //controllo che username non sia vuoto o sia "SERVER"
            log.debug("Username inserito: '{}'", username);
            //qui sarebbe carino un controllo dello username che non sia vuoto
            sendMessageToServer("USRNM-" + username);   //invio username al server
         }

         Message message = waitAndDecryptServerMessage();

         log.debug("Messaggio ricevuto (deserializzato): {}", message.getMessage());
         switch (message.getMessage()) {
            case "USERNAME-OK":{
               //lo username esiste nel database
               usernameInDatabase = true;
               log.info("Lo username inserito è presente nel database");

               System.out.print("Inserisci la password: ");
               String pwHash = null;
               try {
                  //pwHash = SHA256.encrypt(String.valueOf(System.console().readPassword()).trim());  //acquisisco input
                  pwHash = SHA256.encrypt(kbInput.nextLine().trim());   //acquisisco input
               } catch (NoSuchAlgorithmException e) {
                  log.error("errore durante la criptazione della password", e);
               }

               sendMessageToServer("PSSWD-" + pwHash);

               break;
            }

            case "USERNAME-NOTFOUND":{
               log.info("Lo username inserito non è presente nel database");
               System.out.print("Vuoi creare un nuovo utente? [Y/N] >");
               char choice = kbInput.nextLine().trim().charAt(0);  //acquisisco input
               if (choice == 'Y' || choice == 'y') {
                  createNewUser(username);
               } else if (choice == 'N' || choice == 'n') {
                  log.info("Ok, ricomincio autenticazione");
               } else {
                  log.warn("Valore non riconosciuto, risposta ignorata");
               }

               //essendo il flag 'authenticated' ancora falso, ricomincia

               break;
            }

            case "PASSWORD-OK":{
               if (usernameInDatabase) {
                  //la password era corretta
                  log.info("Password corretta");
                  authenticated = true;   //flag autenticato
               } else {
                  log.error("Non dovresti ricevere questo messaggio senza aver inserito prima uno username");
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

            case null: {
               log.warn("Ricevuto un messaggio vuoto, probabilmente c'è qualcosa che non va");
               break;
            }

            default: {
               log.warn("Ricevuto da '{}' il seguente messaggio: '{}'", message.getUsername(), message.getMessage());
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
    * Chiude input e output stream e interrompe il Thread
    */
   protected void stopAndDisconnect(){
      try {
         in.close();
      } catch (IOException e) {
         log.error("Errore durante la chiusura del Reader", e);
      }
      out.close();
      kbInput.close();                    //chiudo scanner
      receiverThread.interrupt();         //fermo thread listener di messaggi
      Thread.currentThread().interrupt(); //fermo thread
   }

   /**
    * Invia un messaggio al server
    *
    * @param message Messaggio
    */
   protected void sendMessageToServer(String message) {
      if (AESKey.isDestroyed()){
         log.warn("Non è stato possibile inviare il messaggio perché la chiave AES è stata distrutta");
         return;
      } else if (message == null || message.trim().isEmpty()) {
         log.warn("Il messaggio che si stava tentando di inviare è null o vuoto, operazione annullata");
         return;
      }

      String serializedMessage = null;
      String tempLoggedUser;
      if (loggedUser == null){
         tempLoggedUser  = "STILLNOTLOGGEDIN";
      } else {
         tempLoggedUser = loggedUser;
      }

      try {
         serializedMessage = JsonHandler.serialize(new Message(tempLoggedUser, message, System.currentTimeMillis()));
      } catch (IOException e) {
         log.error("Errore durante la serializzazione del messaggio: {}", e.getMessage());
      }

      String encryptedMessage;
      try {
         encryptedMessage = AES.encrypt(serializedMessage, AESKey);  //cripto messaggio
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
    * Metodo che esegue l'algoritmo di Diffie-Hellman
    */
   private void runDiffieHellmanAlgorithm() throws IOException  {
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

   private Message waitAndDecryptServerMessage() {
      String encryptedMessage = null;

      try {
         while ((encryptedMessage = in.readLine()) == null){
            /*wait for a message todo: better thread management*/
            try {
               Thread.sleep(100);  //per evitare il busy-waiting
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               log.error("Thread interrotto durante l'attesa del messaggio", e);
            }
         }
      } catch (IOException e) {
         log.error("Errore durante l'attesa del messaggio: {}", e.getMessage());
      }

      log.debug("Messaggio ricevuto (grezzo): {}", encryptedMessage);

      String decryptedMessage = null;
      try {
         decryptedMessage = AES.decrypt(encryptedMessage, AESKey);
      } catch (Exception e) {
         log.error("Errore durante la decriptazione del messaggio: {}", e.getMessage());
         return null;
      }

      try {
         return JsonHandler.deserialize(decryptedMessage);
      } catch (IOException e) {
         log.error("Errore durante la deserializzazione del messaggio: {}", e.getMessage());
      }

      return null;
   }

   /**
    * Ascolta messaggi e li logga
    */
   private void messageListener() {
      while (true){
         Message deserializedMessage = waitAndDecryptServerMessage();

         if (deserializedMessage != null){
            log.info("[{}] Messaggio ricevuto da '{}': '{}'", TimestampConverter.longToStringWithDate(deserializedMessage.getTimestamp()), deserializedMessage.getUsername(), deserializedMessage.getMessage());
         } else {
            log.warn("Messaggio ricevuto vuoto, o errore durante decriptazione/deserializzazione");
         }
      }
   }

   /**
    * Crea un nuovo utente, dato il nuovo username
    *
    * @param newUsername Nuovo username
    */
   private void createNewUser(String newUsername) {
      if (newUsername == null || newUsername.trim().isEmpty()) {
         log.warn("Username non valido, annullo creazione utente");
         return;  //username non valido, non creo utente
      }

      log.info("Stai creando un nuovo utente '{}'", newUsername);
      System.out.print("Inserisci la password del nuovo utente >");
      String newPwHash = null;
      try {
         newPwHash = SHA256.encrypt(kbInput.nextLine().trim());  //acquisisco input
      } catch (Exception e) {
         log.error("Errore durante la criptazione della password");
      }

      System.out.print("Reinserisci la password >");
      String newPwHashCheck = null;
      try {
         newPwHashCheck = SHA256.encrypt(kbInput.nextLine().trim());  //acquisisco input
      } catch (Exception e) {
         log.error("Errore durante la criptazione della password");
      }
      if (newPwHash != null && newPwHash.equals(newPwHashCheck)) {
         //ok le due pw sono uguali
         Database.addUser(newUsername, newPwHash);
      } else {
         //folpo te ga sbaja a reinserire 'e password
         log.warn("Le password non corrispondono, operazione annullata");
      }
   }
}