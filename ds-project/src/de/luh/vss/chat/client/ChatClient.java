package de.luh.vss.chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.common.Message.ChatMessage;
import de.luh.vss.chat.common.Message.ErrorResponse;
import de.luh.vss.chat.common.Message.RegisterRequest;
import de.luh.vss.chat.common.MessageType;
import de.luh.vss.chat.common.User;
import de.luh.vss.chat.common.User.UserId;


public class ChatClient {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
    
    private static final int MAX_MESSAGE_LENGTH = 4000;//4KB
    private static final int LEASE_RENEWAL_PERIOD = 240000;//4 Minutes
    private static final int MAX_LEASE_RENEWAL_DUCATION = 1800000;//30 Minutes

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int UID = 7211;
    private static final String CHAT_MESSAGE = "TEST 1 USER ID: " + UID;
    
    /**
     * Main method to start the ChatClient.
     * @param args Command line arguments
     */
	public static void main(String... args) {
		try {
			new ChatClient().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    /**
     * Starts the ChatClient by establishing a connection to the server,
     * registering the user, and starting the message listener thread.
     * @throws IOException If an I/O error occurs
     */
	public void start() throws IOException {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Client gestartet");
        }
       
		// Create user
		UserId uId = new UserId(UID);
		InetSocketAddress endpoint = new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT);
		User user = new User(uId, endpoint);

        // Establish a TCP connection to the server
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
		DataInputStream in = new DataInputStream(socket.getInputStream());
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		
		// Start thread that listens for incoming messages 
		new Thread(() -> displayMessages(in, out, user)).start();
		
        // Send a chat message to the server that triggers exemplary test case behavior
        new ChatMessage(user.getUserId(), CHAT_MESSAGE).toStream(out);
        out.flush();

		// Register with Server
		new Thread(() -> {
			try {
				renew_lease(true, out,new RegisterRequest(uId, InetAddress.getLocalHost(), SERVER_PORT));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
	
	}
	
    /**
     * Listens for incoming messages from the server and handles them accordingly.
     * @param in DataInputStream to read messages from the server
     * @param out DataOutputStream to send messages to the server
     * @param user The user object representing the client
     */
	private void displayMessages(DataInputStream in, DataOutputStream out, User user) {		
		while(true) {
			try {		
				Message message = Message.parse(in);
				// Check the length of a given chat message
				if(message.getMessageType() == MessageType.CHAT_MESSAGE) {
					handleChatMessage((ChatMessage) message, out, user, false, null);
				}
				// Check if the server sent a error message
				else if(message.getMessageType() == MessageType.ERROR_RESPONSE) {
					handleErrorResponse((ErrorResponse)message);
				}else{
                    logger.severe("displayMessages: Unhandled message type: " + message.getMessageType().toString());
                }
			}			
			catch(IOException e) {
				logger.severe("displayMessages(Parsing Error): " + e);
				break;
			}
			catch(IllegalStateException | ReflectiveOperationException e) {
				logger.severe("displayMessages: " + e.getMessage());
			}
		}
	}
	
    /**
     * Periodically renews the lease by sending a register request to the server.
     * @param keepRenewing Boolean flag to keep renewing the lease
     * @param out DataOutputStream to send the register request
     * @param registerRequest The register request message
     */
	private void renew_lease(Boolean keepRenewing, DataOutputStream out, Message.RegisterRequest registerRequest) {
        long startTime = System.currentTimeMillis();
    	while(keepRenewing) {       
    		try {
		        registerRequest.toStream(out);
		        Thread.sleep(LEASE_RENEWAL_PERIOD);
			} catch (IOException |InterruptedException e) {
				logger.severe(e.getMessage());		
			}

    		if(System.currentTimeMillis() - startTime >= MAX_LEASE_RENEWAL_DUCATION) {
    			keepRenewing = false;
    		}
    	}
	}

    /**
     * Handles error responses from the server.
     * @param errorMsg The error response message
     */
    private void handleErrorResponse(ErrorResponse errorMsg) {
		logger.severe("PrintErrorResponseMessage: " + errorMsg.toString());
		System.exit(0);
	}

    /**
     * Handles chat messages from the server, checks their length, and optionally echoes them back.
     * @param chatMsg The chat message received from the server
     * @param out DataOutputStream to send messages to the server
     * @param user The user object representing the client
     * @param considerPattern Boolean flag to consider a specific pattern in the message
     * @param pattern The pattern to check in the message
     */
    private void handleChatMessage(ChatMessage chatMsg, DataOutputStream out, User user, Boolean considerPattern, String pattern) {
		try {
			int msgLength = chatMsgBytes(chatMsg);
			if (msgLength > MAX_MESSAGE_LENGTH) {
				int byteDiff = msgLength - MAX_MESSAGE_LENGTH;
				ChatMessage responseMessage = new ChatMessage(user.getUserId(), String.valueOf(byteDiff));
				logger.severe(responseMessage.toString());		
				return;
			}
            // Log the chat message
            logger.info("Received Message: " + chatMsg.toString());

            // Check if the chat message contains a specific pattern and echo it(previous implementation)
			if(considerPattern && chatMsg.toString().contains(pattern)) {
				chatMsg.toStream(out);
			}
		}
		catch(IOException e) {
			logger.severe("handleChatMessage: " + e.toString());
		}
	}
	
	/**
	 * Calculates the byte length of the given chat-message.
	 * @param msg The chat-message to check for length 
	 * @return The byte length of the given chat-message
	 */
	private int chatMsgBytes(ChatMessage msg) {
		//STUDIP suggestion: "general assumption in Java that a char is usually 2 bytes long"
		return msg.getMessage().length()*2;
	}
}
