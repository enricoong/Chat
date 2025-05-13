package itts.volterra.quintab.Server.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
   static final String URL = "jdbc:mysql://localhost:3306/";   //LASCIA LO SLASH ALLA FINE!
   static final String DB_NAME = "Chat_Java";
   static final String USER = "CappeLoStalkerDiDatabase";
   static final String PASSWORD = "sanmarino";

   public static Connection getConnection() throws SQLException {
      return DriverManager.getConnection(URL + DB_NAME, USER, PASSWORD);
   }
}
