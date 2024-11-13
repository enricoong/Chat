package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSA {
   private static final Logger log = LogManager.getLogger(RSA.class);
   private static BigInteger n, d, e;
   private final int BIT_LENGTH = 1024;

   /**
    * Costruttore che mi genera i numeri necessari in futuro per criptare e decriptare.
    */
   public RSA(){
      SecureRandom random = new SecureRandom();
      BigInteger p = BigInteger.probablePrime(BIT_LENGTH / 2, random);
      log.debug("Generated 'p' prime number: {}", p);
      BigInteger q = BigInteger.probablePrime(BIT_LENGTH / 2, random);
      log.debug("Generated 'q' prime number: {}", q);

      //n = p * q
      n = p.multiply(q);
      log.debug("Generated 'n' number: {}", n);

      //(p-1)*(q-1)
      BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
      log.debug("Generated 'phi': {}", phi);

      //condizioni: 1 < e < phi(n) e gcd(e, phi(n)) = 1
      e = BigInteger.probablePrime(BIT_LENGTH / 2, random);
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
      log.info("Encrypted string");
      return messageBigInt.modPow(e, n);  //argomenti -> chiave pubblica
   }

   /**
    * Cripta un BigInteger
    *
    * @param message BigInteger da criptare
    * @return BigInteger criptato
    */
   public static BigInteger encrypt(BigInteger message){
      log.info("Encrypted string");
      return message.modPow(e, n);  //argomenti -> chiave pubblica
   }

   /**
    * Decripta un BigInteger
    *
    * @param encrypted BigInteger da decriptare
    * @return BigInteger decriptato
    */
   public static BigInteger decrypt(BigInteger encrypted) {
      log.info("Decrypted string");
      return encrypted.modPow(d, n);   //argomenti -> chiave privata
   }

   /**
    * Decripta un BigInteger e lo converte in stringa
    *
    * @param encrypted BigInteger da decriptare
    * @return Stringa decriptata
    */
   public static String decryptToString(BigInteger encrypted) {
      log.info("Decrypted string");
      return new String(encrypted.modPow(d, n).toByteArray()); //argomenti di modPow -> chiave privata
   }
}