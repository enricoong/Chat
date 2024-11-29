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
   public static BigInteger encrypt(String message){
      return encrypt(new BigInteger(message.getBytes()));
   }

   static int nPezzi;
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

//      if (message.bitLength() > BIT_LENGHT){
//         //separo in pezzi
////         int nPezzi = message.bitLength() / BIT_LENGHT;  //calcolo numero di pezzi interi
////         if (message.bitLength() % BIT_LENGHT != 0){     //se c'è il resto della divisione
////            nPezzi++;                                    //aggiungo una altro pezzo
////         }
////
////         log.debug("Numero di pezzi in cui dividere il messaggio: {}/" + BIT_LENGHT + "={}", message.bitLength(), nPezzi);
////
////         StringBuilder messaggioCriptato = new StringBuilder();   //StringBuilder dove metterò i pezzi del messaggio criptato
//
//         nPezzi = (message.bitLength() + BIT_LENGHT - 1) / BIT_LENGHT;  // divisione con arrotondamento per eccesso
//
//         log.debug("Numero di pezzi in cui dividere il messaggio: {}/" + BIT_LENGHT + "={}", message.bitLength(), nPezzi);
//
//         StringBuilder messaggioCriptato = new StringBuilder();
//         String messageStr = message.toString();
//         int charPerBlock = BIT_LENGHT / 16;  // numero di caratteri per blocco
//
//         //i = contatore bit | j = contatore
////         for (int i = 0, j = 0; j<=nPezzi; i += (BIT_LENGHT/16), j++) {
////            log.debug("Ciclo: {}", j);
////            String block;
////            if (j == nPezzi){                                                 //se sono all'ultimo pezzo del messaggio
////               // TODO il messaggio qui dovrebbe essere lungo meno di 1024, ma non funziona
////               block = message.toString().substring(i);                       //il blocco va da 'i' alla fine del messaggio
////            } else {                                                          //altrimenti
////               block = message.toString().substring(i, i+(BIT_LENGHT/16));    //il blocco va da 'i' a 'i+BIT_LENGHT'
////            }
////
////            //ora devo criptare il singolo messaggio
////            BigInteger bloccoCriptato = encrypt(new BigInteger(block));
////            messaggioCriptato.append(bloccoCriptato);
////            log.debug("Aggiunto al messaggio criptato un blocco: {}", bloccoCriptato);
//         for (int i = 0, j = 0; j < nPezzi; j++) {
//            log.debug("Ciclo: {}", j);
//            String block;
//            int startIndex = i;
//            int endIndex = Math.min(i + charPerBlock, messageStr.length());
//
//            block = messageStr.substring(startIndex, endIndex);
//
//            // Verifica lunghezza del blocco in bit
//            BigInteger blockInt = new BigInteger(block);
//            if (blockInt.bitLength() > BIT_LENGHT) {
//               log.warn("Blocco troppo lungo: {} bits", blockInt.bitLength());
//               // Se necessario, tronca il blocco
//               endIndex = startIndex + (charPerBlock - 1);
//               block = messageStr.substring(startIndex, endIndex);
//               blockInt = new BigInteger(block);
//            }
//
//            BigInteger bloccoCriptato = encrypt(blockInt);
//            messaggioCriptato.append(bloccoCriptato);
//            log.debug("Aggiunto al messaggio criptato un blocco: {} (lunghezza in bit: {})",
//                    bloccoCriptato, blockInt.bitLength());
//
//            i = endIndex; // Aggiorna l'indice per il prossimo blocco
//         }
//
//         log.info("Encrypting string...");
//         return new BigInteger(messaggioCriptato.toString());
//      } else{
         //ritorno stringa intera criptata
         log.info("Encrypting string...");
         return message.modPow(e, n);  //argomenti -> chiave pubblica
//      }
   }

   /**
    * Decripta un BigInteger
    *
    * @param encrypted BigInteger da decriptare
    * @return BigInteger decriptato
    */
   public static BigInteger decrypt(BigInteger encrypted) {
      log.info("Decrypting string...");
      return encrypted.modPow(d, n);   //argomenti -> chiave privata

//      String messageStr = encrypted.toString();
//      int blockSize = BIT_LENGHT / 16;  // Numero di caratteri per blocco nel messaggio criptato
//      int messageLength = messageStr.length();
//      int nBlocchi = nPezzi;
//
//      log.debug("Decrypting message of length {} in {} blocks", messageLength, nBlocchi);
//
//      StringBuilder decryptedMessage = new StringBuilder();
//
//      for (int i = 0; i < nBlocchi; i++) {
//         int startIndex = i * blockSize;
//         int endIndex = Math.min(startIndex + blockSize, messageLength);
//
//         try {
//            String encryptedBlock = messageStr.substring(startIndex, endIndex);
//            BigInteger encryptedBlockInt = new BigInteger(encryptedBlock);
//            BigInteger decryptedBlock = decryptBlock(encryptedBlockInt);
//
//            decryptedMessage.append(decryptedBlock.toString());
//            log.debug("Decrypted block {}/{}: {} -> {}",
//                    i + 1, nBlocchi, encryptedBlock, decryptedBlock);
//
//         } catch (NumberFormatException e) {
//            log.error("Error decrypting block at position {}: {}", i, e.getMessage());
//         }
//      }
//
//      log.info("Message decrypted successfully");
//      return new BigInteger(decryptedMessage.toString());
   }

//   private static BigInteger decryptBlock(BigInteger encryptedBlockInt) {
//      return encryptedBlockInt.modPow(d, n);
//   }

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