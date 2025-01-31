package de.luh.vss.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.common.Message.*;
import de.luh.vss.chat.common.MessageType;

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

    /**
     * Main method to start the server node.
     * 
     * @param args Command line arguments for port and node name.
     */
    public static void main(String... args) {
        currentPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("CurrentPort: %d", currentPort));
        }
        nodeName = args.length > 1 ? args[1] : null;

        new ServerNode().start();
    }

    /**
     * Starts the server node and listens for incoming connections.
     */
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
                    Socket lbSocket = serverSocket.accept();
                	DataInputStream in = new DataInputStream(lbSocket.getInputStream());
                	DataOutputStream out = new DataOutputStream(lbSocket.getOutputStream());
                    new Thread(() -> handleMessage(in, out)).start();
                } catch (java.io.IOException e) {
                    logger.severe("IOException: " + e.getMessage());
                }
            }
        } catch (java.io.IOException e) {
            logger.severe("IOException: " + e.getMessage());
        }
    }

    /**
     * Starts the heartbeat mechanism to send periodic heartbeats to the load balancer.
     */
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

    /**
     * Handles incoming messages from the load balancer.
     * 
     * @param in  DataInputStream to read messages from.
     * @param out DataOutputStream to send responses to.
     */
    private static void handleMessage(DataInputStream in, DataOutputStream out){
        try{
            Message receivedMsg = Message.parse(in);
            if (logger.isLoggable(Level.INFO)) {
            	logger.info("Received Message: " + receivedMsg.toString());
            }

            if(receivedMsg.getMessageType() == MessageType.CHAT_MESSAGE) {
         	    ChatMessage msg = (ChatMessage)receivedMsg;
                // compare [TEST 1 USER ID: 7211]                
                if(msg.getMessage().startsWith("TEST 1 USER ID: ")) {
                    //compare chat-message content  
                    boolean passed = msg.getMessage().equals(String.format("TEST 1 USER ID: %s", msg.getRecipient().id())); 
                    int uid = msg.getRecipient().id();
                	// Save data to database
                	saveToDatabase(String.valueOf(uid), 1, passed);

                    if(passed) {
                    	// Send passed response
                    	new ChatMessage(msg.getRecipient(), "TEST 1 USER ID CORRECTNESS PASSED").toStream(out); 
                    }else {
                    	// Send failed response
                    	new ChatMessage(msg.getRecipient(), "TEST 1 USER ID CORRECTNESS FAILED").toStream(out);
                    }
                    return;
                }
            	// Echo message that are not test messages
            	new ChatMessage(msg.getRecipient(), "ACK: " + msg.getMessage()).toStream(out); 
            }
            else if(receivedMsg.getMessageType() == MessageType.ERROR_RESPONSE) {
            	new ErrorResponse("Invalid Message Type : ERROR_RESPONSE").toStream(out);
            }
            else if(receivedMsg.getMessageType() == MessageType.REGISTER_RESPONSE) {
            	RegisterResponse regRes = (RegisterResponse)receivedMsg;
                if (logger.isLoggable(Level.INFO)) {
            	    logger.info("received REGISTER_RESPONSE: "+ regRes.toString());
                }
            	new ErrorResponse("Invalid Message Type : REGISTER_RESPONSE").toStream(out);
            }
            else if(receivedMsg.getMessageType() == MessageType.REGISTER_REQUEST) {
            	RegisterRequest regReq = (RegisterRequest)receivedMsg;
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(String.format("received REGISTER_REQUEST: %s", regReq.toString()));
                }
            	//todo save to db, return list of online users and add timer that removes user from online list after 3 minutes(if no new lease request is received)    
            }
        } catch (Exception e) {
            try {
                logger.severe("handleClient: " + e.getMessage());
                new ErrorResponse("Unknown message type").toStream(out);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Saves assignment results to the database.
     * 
     * @param uid          The user ID.
     * @param assignmentNr The assignment number.
     * @param passed       Whether the assignment was passed or not.
     */
    private static void saveToDatabase(String uid,int assignmentNr, boolean passed) {
        String insertSQL = "INSERT INTO assignment_results (uid, assignment, passed) VALUES (?, ?, ?) " +
                        "ON CONFLICT (uid, assignment) DO UPDATE SET passed = EXCLUDED.passed";

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
