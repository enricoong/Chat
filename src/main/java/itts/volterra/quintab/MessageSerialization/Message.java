package itts.volterra.quintab.MessageSerialization;

public class Message {
   private String username, message;
   private long timestamp = 0;

   //costruttore predefinito richiesto da Jackson
   public Message() {}

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

   public boolean isEmpty() {
      return username.isEmpty() && message.isEmpty() && timestamp == 0;
   }
}
