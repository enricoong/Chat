package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;

public class Main {
   private static final Logger log = LogManager.getLogger(Main.class);
   private static RSA rsa = new RSA();
   
   public static void main(String[] args) {
      String stringa = "test";
      log.info("Stringa originale: " + stringa);
      BigInteger stringaCriptata = rsa.encrypt(stringa);
      log.info("Stringa criptata: " + stringaCriptata);
      log.info("Stringa decriptata: " + rsa.decryptToString(stringaCriptata));
   }
}