package itts.volterra.quintab.Client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Negretto Enrico, Battiselli Giovanni
 * @version 0.2
 */
public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args){
        new Thread(new Client()).start();           //avvio client
    }
}