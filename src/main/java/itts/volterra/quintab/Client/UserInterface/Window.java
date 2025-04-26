package itts.volterra.quintab.Client.UserInterface;

import com.formdev.flatlaf.FlatDarkLaf;
import itts.volterra.quintab.Client.ClientFunctionalities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Window extends JFrame implements Runnable {
   private static final Logger log = LogManager.getLogger(Window.class);

   JFrame mainFrame, serverInputFrame; //le due finestre
   ClientFunctionalities client;

   public Window(ClientFunctionalities client) {
      try {
         FlatDarkLaf.setup();
      } catch (Exception e) {
         log.error("Errore durante l'inizializzazione del tema FlatLaf: {}", e.getMessage());
      }
      this.client = client;
   }

   public void run() {
      initializeMainWindow();    //inizializzo finestra principale
      initializeServerWindow();  //creo finestra richiesta ip server
   }

   /**
    * Inizializza la finestra principale della chat
    */
   private void initializeMainWindow() {
      SwingUtilities.invokeLater(() -> {
         mainFrame = new JFrame("Chat - Negretto Enrico");
         mainFrame.setSize(800, 600);
         mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   //gestisco chiusura da actionListener
         mainFrame.setLocationRelativeTo(null);  //centra la finestra
         mainFrame.setVisible(false);

         //ATTENZIONE: senza il seguente actionListener, quando il client si chiude,
         // il server non chiude correttamente la connessione
         //actionListener chiusura:
         mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
               //invio messaggio "STOP" a server e chiudo programma
               ClientFunctionalities.sendMessageToServer("STOP");
               System.exit(0);
            }
         });
      });
   }

   /**
    * Inizializza la finestra di input dell'IP del server
    */
   private void initializeServerWindow() {
      SwingUtilities.invokeLater(() -> {
         serverInputFrame = new JFrame("Inserisci server per Chat - Negretto Enrico");
         serverInputFrame.setSize(400, 200);
         serverInputFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         serverInputFrame.setLocationRelativeTo(null);  //centra la finestra
         serverInputFrame.setVisible(true);

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());
         panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.insets = new Insets(5, 5, 5, 5);

         //aggiungi etichetta per IP
         JLabel ipLabel = new JLabel("Indirizzo IP del Server:");
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.gridwidth = 2;
         panel.add(ipLabel, gbc);

         //aggiungi campo di testo per IP
         JTextField ipField = new JTextField("127.0.0.1", 20);
         gbc.gridx = 0;
         gbc.gridy = 1;
         gbc.gridwidth = 2;
         panel.add(ipField, gbc);

         //aggiungi etichetta invisibile per messaggi di errore
         JLabel errorLabel = new JLabel(" ");
         errorLabel.setForeground(Color.RED);
         gbc.gridx = 0;
         gbc.gridy = 2;
         gbc.gridwidth = 2;
         panel.add(errorLabel, gbc);

         //aggiungi pulsante "Connetti"
         JButton connectButton = new JButton("Connetti");
         gbc.gridx = 0;
         gbc.gridy = 3;
         gbc.gridwidth = 2;
         gbc.anchor = GridBagConstraints.CENTER;
         panel.add(connectButton, gbc);

         //aggiungi un ActionListener al pulsante
         connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               String serverIP = ipField.getText().trim();
               if (isValidIP(serverIP)) {
                  errorLabel.setText("Tentativo di connessione a " + serverIP + "...");
                  errorLabel.setForeground(new Color(0, 150, 0)); //verde

                  boolean connectionResult = client.initializeConnection(serverIP);
                  if (connectionResult){
                     errorLabel.setText("Connessione  stabilita con successo!");
                     serverInputFrame.setVisible(false);    //quando connesso, nascondo il popup di connessione
                     initializeMainWindow();
                     mainFrame.setVisible(true);   //e mostro la finestra principale
                  } else {
                     errorLabel.setText("Errore durante l'inizializzazione della connessione");
                  }
               } else {
                  errorLabel.setText("Indirizzo IP non valido. Usa il formato corretto (es. 192.168.1.1)");
                  errorLabel.setForeground(Color.RED);   //rosso
               }
            }
         });

         serverInputFrame.add(panel);
      });
   }

   /**
    * Controlla se una stringa è un'IP
    *
    * @param ip Stringa
    * @return True se è un'IP
    */
   private static boolean isValidIP(String ip) {
      if (ip == null || ip.isEmpty()) {
         return false;
      }

      // Controllo base per la validità dell'IP (puoi migliorarlo con una regex più precisa)
      String[] parts = ip.split("\\.");
      if (parts.length != 4) {
         return false;
      }

      try {
         for (String part : parts) {
            int value = Integer.parseInt(part);
            if (value < 0 || value > 255) {
               return false;
            }
         }
         return true;
      } catch (NumberFormatException e) {
         return false;
      }
   }

   /**
    * Cambia visibilità della finestra della selezione del server
    *
    * @param visible Visibilità
    */
   private void setServerWindowVisible(boolean visible) {
      serverInputFrame.setVisible(visible);
   }

   /**
    * Cambia visibilità della finestra principale
    *
    * @param visible Visibilità
    */
   private void setMainWindowVisible(boolean visible) {
      mainFrame.setVisible(visible);
   }
}