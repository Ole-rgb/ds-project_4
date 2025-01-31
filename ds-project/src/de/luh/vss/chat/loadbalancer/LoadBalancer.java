package de.luh.vss.chat.loadbalancer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

public class LoadBalancer {
    private final List<Server> servers = new CopyOnWriteArrayList<>(); // Thread-safe list of servers
    private int currentServerIndex = 0; // Index für Round-Robin
    private static final Logger logger = Logger.getLogger(LoadBalancer.class.getName());
    private static final int HEARTBEAT_PORT = 8081;
    private static final int HEARTBEAT_TIMEOUT = 5500; // 5,5 > 2*2,5 (e.g. two heartbeats)
    private final Map<Server, ScheduledFuture<?>> removalTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Methode, um den nächsten Server auszuwählen (Round-Robin).
     * 
     * @return Der nächste Server in der Round-Robin-Reihenfolge.
     */
    private synchronized Server getNextServer() {
        Server server = servers.get(currentServerIndex);
        currentServerIndex = (currentServerIndex + 1) % servers.size();
        return server;
    }

    /**
     * Startet den Load Balancer und hört auf eingehende Verbindungen.
     * 
     * @param loadBalancerPort Der Port, auf dem der Load Balancer läuft.
     */
    public void start(int loadBalancerPort) {
        try (ServerSocket serverSocket = new ServerSocket(loadBalancerPort)) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info("Load Balancer gestartet auf Port "+ loadBalancerPort);
            }
            //start the heartbeat listening
            new Thread(this::listenForHeartbeats).start();
            
            while (true) {
                // Akzeptiere eingehende Verbindung vom Client
                Socket clientSocket = serverSocket.accept();
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Anfrage von Client " +  clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                }

                // Wähle den nächsten Server
                if(servers.isEmpty()) {
                	//TODO buffer the request until server is available
                    if (logger.isLoggable(Level.INFO)) {
                        logger.severe("Kein Server registriert, Anfrage gespeichert");
                        
                    }               
                    continue;
                }
                Server backendServer = getNextServer();
                // Starte einen neuen Thread für die Anfrage
                new Thread(() -> handleRequest(clientSocket, backendServer)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Leitet eine Anfrage an den Backend-Server weiter.
     * 
     * @param clientSocket  Die Socket-Verbindung zum Client.
     * @param backendServer Der Backend-Server, an den die Anfrage weitergeleitet wird.
     */
    private void handleRequest(Socket clientSocket, Server backendServer) {
        int retryCount = 3;
        while (retryCount > 0) {
            try (
                DataInputStream clientIn = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());

                Socket serverSocket = new Socket(backendServer.getKey(), backendServer.getValue());
            	DataInputStream serverIn = new DataInputStream(serverSocket.getInputStream());
            	DataOutputStream serverOut = new DataOutputStream(serverSocket.getOutputStream());
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
                break;
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

    /**
     * Überträgt Daten zwischen zwei Streams.
     * 
     * @param in  Der Eingabestream.
     * @param out Der Ausgabestream.
     */
    private void transferData(DataInputStream in, DataOutputStream out) {
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

    /**
     * Hört auf Heartbeats von den Servern.
     */
    private void listenForHeartbeats() {
        try (DatagramSocket socket = new DatagramSocket(HEARTBEAT_PORT)) {
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
            	Server server = retrieveServerData(packet);
                if (!servers.contains(server)) {
                    servers.add(server);
                    if (logger.isLoggable(Level.INFO)) {
                        logger.info("Server hinzugefügt: " + servers);
                    }
                }
                
                // Cancel any existing scheduled removal task for this server
                ScheduledFuture<?> existingTask = removalTasks.remove(server);
                if (existingTask != null) {
                    existingTask.cancel(false);
                }            
                // Schedule a new removal task
                ScheduledFuture<?> removalTask = scheduler.schedule(() -> {
                    servers.remove(server);
                    removalTasks.remove(server);
                    logger.info("Server entfernt: " + server);
                }, HEARTBEAT_TIMEOUT, TimeUnit.MILLISECONDS);

                removalTasks.put(server, removalTask);

                
            }
        } catch (IOException e) {
            logger.severe("Fehler beim Empfangen von Heartbeats: " + e.getMessage());
        }
    }
    
    /**
     * Extrahiert Serverdaten aus einem DatagramPacket.
     * 
     * @param packet Das DatagramPacket, das die Serverdaten enthält.
     * @return Ein Server-Objekt mit den extrahierten Daten.
     */
    private Server retrieveServerData(DatagramPacket packet) {
        String serverInfo = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serverInfo.split(":");
        
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
    	return new Server(host, port);
    }
    
    public static void main(String[] args) {
        // Starte den Load Balancer
        new LoadBalancer().start(8080);
    }
}

/**
 * Klasse, die einen Server repräsentiert.
 */
class Server extends AbstractMap.SimpleEntry<String, Integer> {
    public Server(String key, Integer value) {
        super(key, value);
    }
}