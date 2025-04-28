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
import java.text.SimpleDateFormat;
import java.util.Date;

public class Window extends JFrame implements Runnable {
   private static final Logger log = LogManager.getLogger(Window.class);

   JFrame mainFrame, serverInputFrame, loginFrame; //le tre finestre
   ClientFunctionalities client;

   //elementi mainFrame:
   private JTextArea chatArea;
   private JTextField messageField;
   private JComboBox<String> messageTypeComboBox;

   //elementi loginFrame:
   private JTextField usernameField;
   private JPasswordField passwordField;
   private JLabel statusLabel;
   private boolean isUsernamePhase = true;   //true = inserimento username, false = inserimento password
   private String currentUsername;           //memorizza l'username corrente

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

      //TODO: aggiungere logging
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

         //crea il pannello principale con BorderLayout
         JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
         mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

         //crea l'area della chat con scroll
         chatArea = new JTextArea();
         chatArea.setEditable(false);
         chatArea.setLineWrap(true);
         chatArea.setWrapStyleWord(true);
         chatArea.setFont(new Font("Sans-Serif", Font.PLAIN, 14));

         JScrollPane scrollPane = new JScrollPane(chatArea);
         scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

         //crea il pannello inferiore per input e invio
         JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));

         //menu dropdown a sinistra
         String[] messageTypes = {"Messaggio Pubblico", "Messaggio Privato", "Server (RAW)"};
         messageTypeComboBox = new JComboBox<>(messageTypes);
         messageTypeComboBox.setPreferredSize(new Dimension(150, 30));

         //input al centro
         messageField = new JTextField();
         messageField.setFont(new Font("Sans-Serif", Font.PLAIN, 14));

         //pulsante invio a destra
         JButton sendButton = new JButton("Invia");
         sendButton.setPreferredSize(new Dimension(100, 30));

         //azione per l'invio del messaggio
         ActionListener sendAction = e -> sendMessage();

         //associa l'azione al pulsante e al tasto Invio nel campo di testo
         sendButton.addActionListener(sendAction);
         messageField.addActionListener(sendAction);

         //aggiungi i componenti al pannello inferiore
         bottomPanel.add(messageTypeComboBox, BorderLayout.WEST);
         bottomPanel.add(messageField, BorderLayout.CENTER);
         bottomPanel.add(sendButton, BorderLayout.EAST);

         //aggiungi i pannelli principali al frame
         mainPanel.add(scrollPane, BorderLayout.CENTER);
         mainPanel.add(bottomPanel, BorderLayout.SOUTH);

         mainFrame.add(mainPanel);

         //aggiungi un messaggio iniziale
         addSystemMessage("Connesso al server. Benvenuto!");

         //focus sul campo di input
         messageField.requestFocusInWindow();
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
                     /*errorLabel.setText("Connessione  stabilita con successo!");
                     serverInputFrame.setVisible(false);    //quando connesso, nascondo il popup di connessione
                     //initializeMainWindow();
                     mainFrame.setVisible(true);   //e mostro la finestra principale*/

                     errorLabel.setText("Connessione stabilita con successo!");
                     serverInputFrame.setVisible(false);

                     // Passa alla finestra di login
                     initializeLoginWindow();
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

   /**
    * Metodo per aggiungere messaggi di sistema all'area della chat
    *
    * @param message Messaggio
    */
   public void addSystemMessage(String message) {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      String timestamp = sdf.format(new Date());

      chatArea.append("[" + timestamp + "] [SISTEMA]: " + message + "\n");
      scrollToBottom();
   }

   /**
    * Aggiunge on-screen i messaggi inviati
    *
    * @param type Tipo messaggio
    * @param message Messaggio
    */
   private void addOutgoingMessage(String type, String message) {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      String timestamp = sdf.format(new Date());

      chatArea.append("[" + timestamp + "] [TU] [" + type + "]: " + message + "\n");
      scrollToBottom();
   }

   /**
    * Invia messaggio al server
    */
   private void sendMessage() {
      log.debug("Premuto tasto di invio messaggio");

      String messageText = messageField.getText().trim();
      if (!messageText.isEmpty()) {
         log.debug("Il campo di testo non è vuoto");
         String messageType = (String) messageTypeComboBox.getSelectedItem();

         //aggiungi il messaggio all'area della chat
         addOutgoingMessage(messageType, messageText);

         //todo: logica per inviare il messaggio

         String messagePrefix = "";
         switch (messageType) {
            case "Messaggio Privato":{
               messagePrefix = "PRIVATE-";

               break;
            }

            case "Server (RAW)":{
               messagePrefix = "";

               break;
            }
         }

         ClientFunctionalities.sendMessageToServer(messagePrefix + messageText);

         //pulisci campo input
         messageField.setText("");
      }
   }

   /**
    * Scorre automaticamente verso il basso
    */
   private void scrollToBottom() {
      chatArea.setCaretPosition(chatArea.getDocument().getLength());
   }

   /**
    * Inizializza la finestra di login per username e password
    */
   private void initializeLoginWindow() {
      SwingUtilities.invokeLater(() -> {
         loginFrame = new JFrame("Login - Negretto Enrico");
         loginFrame.setSize(400, 250);
         loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         loginFrame.setLocationRelativeTo(null);

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());
         panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.insets = new Insets(5, 5, 5, 5);

         //label e campo per username
         JLabel usernameLabel = new JLabel("Username:");
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.gridwidth = 2;
         panel.add(usernameLabel, gbc);

         usernameField = new JTextField(20);
         gbc.gridx = 0;
         gbc.gridy = 1;
         gbc.gridwidth = 2;
         panel.add(usernameField, gbc);

         //label e campo per password (inizialmente nascosto)
         JLabel passwordLabel = new JLabel("Password:");
         gbc.gridx = 0;
         gbc.gridy = 2;
         gbc.gridwidth = 2;
         passwordLabel.setVisible(false);
         panel.add(passwordLabel, gbc);

         passwordField = new JPasswordField(20);
         gbc.gridx = 0;
         gbc.gridy = 3;
         gbc.gridwidth = 2;
         passwordField.setVisible(false);
         panel.add(passwordField, gbc);

         //label per messaggi di stato
         statusLabel = new JLabel(" ");
         statusLabel.setForeground(Color.RED);
         gbc.gridx = 0;
         gbc.gridy = 4;
         gbc.gridwidth = 2;
         panel.add(statusLabel, gbc);

         //pulsante invio
         JButton submitButton = new JButton("Invia");
         gbc.gridx = 0;
         gbc.gridy = 5;
         gbc.gridwidth = 2;
         gbc.anchor = GridBagConstraints.CENTER;
         panel.add(submitButton, gbc);

         //actionListener per il pulsante e per il tasto Invio nei campi di testo
         ActionListener submitAction = e -> {
            if (isUsernamePhase) {
               submitUsername();
            } else {
               submitPassword();
            }
         };

         submitButton.addActionListener(submitAction);
         usernameField.addActionListener(submitAction);
         passwordField.addActionListener(submitAction);

         loginFrame.add(panel);
         loginFrame.setVisible(true);

         //focus iniziale sul campo username
         usernameField.requestFocusInWindow();
      });
   }

   /**
    * Gestisce l'invio dell'username
    */
   private void submitUsername() {
      String username = usernameField.getText().trim();
      if (username.isEmpty()) {
         statusLabel.setText("Username non può essere vuoto");
         statusLabel.setForeground(Color.RED);
         return;
      }

      // Salva l'username per l'uso successivo
      currentUsername = username;

      // Invia l'username al server
      log.debug("Invio username: {}", username);
      ClientFunctionalities.sendMessageToServer("USERNAME-" + username);

      // Qui dovresti attendere la risposta dal server
      // Per simulare questo, imposta un listener che verrà chiamato quando arriva la risposta

      // Nota: il codice seguente è solo un esempio, dovrai implementare un vero meccanismo
      // per gestire le risposte dal server

      // Simuliamo una risposta positiva dal server per questo esempio
      processServerResponse("USERNAME-OK");
   }

   /**
    * Gestisce l'invio della password
    */
   private void submitPassword() {
      String password = new String(passwordField.getPassword()).trim();
      if (password.isEmpty()) {
         statusLabel.setText("Password non può essere vuota");
         statusLabel.setForeground(Color.RED);
         return;
      }

      //invia la password al server
      log.debug("Invio password per l'utente: {}", currentUsername);
      ClientFunctionalities.sendMessageToServer("PASSWORD-" + password);

      // Qui dovresti attendere la risposta dal server
      // Come nell'esempio precedente, questo è solo un esempio

      //simulazione risposta positiva dal server
      processServerResponse("PASSWORD-OK");
   }

   /**
    * Elabora la risposta del server per login
    *
    * @param response La risposta del server
    */
   public void processServerResponse(String response) {
      log.debug("Risposta dal server: {}", response);

      SwingUtilities.invokeLater(() -> {
         if (response.equals("USERNAME-OK")) {
            //username accettato, passa alla fase password
            isUsernamePhase = false;
            statusLabel.setText("Username accettato. Inserire la password.");
            statusLabel.setForeground(new Color(0, 150, 0));

            //mostra i campi per la password
            Component[] components = ((JPanel) loginFrame.getContentPane().getComponent(0)).getComponents();
            for (Component component : components) {
               if (component instanceof JLabel && ((JLabel) component).getText().equals("Password:")) {
                  component.setVisible(true);
               }
               if (component instanceof JPasswordField) {
                  component.setVisible(true);
               }
            }

            //sposta il focus sul campo password
            passwordField.requestFocusInWindow();

         } else if (response.equals("USERNAME-NOTFOUND")) {
            //username non trovato, torna alla fase username
            isUsernamePhase = true;
            statusLabel.setText("Username non trovato. Riprova.");
            statusLabel.setForeground(Color.RED);
            usernameField.setText("");
            usernameField.requestFocusInWindow();

         } else if (response.equals("PASSWORD-OK")) {
            //password corretta, procedi alla finestra della chat
            statusLabel.setText("Login riuscito!");
            statusLabel.setForeground(new Color(0, 150, 0));
            setMainWindowVisible(true);

            //chiudi la finestra di login e apri la finestra della chat
            loginFrame.setVisible(false);
            initializeMainWindow();

         } else if (response.equals("PASSWORD-WRONG")) {
            //password errata, torna alla fase username
            isUsernamePhase = true;
            statusLabel.setText("Password errata. Riprova con un altro username.");
            statusLabel.setForeground(Color.RED);

            //nascondi i campi per la password
            Component[] components = ((JPanel) loginFrame.getContentPane().getComponent(0)).getComponents();
            for (Component component : components) {
               if (component instanceof JLabel && ((JLabel) component).getText().equals("Password:")) {
                  component.setVisible(false);
               }
               if (component instanceof JPasswordField) {
                  component.setVisible(false);
                  ((JPasswordField) component).setText("");
               }
            }

            usernameField.setText("");
            usernameField.requestFocusInWindow();
         }
      });
   }
}