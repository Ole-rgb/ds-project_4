# Use an official OpenJDK runtime as a parent image
FROM openjdk:23-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the compiled server program into the container
COPY bin /app

# Expose the port the server listens on
EXPOSE 4444

# Installiere curl (optional für Tests)
RUN apt-get update && apt-get install -y ncat

# Run the server program with the port argument
CMD ["java", "de.luh.vss.chat.server.ServerNode", "4444"]
