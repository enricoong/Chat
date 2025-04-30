package itts.volterra.quintab.MessageSerialization;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonHandler {
   private static final ObjectMapper objectMapper = new ObjectMapper();

   //serializza oggetto in stringa JSON
   public static String serialize(Message messaggio) throws IOException {
      return objectMapper.writeValueAsString(messaggio);
   }

   //deserializza stringa JSON in oggetto
   public static Message deserialize(String json) throws IOException {
      return objectMapper.readValue(json, Message.class);
   }
}
