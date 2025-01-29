package de.luh.vss.chat.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ChatClient {
    private static Logger logger = Logger.getLogger(ChatClient.class.getName());

	public static void main(String... args) {
		try {
			new ChatClient().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() throws IOException {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Client gestartet");
        }
	
		// implement your chat client logic here
        String serverAddress = "localhost"; // IP-Adresse des Servers
        int serverPort = 4444; // Port des Servers
        String message = "Hallo, Server!"; // Nachricht, die gesendet werden soll
        
        try (DatagramSocket socket = new DatagramSocket()) {
            // Nachricht in Bytes umwandeln
            byte[] messageBytes = message.getBytes();

            // Serveradresse und Port einstellen
            InetAddress serverInetAddress = InetAddress.getByName(serverAddress);
            if(logger.isLoggable(Level.INFO)){
                logger.info("Serveradresse: " + serverInetAddress);
            }
            // Paket erstellen und senden
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverInetAddress, serverPort);
            if(logger.isLoggable(Level.INFO)){
                logger.info("Paketadresse: " + packet.getAddress());
            }
            socket.send(packet);
            if(logger.isLoggable(Level.INFO)){
                logger.info("Nachricht gesendet: " + message);
            }

            // Antwort vom Server empfangen
            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            if(logger.isLoggable(Level.INFO)){
                logger.info("Antwort vom Server erhalten: " + response);
            }
        }
	}

}
