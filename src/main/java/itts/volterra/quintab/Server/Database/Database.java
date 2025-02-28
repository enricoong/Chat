package itts.volterra.quintab.Server.Database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import itts.volterra.quintab.Features.SHA256;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
   private static final Logger log = LogManager.getLogger(Database.class);

   public static void initialize() {
      try (Connection conn = DatabaseConnection.getConnection();
           Statement stmt = conn.createStatement()) {

         //creo tabella Utenti
         String createTableSQL =
               "CREATE TABLE IF NOT EXISTS Utenti (" +
                     "username VARCHAR(50) PRIMARY KEY," +
                     "pwHash VARCHAR(255) NOT NULL," +
                     "level INT NOT NULL DEFAULT 0" +
                     ")";

         stmt.execute(createTableSQL);

         log.debug("Tabella Utenti creata con successo");

      } catch (SQLException e) {
         log.error("Errore durante l'inizializzazione del database: {}", e.getMessage());
         e.printStackTrace();
      }
   }

   //Aggiungere utenti di test/default se necessario
   public static void addDefaultUsers() {
      try (Connection conn = DatabaseConnection.getConnection();
           Statement stmt = conn.createStatement()) {

         //Inserimento di utenti di esempio (in un'applicazione reale, gli hash sarebbero generati correttamente)
         String insertUsersSQL = null;
         boolean error = false;

         try{
            insertUsersSQL =
                  "INSERT IGNORE INTO Utenti (username, pwHash, level) VALUES " +
                        "('admin', " + SHA256.encrypt("amdin") +", 10)," +
                        "('user', " + SHA256.encrypt("user") + ", 1)";
         } catch (NoSuchAlgorithmException e){
            error = true;
            log.error("Errore durante la criptazione dei dati da inserire del database: {}", e.getMessage());
         }

         if(!error){
            stmt.execute(insertUsersSQL);
            log.info("Utenti predefiniti aggiunti con successo");
         } else {
            log.warn("Errore durante l'aggiunta degli utenti predefiniti");
         }

      } catch (SQLException e) {
         log.error("Errore durante l'aggiunta degli utenti: {}", e.getMessage());
         e.printStackTrace();
      }
   }
}