# ds-project

## Build the docker image from workdirectory
docker build -t server-node -f src/de/luh/vss/chat/server/Dockerfile .
## Run the image
docker run --name servernode-container -p 4444:4444/udp server-node