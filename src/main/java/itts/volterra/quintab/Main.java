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

   public static void main(String[] args) throws InterruptedException {
      //TODO: se stringa da criptare è troppo lunga, la separo in varie parti lunghe ognuna BIT_LENGHT

      //todo: add api, user database, separated instances

      try {
         new Thread(new Server()).start();            //avvio server
      } catch (IOException e) {                       //se lancia eccezione
         log.error("Che palle (RuntimeException)");   //log
         throw new RuntimeException(e);               //lancio eccezione anch'io perché non mi pagano per far sta roba
      }

      new Thread(new Client()).start();               //avvio client 1
      //new Thread(new Client()).start();               //avvio client 2
   }
}