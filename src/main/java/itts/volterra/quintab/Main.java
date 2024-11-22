package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Scanner;

/**
 * @author Negretto Enrico, Battiselli Giovanni
 * @version 0.1.1
 */
public class Main {
   private static final Logger log = LogManager.getLogger(Main.class);
   private static RSA rsa = new RSA();

   public static void main(String[] args) {
      //TODO: se stringa da criptare è troppo lunga, la separo in varie parti lunghe ognuna BIT_LENGHT
      Scanner kbInput = new Scanner(System.in);

      String stringa;   //dichiaro stringa di prendere in input
      do {
         System.out.print("Inserisci una stringa: ");
         stringa = kbInput.nextLine().trim();   //prendo in input e trimmo
      } while (stringa.isBlank());  //se la stringa è vuota o contiene solo spazi vuoti


      log.info("Stringa originale: " + stringa);

      BigInteger stringaCriptata = rsa.encrypt(stringa);
      log.info("Stringa criptata: " + stringaCriptata);

      String decrypted_string = rsa.decryptToString(stringaCriptata);
      if (!decrypted_string.equals(stringa)){   //stringa iniziale != stirnga finale
         //probabilmente numeri primi non sono primi, devo re-runnare il costruttore
         rsa = new RSA();
      }

      log.info("Stringa decriptata: " + decrypted_string);
   }
}