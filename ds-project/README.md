# ds-project

# What did I build
Basically I build a distributed version of the ds-assignment server.
My design is very simple. I have a loadbalancer(round-robin) that forwards the tcp requests to the servernodes.
The server nodes run in two different networks, possibly representing different datacenters. In each network the servers can communicate with eachother but there is no comminucation between servers of different networks.
The basic flow is USER-REQUEST(text-message)->LoadBalancer(round-robin to node1/2/3)->selected node
(and then back)
I also implemented a UDP-heartbeat mechanism that periodically checks if all server are still running.
To visualize I build a small api that reads the database-tables.
To test the database-reads and the effect on the api, I implemented a small testcase.
## TESTCASE:
If the chatmessage in the client 

### To compose the entire infrastructure run 
cd ds_structure
docker compose up 
(The docker-files rely on the .class files from bin, so best compile it first in eclipse).

## The api can be reached at localhost:8000, documentation you be found at localhost:8000/docs
### Potential goals:
- Leader-Follower database
- Nodes communicate internally(for "vertical"-loadsharing)
- Implementation of online user-ids/ips
- Caching 

# Running the system
### Build the server-node image from workdirectory(ds-project)
docker build -t server-node -f src/de/luh/vss/chat/server/Dockerfile .
### Run the server-node image (keep in mind that the ports can be modified to suit your needs)
docker run --name servernode-container -p 4444:4444 server-node

### Build the loadbalancer images from workdirectory(ds-project)
docker build -t loadbalancer -f src/de/luh/vss/chat/loadbalancer/Dockerfile .
### Run the loadbalancer image (keep in mind that the ports can be modified to suit your needs)
docker run --name loadbalancer-container -p 8080:8080 loadbalancer

### Build the api images from workdirectory(ds-project)
docker build -t api -f api/Dockerfile .
### Run the api image (keep in mind that the ports can be modified to suit your needs)
docker run --name api-container -p 8000:8000 api
