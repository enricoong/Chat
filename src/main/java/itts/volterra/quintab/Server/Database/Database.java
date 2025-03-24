package itts.volterra.quintab.Server.Database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import itts.volterra.quintab.Features.SHA256;

import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class Database {
   private static final Logger log = LogManager.getLogger(Database.class);
   private static final String DB_NAME = "Chat_Java";
   private static boolean dbOk;

   /**
    * Inizializza il database: crea il db se non esiste, poi stabilisce una connessione
    * e crea una tabella e aggiunge degli utenti di default.
    *
    * @return True se l'inizializzazione è andata a buon fine, altrimenti False
    */
   public static boolean initialize() {
      //controllo se esiste e creiamo il database se necessario
      dbOk = createDatabaseIfNotExists();

      if (dbOk) {
         try {
            addUserTable();
         } catch (SQLException e) {
            log.error("Errore durante l'inizializzazione del database", e);
         }

         addDefaultUsers();

         return true;
      } else {
         return false;
      }
   }

   /**
    * Verifica l'esistenza del database e lo crea se non esiste
    *
    * @return True se il database esiste/è stato creato | False se errore
    */
   private static boolean createDatabaseIfNotExists() {
      // Connessione al server MySQL senza specificare un database
      String url = "jdbc:mysql://localhost:3306/";
      String user = "root"; //utente predefinito XAMPP
      String password = ""; //password predefinita XAMPP

      try (Connection conn = DriverManager.getConnection(url, user, password)) {
         Statement stmt = conn.createStatement();

         //verifico se il database esiste
         ResultSet resultSet = stmt.executeQuery(
               "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + DB_NAME + "'");

         if (!resultSet.next()) {
            //il database non esiste, lo creo
            stmt.executeUpdate("CREATE DATABASE " + DB_NAME);
            log.info("Database {} creato con successo", DB_NAME);
         } else {
            log.info("Database {} già esistente", DB_NAME);
         }

         return true;
      } catch (SQLException e) {
         log.error("Errore durante la verifica/creazione del database: {}", e.getMessage());
         return false;
      }
   }

   /**
    * Aggiunge utenti default ('admin' e 'user')
    */
   private static void addDefaultUsers() {
      try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt = conn.createStatement()) {

         //inserimento di utenti di esempio
         String insertUsersSQL = null;
         boolean error = false;

         try {
            insertUsersSQL =
                  "INSERT IGNORE INTO `users` (`username`, `pwHash`, `level`) VALUES " +
                        "('admin', '" + SHA256.encrypt("admin") + "', 4)," +
                        "('user', '" + SHA256.encrypt("user") + "', 1)";
         } catch (NoSuchAlgorithmException e) {
            error = true;
            log.error("Errore durante la criptazione dei dati da inserire del database: {}", e.getMessage());
         }

         if (!error) {
            stmt.execute(insertUsersSQL);
            log.debug("Utenti predefiniti -> OK");
         } else {
            log.warn("Errore durante l'aggiunta degli utenti predefiniti");
         }

      } catch (SQLException e) {
         log.error("Errore durante l'aggiunta degli utenti: {}", e.getMessage());
      }
   }

   private static void addUserTable() throws SQLException {
      try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt = conn.createStatement()) {

         //crea la tabella users (se non esiste)
         String createTableSQL =
               "CREATE TABLE IF NOT EXISTS `users` (" +
                     "`ID` int(11) NOT NULL AUTO_INCREMENT, " +
                     "`username` varchar(24) NOT NULL, " +
                     "`pwHash` varchar(64) NOT NULL, " +
                     "`level` int(1) NOT NULL DEFAULT 1, " +
                     "PRIMARY KEY (`ID`), " +
                     "UNIQUE KEY `unique-username` (`username`)" +
                     ")";

         stmt.execute(createTableSQL);

         //aggiungo il constraint per il level
         String constraintSQL =
               "ALTER TABLE `users` " +
                     "ADD CONSTRAINT IF NOT EXISTS `level_constraint` CHECK (level >= 1 AND level <= 4)";

         try {
            stmt.execute(constraintSQL);
         } catch (SQLException e) {
            //alcuni database potrebbero non supportare ADD CONSTRAINT IF NOT EXISTS quindi...
            log.warn("Potrebbe essere già presente il constraint sul level: {}", e.getMessage());
         }

         log.debug("Tabella users -> OK");
      }
   }

   /**
    * Controlla se uno username è presente nel database
    *
    * @param username username da controllare
    * @return True - Esiste lo username | False - Non esiste lo username
    */
   public static boolean usernameExists(String username) {
      try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM users WHERE username = ?")) {

         //passo il parametro username
         pstmt.setString(1, username);

         //eseguo la query col parametro impostato
         ResultSet rs = pstmt.executeQuery();

         //scorro i risultati della query
         if (rs.next()) {
            //leggo il valore del conteggio
            int count = rs.getInt("count");

            //restituisco true se count > 0
            return count > 0;
         }
      } catch (SQLException e) {
         log.error("Errore durante il controllo sull'esistenza dell'utente: {}", e.getMessage());
      }

      //in caso di errore
      return false;
   }

   /**
    * Recupera l'hash della password per un determinato username
    *
    * @param username Username dell'utente
    * @return La stringa contenente il pwHash o null se l'utente non esiste
    */
   public static String getPasswordHash(String username) {
      try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement("SELECT pwHash FROM users WHERE username = ?")) {

         //passo il parametro username
         pstmt.setString(1, username);

         //eseguo la query
         ResultSet rs = pstmt.executeQuery();

         //verifico se è stato trovato un risultato
         if (rs.next()) {
            //restituisco pwHash
            return rs.getString("pwHash");
         }
      } catch (SQLException e) {
         log.error("Errore durante il recupero dell'hash della password: {}", e.getMessage());
      }

      //nessun risultato trovato o errore
      return null;
   }
}