package de.luh.vss.chat.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class ServerNode {
	public static void main(String... args) {
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ServerNode.class.getName());

		logger.info("ServerNode starting");
		int port = 4444;
        try (DatagramSocket socket = new DatagramSocket(port)) {
            if (logger.isLoggable(java.util.logging.Level.INFO)) {
                logger.info(String.format("Server läuft auf Port: %d", port));
			}
            byte[] buffer = new byte[1024];
            while (true) {
				if(socket.isClosed()) {
					logger.severe("Socket is closed");
					break;
				}
				// Nachricht empfangen
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
				// Nachricht ausgeben
                if (logger.isLoggable(java.util.logging.Level.INFO)) {
                    logger.info(String.format("Nachricht erhalten: %s", message));
                }
                // Antwort senden
                String response = "ACK(" + port + "): " + message;
                byte[] responseBytes = response.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(
                    responseBytes, responseBytes.length, packet.getAddress(), packet.getPort()
                );
                socket.send(responsePacket);
            }

        } catch (java.net.SocketException e) {
            logger.severe("SocketException: " + e.getMessage());
        } catch (java.io.IOException e) {
            logger.severe("IOException: " + e.getMessage());
        }
	}
}
