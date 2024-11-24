package itts.volterra.quintab;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class RSA {
   private static final Logger log = LogManager.getLogger(RSA.class);
   private static final int BIT_LENGHT = 1024;   //minimo 1024 in RSA per una chiave sicura
   private static BigInteger n, d, e;

   /**
    * Costruttore che mi genera i numeri necessari in futuro per criptare e decriptare.
    */
   public RSA(){
      SecureRandom random = new SecureRandom(); //SecureRandom è più random di Random
      BigInteger p = BigInteger.probablePrime(BIT_LENGHT, random);
      log.debug("Generated 'p' prime number: {}", p);
      BigInteger q = BigInteger.probablePrime(BIT_LENGHT, random);
      log.debug("Generated 'q' prime number: {}", q);

      //n = p * q
      n = p.multiply(q);
      log.debug("Generated 'n' number: {}", n);

      //(p-1)*(q-1)
      BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
      log.debug("Generated 'phi': {}", phi);

      //condizioni: 1 < e < phi(n) e gcd(e, phi(n)) = 1
      e = BigInteger.probablePrime(BIT_LENGHT, random);
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
      return encrypt(new BigInteger(message.getBytes()));
   }

   /**
    * Cripta un BigInteger
    *
    * @param message BigInteger da criptare
    * @return BigInteger criptato
    */
   public static BigInteger encrypt(BigInteger message){
      /*
      Se io ho un messaggio lungo più di BIT_LENGHT bit, allora separo il big integer in pezzi lunghi al massimo
      BIT_LENGHT caratteri, ma non per forza con quella lunghezza, esempio: se la lunghezza in bit massima fosse 5 e io
      devo criptare un messaggio lungo 13 allora lo dividerebbe in 5+5+3, poi li cripta singolarmente e li rimette insieme
       */

      if (message.bitLength() > BIT_LENGHT){
         //separo in pezzi
         int nPezzi = message.bitLength() / BIT_LENGHT;  //calcolo numero di pezzi interi
         if (message.bitLength() % BIT_LENGHT != 0){     //se c'è il resto della divisione
            nPezzi++;                                    //aggiungo una altro pezzo
         }

         log.debug("Numero di pezzi in cui dividere il messaggio: {}/" + BIT_LENGHT + "={}", message.bitLength(), nPezzi);

         StringBuilder messaggioCriptato = new StringBuilder();   //StringBuilder dove metterò i pezzi del messaggio criptato

         //i = contatore bit | j = contatore
         for (int i = 0, j = 0; j<=nPezzi; i += (BIT_LENGHT*16), j++) {
            String block;
            if (j == nPezzi){                                                 //se sono all'ultimo pezzo del messaggio
               block = message.toString().substring(i, message.bitLength());  //il blocco va da 'i' alla fine del messaggio
            } else {                                                          //altrimenti
               block = message.toString().substring(i, i+(BIT_LENGHT*16));    //il blocco va da 'i' a 'i+BIT_LENGHT'
            }

            //ora devo criptare il singolo messaggio
            BigInteger bloccoCriptato = encrypt(new BigInteger(block));
            messaggioCriptato.append(bloccoCriptato);
            log.debug("Aggiunto al messaggio criptato un blocco: {}", bloccoCriptato);
         }

         log.info("Encrypting string...");
         return new BigInteger(messaggioCriptato.toString());
      } else{
         //ritorno stringa intera criptata
         log.info("Encrypting string...");
         return message.modPow(e, n);  //argomenti -> chiave pubblica
      }
   }

   /**
    * Decripta un BigInteger
    *
    * @param encrypted BigInteger da decriptare
    * @return BigInteger decriptato
    */
   public static BigInteger decrypt(BigInteger encrypted) {
      log.info("Decrypting string");
      return encrypted.modPow(d, n);   //argomenti -> chiave privata
   }

   /**
    * Decripta un BigInteger e lo converte in stringa
    *
    * @param encrypted BigInteger da decriptare
    * @return Stringa decriptata
    */
   public static String decryptToString(BigInteger encrypted) {
      return new String(decrypt(encrypted).toByteArray());
   }

   /**
    * Separa un BigInteger in pezzi di determinata lunghezza
    *
    * @param num BigInteger da separare
    * @param chunkSize Dimensione del singolo blocco
    * @return List di tutti i pezzi
    */
//   public static List<BigInteger> splitBigInteger(BigInteger num, int chunkSize) {
//      //TODO
//   }
}