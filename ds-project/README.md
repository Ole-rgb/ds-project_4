# ds-project

## Build the server-node image from workdirectory(ds-project)
docker build -t server-node -f src/de/luh/vss/chat/server/Dockerfile .
## Run the server-node image (keep in mind that the ports can be modified to suit your needs)
docker run --name servernode-container -p 4444:4444 server-node

## Build the loadbalancer images from workdirectory(ds-project)
docker build -t loadbalancer -f src/de/luh/vss/chat/loadbalancer/Dockerfile .
## Run the loadbalancer image (keep in mind that the ports can be modified to suit your needs)
docker run --name loadbalancer-container -p 8080:8080 loadbalancer

## To compose the entire infrastructure run 
cd ds_structure
docker compose up 