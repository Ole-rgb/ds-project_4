package de.luh.vss.chat.loadbalancer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class LoadBalancer {
    private static final String DEFAULT_HOST = "localhost";
    private final List<Server> servers; // Liste der Backend-Server
    private int currentServerIndex = 0; // Index für Round-Robin
    private static final Logger logger = Logger.getLogger(LoadBalancer.class.getName());


    public LoadBalancer(List<Server> servers) {
        this.servers = servers;
    }

    // Methode, um den nächsten Server auszuwählen (Round-Robin)
    private synchronized Server getNextServer() {
        Server server = servers.get(currentServerIndex);
        currentServerIndex = (currentServerIndex + 1) % servers.size();
        return server;
    }

    public void start(int loadBalancerPort) {
        try (ServerSocket serverSocket = new ServerSocket(loadBalancerPort)) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info("Load Balancer gestartet auf Port "+ loadBalancerPort);
            }

            while (true) {
                // Akzeptiere eingehende Verbindung vom Client
                Socket clientSocket = serverSocket.accept();
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Anfrage von client " +  clientSocket.getInetAddress().getHostAddress());
                }

                // Wähle den nächsten Server
                Server backendServer = getNextServer();

                // Starte einen neuen Thread für die Anfrage
                new Thread(() -> handleRequest(clientSocket, backendServer)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Anfrage an Backend-Server weiterleiten
    private void handleRequest(Socket clientSocket, Server backendServer) {
        int retryCount = 3;
        while (retryCount > 0) {
            try (
                InputStream clientIn = clientSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();

                Socket serverSocket = new Socket(backendServer.getKey(), backendServer.getValue());
                InputStream serverIn = serverSocket.getInputStream();
                OutputStream serverOut = serverSocket.getOutputStream()
            ) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Verbindung zu Backend-Server " + backendServer.getKey() + ":" + backendServer.getValue() + " hergestellt");
                }
                // Weiterleiten der Anfrage vom Client zum Server
                Thread clientToServer = new Thread(() -> transferData(clientIn, serverOut));
                clientToServer.start();

                // Weiterleiten der Antwort vom Server zum Client
                transferData(serverIn, clientOut);

                // Warte, bis die Übertragung vom Client abgeschlossen ist
                clientToServer.join();
                return;

            } catch (Exception e) {
                retryCount--;
                if (retryCount == 0) {
                    logger.severe("Fehler beim Weiterleiten der Anfrage an " + backendServer.getKey() + ":" + backendServer.getValue() + " - " + e.getMessage());
                } else {
                    logger.warning("Retrying connection to " + backendServer.getKey() + ":" + backendServer.getValue());
                }
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    // Überträgt Daten zwischen zwei Streams
    private void transferData(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            logger.severe("Fehler beim Übertragen der Daten: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        List<Server> servers = Arrays.asList(

            new Server(System.getenv().getOrDefault("SERVER1_HOST", DEFAULT_HOST), 4444),
            new Server(System.getenv().getOrDefault("SERVER2_HOST", DEFAULT_HOST), 4445),
            new Server(System.getenv().getOrDefault("SERVER3_HOST", DEFAULT_HOST), 4446)
        );

        // Starte den Load Balancer
        new LoadBalancer(servers).start(8080);
    }
}

class Server extends AbstractMap.SimpleEntry<String, Integer> {
    public Server(String key, Integer value) {
        super(key, value);
    }
}