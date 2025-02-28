package itts.volterra.quintab.Server.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
   private static final String URL = "jdbc:mysql://localhost:3306/Chat_Java";
   private static final String USER = "CappeLoStalkerDiDatabasa";
   private static final String PASSWORD = "sanmarino"; // Se hai impostato una password in XAMPP, inseriscila qui

   public static Connection getConnection() throws SQLException {
      return DriverManager.getConnection(URL, USER, PASSWORD);
   }
}
