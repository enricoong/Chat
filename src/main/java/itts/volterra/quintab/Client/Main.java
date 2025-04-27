package itts.volterra.quintab.Client;

import itts.volterra.quintab.Client.UserInterface.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Negretto Enrico, Battiselli Giovanni
 * @version 0.2
 */
public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args){
        //ClientFunctionalities client = new ClientFunctionalities();
        //new Thread(new Window(client)).start();
        new Thread(new Client()).start();           //avvio client
    }
}