# Use an official OpenJDK runtime as a parent image
FROM openjdk:23-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Set environment variables
ENV DB_URL=jdbc:postgresql://postgres_primary:5432/mydb


ENV DB_USER=user
ENV DB_PASSWORD=password

# Copy the compiled server program into the container
COPY bin /app

# Copy the postgresql driver into the container
COPY ../libs/postgresql-42.7.5.jar /app/lib/

# Expose the port the server listens on
EXPOSE 4444

# Installiere ncat (optional für Tests)
RUN apt-get update && apt-get install -y ncat

# Run the server program with the port argument
CMD ["java", "-cp", ".:lib/postgresql-42.7.5.jar","de.luh.vss.chat.server.ServerNode", "4444"]
