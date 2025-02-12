package itts.volterra.quintab.Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @author Negretto Enrico, Battiselli Giovanni
 * @version 0.2
 */
public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args){
        try {
            Thread t = new Thread(new Server());            //avvio server
            t.setName("Serv-Main");
            t.start();
        } catch (IOException e) {                        //se lancia eccezione
            log.error("Che palle (RuntimeException): {}", e);   //log
            throw new RuntimeException(e);               //lancio eccezione anch'io perché non mi pagano per far sta roba
        }
    }
}
