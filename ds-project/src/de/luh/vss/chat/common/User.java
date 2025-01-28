package de.luh.vss.chat.common;

import java.net.SocketAddress;

public class User {

	private final UserId userId;
	private final SocketAddress endpoint;

	public User(UserId userId, SocketAddress endpoint) {
		this.userId = userId;
		this.endpoint = endpoint;
	}

	public UserId getUserId() {
		return userId;
	}

	public SocketAddress getEndpoint() {
		return endpoint;
	}

	public record UserId(int id) {
		public static UserId BROADCAST = new UserId(0);
		
		public UserId(int id) {
			if (id < 0 || id > 9999)
				throw new IllegalArgumentException("wrong user ID");
			this.id = id;
		}

	}

}
