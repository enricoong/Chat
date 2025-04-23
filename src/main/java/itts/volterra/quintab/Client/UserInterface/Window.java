package itts.volterra.quintab.Client.UserInterface;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class Window extends JFrame implements Runnable {
   private static final Logger log = LogManager.getLogger(Window.class);

   public Window() {
      try {
         FlatDarkLaf.setup();
      } catch (Exception e) {
         log.error("Errore durante l'inizializzazione del tema FlatLaf: {}", e.getMessage());
      }
   }

   public void run() {
      //TODO creare
      
   }
}