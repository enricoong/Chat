package itts.volterra.quintab.MessageSerialization;

import java.net.Socket;

public class Message {
   private String username, message;
   private long timestamp;

   public Message(String username, String message, long timestamp) {
      this.username = username;
      this.message = message;
      this.timestamp = timestamp;
   }

   public String getUsername() {
      return username;
   }

   public String getMessage() {
      return message;
   }

   public long getTimestamp() {
      return timestamp;
   }
}
