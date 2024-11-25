//package itts.volterra.quintab;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.ConnectException;
//import java.net.InetAddress;
//import java.net.Socket;
//import java.net.UnknownHostException;
//
//public class ClientSocket {
//    private final static int PORT = 12345;                      //porta
//    private static final Logger log = LogManager.getLogger(ClientSocket.class);
//    private static Socket client;                               //oggetto socket
//    private InetAddress host = InetAddress.getLocalHost();      //prendo ip macchina
//    private PrintWriter out;                                    //invia messaggio a server
//    private static BufferedReader in;                           //ricevi messaggio
//
//    public ClientSocket(String nome) throws UnknownHostException {
//        try {
//            client = new Socket(host.getHostName(), PORT);
//            log.debug("Server connesso");
//            out = new PrintWriter(client.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public ClientSocket(String nome, String hostName) throws UnknownHostException, ConnectException {
//        try {
//            client = new Socket(hostName, PORT);
//            log.debug("Server connesso");
//            out = new PrintWriter(client.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
//        } catch (ConnectException connectException){
//            throw connectException;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Mi permette di inviare un messaggio al server
//     *
//     * @param msg Messaggio da inviare
//     * @return Risposta ricevuta (è inizializzata a 'null')
//     */
//    public String sendMessage(String msg){
//        out.println(msg); //invia messaggio
//        String response = null;
//        try {
//            response = in.readLine();   //ritorna risposta
//        } catch (IOException e){
//            e.printStackTrace();
//        }
//
//        //System.out.println("Risposta del server: " + response);   //debug
//
//        return response;
//    }
//
//    public static Socket getClient() {
//        return client;
//    }
//
//    public static BufferedReader getIn() {
//        return in;
//    }
//}