package itts.volterra.quintab.MessageSerialization;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimestampConverter {
   /**
    * Converte il timestamp da long a stringa formattata
    *
    * @param timestamp Timestamp in millisecondi
    * @return Timestamp formattato come stringa
    */
   public static String longToStringWithoutDate(long timestamp) {
      //converto timestamp da long a stringa
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
      return formatter.format(Instant.ofEpochMilli(timestamp));
   }

   /**
    * Converte il timestamp da long a stringa formattata
    *
    * @param timestamp Timestamp in millisecondi
    * @return Timestamp formattato come stringa
    */
   public static String longToStringWithDate(long timestamp) {
      //converto timestamp da long a stringa
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());
      return formatter.format(Instant.ofEpochMilli(timestamp));
   }
}
