package de.luh.vss.chat.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ChatClient {

	public static void main(String... args) {
		try {
			System.out.println(System.getProperty("java.runtime.version"));
			new ChatClient().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() throws IOException {
		System.out.println("Congratulation for successfully setting up a client");
		
		// implement your chat client logic here
        String serverAddress = "localhost"; // IP-Adresse des Servers
        int serverPort = 4444; // Port des Servers
        String message = "Hallo, Server!"; // Nachricht, die gesendet werden soll
        
        try (DatagramSocket socket = new DatagramSocket()) {
            // Nachricht in Bytes umwandeln
            byte[] messageBytes = message.getBytes();

            // Serveradresse und Port einstellen
            InetAddress serverInetAddress = InetAddress.getByName(serverAddress);
            System.out.println(serverInetAddress);
            // Paket erstellen und senden
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverInetAddress, serverPort);
            System.out.println(packet.getAddress());
            socket.send(packet);
            System.out.println("Nachricht gesendet: " + message);

            // Antwort vom Server empfangen
            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            System.out.println("Antwort vom Server erhalten: " + response);
        }
	}

}
