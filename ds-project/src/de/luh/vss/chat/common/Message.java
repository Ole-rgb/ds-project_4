package de.luh.vss.chat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import de.luh.vss.chat.common.User.UserId;

public abstract class Message {

	public static class RegisterRequest extends Message {

		private final UserId id;
		private final InetAddress address;
		private final int port;

		public RegisterRequest(final UserId id, final InetAddress address, final int port) {
			this.id = id;
			this.address = address;
			this.port = port;
		}

		public RegisterRequest(final DataInputStream in) throws IOException {
			this.id = new UserId(in.readInt());
			this.address = InetAddress.getByName(in.readUTF());
			this.port = in.readInt();
		}

		@Override
		public MessageType getMessageType() {
			return MessageType.REGISTER_REQUEST;
		}

		@Override
		public void toStream(final DataOutputStream out) throws IOException {
			out.writeInt(MessageType.REGISTER_REQUEST.msgType());
			out.writeInt(id.id());
			out.writeUTF(address.getCanonicalHostName());
			out.writeInt(port);
		}

		public UserId getUserId() {
			return id;
		}

		public InetAddress getUDPAddress() {
			return address;
		}

		public int getUDPPort() {
			return port;
		}

		@Override
		public String toString() {
			return "REGISTER_REQUEST (" + id + ", " + address.getCanonicalHostName() + ":" + port + ")";
		}

	}

	public static class RegisterResponse extends Message {

		public RegisterResponse() {

		}

		public RegisterResponse(final DataInputStream in) {

		}

		@Override
		public MessageType getMessageType() {
			return MessageType.REGISTER_RESPONSE;
		}

		@Override
		public void toStream(final DataOutputStream out) throws IOException {
			out.writeInt(MessageType.REGISTER_RESPONSE.msgType());
		}

		@Override
		public String toString() {
			return "REGISTER_RESPONSE ()";
		}

	}

	public static class ErrorResponse extends Message {

		private final String errorMsg;

		public ErrorResponse(final Exception e) {
			this.errorMsg = e.getMessage();
		}

		public ErrorResponse(final DataInputStream in) throws IOException {
			errorMsg = in.readUTF();
		}

		public ErrorResponse(final String e) {
			this.errorMsg = e;
		}

		@Override
		public void toStream(final DataOutputStream out) throws IOException {
			out.writeInt(MessageType.ERROR_RESPONSE.msgType());
			out.writeUTF(errorMsg);
		}

		@Override
		public MessageType getMessageType() {
			return MessageType.ERROR_RESPONSE;
		}

		@Override
		public String toString() {
			return "ERROR_RESPONSE (" + errorMsg + ")";
		}

	}

	public static class ChatMessage extends Message {

		private final UserId recipient;
		private final String msg;

		public ChatMessage(final UserId recipient, final String msg) {
			this.recipient = recipient;
			this.msg = msg;
		}

		public ChatMessage(final DataInputStream in) throws IOException {
			this.recipient = new UserId(in.readInt());
			this.msg = in.readUTF();
		}

		@Override
		public void toStream(final DataOutputStream out) throws IOException {
			out.writeInt(MessageType.CHAT_MESSAGE.msgType());
			out.writeInt(recipient.id());
			out.writeUTF(msg);
		}

		@Override
		public MessageType getMessageType() {
			return MessageType.CHAT_MESSAGE;
		}

		public UserId getRecipient() {
			return recipient;
		}

		public String getMessage() {
			return msg;
		}

		@Override
		public String toString() {
			return "CHAT_MESSAGE (to " + recipient + ": '" + msg + "')";
		}
	}

	public static Message parse(final DataInputStream in) throws IOException, ReflectiveOperationException {
		return MessageType.fromInt(in.readInt(), in);
	}

	public abstract void toStream(final DataOutputStream out) throws IOException;

	public abstract MessageType getMessageType();
}
