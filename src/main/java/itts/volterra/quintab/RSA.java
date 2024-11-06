package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;

public class RSA {
   private static final Logger log = LogManager.getLogger(RSA.class);

   public static int getRandomPrimeNumber(){
      int numero;
      int numMinimo = 50;  //numero minimo del numero primo
      do {
         numero = numMinimo + (int) (Math.random() * 3000); //max limit
      } while (!BigInteger.valueOf(numero).isProbablePrime(10)); //controllo se è un numero primo

      return numero;
   }

   public static int getRandomBetween(int min, int max){
      return (min + (int) (Math.random() * max));
   }

   public static void generateKey(){
      int p = getRandomPrimeNumber();
      log.debug("Generated 'p' prime number: {}", p);
      int q = getRandomPrimeNumber();
      log.debug("Generated 'q' prime number: {}", q);
      int n = p * q;
      log.debug("Generated 'n' number (p*q): {}", n);
      int e = getRandomBetween(1, n);
      log.debug("Generated 'e' number: {}", e);
   }
}