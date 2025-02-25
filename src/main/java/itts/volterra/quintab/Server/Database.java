package itts.volterra.quintab.Server;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
   public static void initialize() {
      try (Connection conn = DatabaseConnection.getConnection();
           Statement stmt = conn.createStatement()) {

         // Creazione della tabella Utenti
         String createTableSQL =
               "CREATE TABLE IF NOT EXISTS Utenti (" +
                     "username VARCHAR(50) PRIMARY KEY," +
                     "pwHash VARCHAR(255) NOT NULL," +
                     "level INT NOT NULL DEFAULT 0" +
                     ")";

         stmt.execute(createTableSQL);

         System.out.println("Tabella Utenti creata con successo");

      } catch (SQLException e) {
         System.err.println("Errore durante l'inizializzazione del database: " + e.getMessage());
         e.printStackTrace();
      }
   }

   // Metodo per aggiungere utenti di test/default se necessario
   public static void addDefaultUsers() {
      try (Connection conn = DatabaseConnection.getConnection();
           Statement stmt = conn.createStatement()) {

         // Inserimento di utenti di esempio (in un'applicazione reale, gli hash sarebbero generati correttamente)
         String insertUsersSQL =
               "INSERT IGNORE INTO Utenti (username, pwHash, level) VALUES " +
                     "('admin', 'hash_password_admin', 10)," +
                     "('user', 'hash_password_user', 1)";

         stmt.execute(insertUsersSQL);

         System.out.println("Utenti predefiniti aggiunti con successo");

      } catch (SQLException e) {
         System.err.println("Errore durante l'aggiunta degli utenti: " + e.getMessage());
         e.printStackTrace();
      }
   }
}