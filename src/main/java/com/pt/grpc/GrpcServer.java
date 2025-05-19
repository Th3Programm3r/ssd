package com.pt.grpc;

import com.pt.Utils;
import com.pt.kademlia.KademliaServiceImpl;
import com.pt.kademlia.Node;
import com.pt.kademlia.RoutingTable;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;



public class GrpcServer {
    private final int port;
    private final String ip;
    private final Server server;
    private final RoutingTable routingTable;


    public GrpcServer(String ip, int port) {
        this.ip = ip;
        this.port = port;

        Node localNode = new Node(ip, port); // Generates ID based on IP and port
        this.routingTable = new RoutingTable(localNode);
        this.server = ServerBuilder
                .forPort(port)
                .addService(new KademliaServiceImpl(routingTable))
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.printf("Kademlia server started at %s:%d (ID: %s)%n", ip, port, routingTable.getLocalNode().getId());
    }

    public void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }

    public Node getLocalNode() {
        return routingTable.getLocalNode();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        GrpcServer grpcServer = new GrpcServer("127.0.0.1", 50051); // example node
        grpcServer.start();
        grpcServer.blockUntilShutdown();
    }
}
