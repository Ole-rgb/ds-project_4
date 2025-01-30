package de.luh.vss.chat.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import java.util.logging.Level;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class ServerNode {
    private static final Logger logger = Logger.getLogger(ServerNode.class.getName());
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4444;
    private static final int HEARTBEAT_SEND_PORT = 8081;
    
    private static final int HEARTBEAT_INTERVAL = 2500; // 2,5 seconds
    private static boolean running = true;
    private static int currentPort;
    private static String nodeName;

    // Database credentials from environment variables
    private static final String DB_URL = System.getenv("DB_URL")==null ? "jdbc:postgresql://localhost:5432/mydb" : System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER")==null ? "user" : System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD")==null ? "password" : System.getenv("DB_PASSWORD");

    
    public static void main(String... args) {
        currentPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("CurrentPort: %d", currentPort));
        }
        nodeName = args.length > 1 ? args[1] : null;

        new ServerNode().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(currentPort)) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info("Server läuft auf Port: " + currentPort);
            }
            startHeartbeat();
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

    private void startHeartbeat() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try (DatagramSocket socket = new DatagramSocket()) {
                    String address = nodeName==null ?  DEFAULT_HOST: nodeName;
                    String message = address + ":" + currentPort;
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, nodeName==null ? InetAddress.getByName(DEFAULT_HOST) : InetAddress.getByName("loadbalancer"), HEARTBEAT_SEND_PORT);
                    socket.send(packet);
                } catch (IOException e) {
                    logger.severe("Fehler beim Senden des Heartbeats: " + e.getMessage());
                }
            }
        }, 0, HEARTBEAT_INTERVAL);
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
                
                // Save data to database
                saveToDatabase("7211", 1, true);

                String response = "ACK(" + currentPort + "): " + message;
                out.println(response);
                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Antwort gesendet: " + response);
                }
            }
        } catch (Exception e) {
            logger.severe("handleClient: " + e.getMessage());
        }
    }

    private static void saveToDatabase(String uid,int assignmentNr, boolean passed) {
        String insertSQL = "INSERT INTO assignment_results (uid, assignment, passed) VALUES (?, ?, ?) " +
                        "ON CONFLICT (uid, assignment) DO UPDATE SET passed = EXCLUDED.passed";
        try{
            // Ensure that the driver is loaded
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL driver loaded.");
        }catch(ClassNotFoundException e){
            logger.severe("PostgreSQL driver not found: " + e.getMessage());
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement stmt = conn.prepareStatement(insertSQL)) {

            stmt.setInt(1, Integer.parseInt(uid));
            stmt.setInt(2, assignmentNr);
            stmt.setBoolean(3, passed); 

            stmt.executeUpdate();
            
            if (logger.isLoggable(Level.INFO)) {
                logger.info("Data saved to database: " + Arrays.toString(new Object[]{uid,assignmentNr,passed}));
            }

        } catch (SQLException | NumberFormatException e) {
            logger.severe("Database Error: " + e.getMessage());
        }
    }   
}
