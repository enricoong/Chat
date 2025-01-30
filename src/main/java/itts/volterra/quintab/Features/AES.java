package itts.volterra.quintab.Features;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;

public class AES {
   private static final int GCM_IV_LENGTH = 12;
   private static final int GCM_TAG_LENGTH = 128;

   /**
    * Genero una chiave AES data una chiave segreta
    *
    * @param key Chiave segreta
    * @return Chiave AES
    */
   public static SecretKey generateKeyForAES(BigInteger key) {
      return new SecretKeySpec(key.toByteArray(), 0, 32, "AES");
   }

   /**
    * Genero una chiave AES data una chiave segreta
    *
    * @param key Chiave segreta
    * @return Chiave AES
    */
   public static SecretKey generateKeyForAES(byte[] key) {
      return new SecretKeySpec(key, 0, 32, "AES");
   }

   /**
    * Cripta del testo data la chiave
    *
    * @param plaintext Testo
    * @param key Chiave segreta
    * @return Testo criptato
    * @throws Exception
    */
   public static String encrypt(String plaintext, SecretKey key) throws Exception {
      //genero un Initialization Vector casuale
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);

      //inizializzo il cipher
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

      //cifro il testo
      byte[] cipherText = cipher.doFinal(plaintext.getBytes());

      //unisco IV e testo cifrato
      byte[] encrypted = new byte[iv.length + cipherText.length];
      System.arraycopy(iv, 0, encrypted, 0, iv.length);
      System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);

      //codifico in Base64 per trasmissione sicura
      return Base64.getEncoder().encodeToString(encrypted);
   }

   /**
    * Decripta del testo data la chiave
    *
    * @param ciphertext Testo cifrato
    * @param key Chiave segreta
    * @return Testo originale
    * @throws Exception
    */
   public static String decrypt(String ciphertext, SecretKey key) throws Exception {
      //decodifica da Base64
      byte[] decoded = Base64.getDecoder().decode(ciphertext);

      //estraggo il vettore d'inizializzazione
      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

      //estraggo il testo cifrato
      byte[] cipherText = new byte[decoded.length - GCM_IV_LENGTH];
      System.arraycopy(decoded, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

      // inizializzo il cipher per la decifratura
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

      //decifro
      byte[] decrypted = cipher.doFinal(cipherText);
      return new String(decrypted);
   }
}