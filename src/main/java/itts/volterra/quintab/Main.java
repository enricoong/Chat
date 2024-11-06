package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
   private static final Logger log = LogManager.getLogger(Main.class);

   public static void main(String[] args) {
      log.error("Test");

      RSA.generateKey();
   }
}