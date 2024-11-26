package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Scanner;

/**
 * @author Negretto Enrico, Battiselli Giovanni
 * @version 0.1.1
 */
public class Main {
   private static final Logger log = LogManager.getLogger(Main.class);
   private static final Scanner kbInput = new Scanner(System.in);

   public static void main(String[] args) {
      //TODO: se stringa da criptare è troppo lunga, la separo in varie parti lunghe ognuna BIT_LENGHT

//      String stringa;   //dichiaro stringa di prendere in ingresso
//      do {
//         System.out.print("Inserisci una stringa: ");
//         stringa = kbInput.nextLine().trim();   //prendo in ingresso e trimmo
//      } while (stringa.isBlank());  //se la stringa è vuota o contiene solo spazi vuoti
//
//
//      log.info("Stringa originale: " + stringa);
//
//      BigInteger stringaCriptata = rsa.encrypt(new BigInteger(stringa.getBytes()));
//      log.info("Stringa criptata: " + stringaCriptata);
//
//      String decrypted_string = rsa.decryptToString(stringaCriptata);
//      int nTentativi = 0;
//      while (!decrypted_string.equals(stringa) && nTentativi<10){   //stringa iniziale != stirnga finale
//         //probabilmente numeri primi non sono primi, devo re-runnare il costruttore
//         log.warn("Errore durante la criptazione, nuovo tentativo...");
//         rsa = new RSA();
//         nTentativi++;
//      }
//      if (!(nTentativi < 10)){
//         log.error("Errore, troppi tentativi effettuati");
//      }
//
//      log.info("Stringa decriptata: " + decrypted_string);

      try {
         new Thread(new Server()).start();            //avvio server
      } catch (IOException e) {                       //se lancia eccezione
         log.error("Che palle (RuntimeException)");   //log
         throw new RuntimeException(e);               //lancio eccezione anch'io perché non mi pagano per far sta roba
      }
      new Thread(new Client()).start();               //avvio client
   }
}