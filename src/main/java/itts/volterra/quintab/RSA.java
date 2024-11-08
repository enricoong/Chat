package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSA {
   private static final Logger log = LogManager.getLogger(RSA.class);
   private static BigInteger n, d, e;
   private final int bitLength = 1024;

   /**
    * Costruttore che mi genera i numeri necessari in futuro per criptare e decriptare.
    */
   public RSA(){
      SecureRandom random = new SecureRandom();
      BigInteger p = BigInteger.probablePrime(bitLength / 2, random);
      log.debug("Generated 'p' prime number: {}", p);
      BigInteger q = BigInteger.probablePrime(bitLength / 2, random);
      log.debug("Generated 'q' prime number: {}", q);

      //n = p * q
      n = p.multiply(q);
      log.debug("Generated 'n' number: {}", n);

      //(p-1)*(q-1)
      BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
      log.debug("Generated 'phi': {}", phi);

      //condizioni: 1 < e < phi(n) e gcd(e, phi(n)) = 1
      e = BigInteger.probablePrime(bitLength / 2, random);
      while (phi.gcd(e).compareTo(BigInteger.ONE) > 0 && e.compareTo(phi) < 0) {
         e = e.add(BigInteger.ONE);
      }
      log.debug("Generated 'e': {}", e);

      //inverso moltiplicativo di e mod(phi(n))
      d = e.modInverse(phi);
      log.debug("Calculated the 'd' multiplicative inverse: {}", d);
   }

   /**
    * Cripta una stringa
    *
    * @param message Stringa da criptare
    * @return Stringa criptata
    */
   public static BigInteger encrypt(String message){
      BigInteger messageBigInt = new BigInteger(message.getBytes());
      log.debug("Il 'messageBigInt' vale: {}", messageBigInt);
      log.debug("'e' vale: {}", e);
      log.debug("'n' vale: {}", n);
      return messageBigInt.modPow(e, n);
   }

   /**
    * Cripta un BigInteger
    *
    * @param message BigInteger da criptare
    * @return BigInteger criptato
    */
   public static BigInteger encrypt(BigInteger message){
      return message.modPow(e, n);
   }

   /**
    * Decripta un BigInteger
    *
    * @param encrypted BigInteger da decriptare
    * @return BigInteger decriptato
    */
   public static BigInteger decrypt(BigInteger encrypted) {
      return encrypted.modPow(d, n);
   }

   public static String decryptToString(BigInteger encrypted) {
      return new String(encrypted.modPow(d, n).toByteArray());
   }
}