package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.security.SecureRandom;

public class RSA {
   private static final Logger log = LogManager.getLogger(RSA.class);
   private static final int BIT_LENGHT = 1024;   //minimo 1024 in RSA per una chiave sicura
   private BigInteger n, d, e;

   /**
    * Costruttore che mi genera i numeri necessari in futuro per criptare e decriptare.
    */
   public RSA(){
      SecureRandom random = new SecureRandom(); //SecureRandom è più random di Random
      BigInteger p = BigInteger.probablePrime(BIT_LENGHT, random);
      log.debug("Generated 'p' prime number");
      BigInteger q = BigInteger.probablePrime(BIT_LENGHT, random);
      log.debug("Generated 'q' prime number");

      //n = p * q
      n = p.multiply(q);
      log.debug("Generated 'n' number (P*Q)");

      //(p-1)*(q-1)
      BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
      log.debug("Generated 'phi' ((P-1) * (Q-1))");

      //condizioni: 1 < e < phi(n) e gcd(e, phi(n)) = 1
      e = BigInteger.probablePrime(BIT_LENGHT, random);
      while (phi.gcd(e).compareTo(BigInteger.ONE) > 0 && e.compareTo(phi) < 0) {
         e = e.add(BigInteger.ONE);
      }
      log.debug("Generated 'e' number");

      //inverso moltiplicativo di e mod(phi(n))
      d = e.modInverse(phi);
      log.debug("Calculated the 'd' multiplicative inverse");
   }

   /**
    * Cripta una stringa
    *
    * @param message Stringa da criptare
    * @return Stringa criptata
    */
   public BigInteger encrypt(String message){
      return encrypt(new BigInteger(message.getBytes()));
   }

   static int nPezzi;
   /**
    * Cripta un BigInteger
    *
    * @param message BigInteger da criptare
    * @return BigInteger criptato
    */
   public BigInteger encrypt(BigInteger message){
      /*
      Se io ho un messaggio lungo più di BIT_LENGHT bit, allora separo il big integer in pezzi lunghi al massimo
      BIT_LENGHT caratteri, ma non per forza con quella lunghezza, esempio: se la lunghezza in bit massima fosse 5 e io
      devo criptare un messaggio lungo 13 allora lo dividerebbe in 5+5+3, poi li cripta singolarmente e li rimette insieme
       */

      //ritorno stringa intera criptata
      log.info("Encrypting string...");
      return message.modPow(e, n);  //argomenti -> chiave pubblica
   }

   /**
    * Decripta un BigInteger
    *
    * @param encrypted BigInteger da decriptare
    * @return BigInteger decriptato
    */
   public BigInteger decrypt(BigInteger encrypted) {
      log.info("Decrypting string...");
      return encrypted.modPow(d, n);   //argomenti -> chiave privata
   }

   /**
    * Decripta un BigInteger e lo converte in stringa
    *
    * @param encrypted BigInteger da decriptare
    * @return Stringa decriptata
    */
   public String decryptToString(BigInteger encrypted) {
      return new String(decrypt(encrypted).toByteArray());
   }
}