# ds-project

# What did I build
Basically I build a distributed version of the ds-assignemnt server.
I didn't implement the testcases(yet).
My design is very simple. I have a loadbalancer(round-robin) that forwards the tcp requests to the servernodes.
The server nodes run in two different networks, possibly representing different datacenters. In each network the servers can communicate with eachother but there is no comminucation between servers of different networks.
The basic flow is USER-REQUEST(soon text-message)->LoadBalancer(round-robin to node1/2/3)->selected node
(and then back)
I also implemented a UDP-heartbeat mechanism that periodically checks if all server are still running.
### Potential goals:
- Shared database for students grades? And other online user-ids?
- Nodes communicate internally (why?)
- ChatMessage/Message besed communication

# Running the system
### Build the server-node image from workdirectory(ds-project)
docker build -t server-node -f src/de/luh/vss/chat/server/Dockerfile .
### Run the server-node image (keep in mind that the ports can be modified to suit your needs)
docker run --name servernode-container -p 4444:4444 server-node

### Build the loadbalancer images from workdirectory(ds-project)
docker build -t loadbalancer -f src/de/luh/vss/chat/loadbalancer/Dockerfile .
### Run the loadbalancer image (keep in mind that the ports can be modified to suit your needs)
docker run --name loadbalancer-container -p 8080:8080 loadbalancer

### To compose the entire infrastructure run 
cd ds_structure
docker compose up 
