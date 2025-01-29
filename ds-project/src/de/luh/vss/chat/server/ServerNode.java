package de.luh.vss.chat.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ServerNode {
    private static final Logger logger = Logger.getLogger(ServerNode.class.getName());
    private static final int PORT = 4444;
    private static boolean running = true;
    private int port;

    public ServerNode(int port) {
        this.port = port;
    }
    
    public static void main(String... args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;
        new ServerNode(port).start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info("Server läuft auf Port: " + port);
            }
            while(running){
                if(!running){
                    break;
                }
                try {
                    Socket clientSocket = serverSocket.accept();
                    Thread clientThread = new Thread(() -> handleClient(clientSocket));
                    clientThread.start();
                } catch (java.io.IOException e) {
                    logger.severe("IOException: " + e.getMessage());
                }
            }
        } catch (java.io.IOException e) {
            logger.severe("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket){
        try(
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            ){
            String message = in.readLine();
            if (message != null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Nachricht erhalten: " + message);
                }
                String response = "ACK(" + PORT + "): " + message;
                out.println(response);
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Antwort gesendet: " + response);
                }
            }
        } catch (Exception e) {
            logger.severe("handleClient: " + e.getMessage());
        }
    }
}
