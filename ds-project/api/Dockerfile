# Use an official Python runtime as a parent image
FROM python:3.10-slim

# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app

# Copy only the api.py and the requirements.txt files into the container
COPY api/api.py /app/api.py
COPY api/requirements.txt /app/requirements.txt

# Install any needed packages specified in requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Make port 8000 available to the world outside this container
EXPOSE 8000:8000

# Define environment variable
ENV DB_USER=user
ENV DB_PASSWORD=password
ENV DB_HOST=postgres_primary
ENV DB_PORT=5432
# Run app.py when the container launches
CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8000"]


