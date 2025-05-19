package com.pt.grpc;


import com.pt.Utils;
import com.pt.kademlia.Node;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import kademlia.Kademlia;
import kademlia.KademliaServiceGrpc;
import kademlia.Kademlia.PingRequest;
import kademlia.Kademlia.PingResponse;
import kademlia.Kademlia.AddNodeResponse;
import kademlia.Kademlia.FindRequest;
import kademlia.Kademlia.NodeGrpc;
import kademlia.Kademlia.AuctionGrpc;
import kademlia.Kademlia.ProductGrpc;
import kademlia.Kademlia.BidGrpc;
import kademlia.Kademlia.AuctionResponse;
import kademlia.Kademlia.PrintRoutingTableResponse;

import java.io.IOException;


public class GrpcClient {
    private final ManagedChannel channel;
    private final KademliaServiceGrpc.KademliaServiceBlockingStub stub;

    public GrpcClient(String targetIp, int targetPort) {
        this.channel = ManagedChannelBuilder.forAddress(targetIp, targetPort)
                .usePlaintext()
                .build();
        this.stub = KademliaServiceGrpc.newBlockingStub(channel);
    }

    public void ping(String myId) {
        Kademlia.PingRequest request = PingRequest.newBuilder().setId(myId).build();
        PingResponse response = stub.ping(request);
        System.out.println("Ping: " + response.getMessage());
    }

    public void sendNode(Node node) {
        NodeGrpc protoNode = NodeGrpc.newBuilder()
                .setId(node.getId())
                .setIp(node.getIp())
                .setPort(node.getPort())
                .build();
        AddNodeResponse response = stub.addNode(protoNode);
        System.out.println("Add Node: " + response.getMessage());
    }

    public NodeGrpc findClosest(String targetId) {
        FindRequest request = FindRequest.newBuilder().setTargetId(targetId).build();
        return stub.findClosestNode(request);
    }

    public Node getTargetNode() {
        NodeGrpc protoNode = stub.getSelfNode(Kademlia.Empty.newBuilder().build());
        return new Node(protoNode.getId(), protoNode.getIp(), protoNode.getPort());
    }

    public void sendAuctionTest(){
        AuctionGrpc auction = AuctionGrpc.newBuilder()
                .setId(1)
                .addProducts(ProductGrpc.newBuilder()
                        .setId(1)
                        .setName("Phone")
                        .setInitialPrice(100.0f)
                        .setFinalPrice(150.0f)
                        .build())
                .addBids(BidGrpc.newBuilder()
                        .setId(1)
                        .setProductId(1)
                        .setBidId(2)
                        .build())
                .build();

        AuctionResponse response = stub.broadcastAuction(auction);
        System.out.println(response.getMessage());
    }

    public void printRoutingTable() {
        System.out.println("Calling printRoutingTable()...");
        PrintRoutingTableResponse response = stub.printRoutingTable(Kademlia.Empty.newBuilder().build());
        System.out.println("TABLE: " + response.getMessage());
    }

    public void getAuctions() {
        Kademlia.GetAuctionsResponse response = stub.getAuctions(Kademlia.Empty.newBuilder().build());
        System.out.println("Auctions:");
        for (Kademlia.AuctionGrpc auction : response.getAuctionsList()) {
            System.out.println("Auction ID: " + auction.getId());
            for (Kademlia.ProductGrpc product : auction.getProductsList()) {
                System.out.println("  Product: " + product.getName() + ", Initial: " + product.getInitialPrice() + ", Final: " + product.getFinalPrice());
            }
            for (Kademlia.BidGrpc bid : auction.getBidsList()) {
                System.out.println("  Bid ID: " + bid.getId() + ", Product ID: " + bid.getProductId() + ", Bidder ID: " + bid.getBidId());
            }
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        // 1. Define this node’s IP and port
        String localIp = Utils.getLocalIp();
        int localPort = Utils.getFreePort();
        Node localNode = new Node(localIp, localPort);

        // 2. Start gRPC server for this node
        GrpcServer server = new GrpcServer(localIp, localPort);
        server.start(); // async
        System.out.printf("Node started: %s:%d (ID: %s)%n", localIp, localPort, localNode.getId());

        // 3. Bootstrap contact
        String bootstrapIp = "127.0.0.1";
        int bootstrapPort = 50051;

        GrpcClient bootstrapClient = new GrpcClient(bootstrapIp, bootstrapPort);
        if (localPort != bootstrapPort) { // Don't ping yourself

            bootstrapClient.sendNode(localNode);   // Add self to bootstrap’s routing table
            bootstrapClient.printRoutingTable();
        }

        GrpcClient selfClient = new GrpcClient(localIp,localPort);
        selfClient.printRoutingTable();
        selfClient.sendAuctionTest();
        bootstrapClient.getAuctions();

        // 4. Keep server running
        server.blockUntilShutdown();



    }

    //Cria id de leilao apartir de hash de timestamp atual
    //Ora que ta envia um leilao adciona na objecto leilao no de quenha que envia leilao asi pa na ora de
    //Ora ki é pa lista leilao, busca de service leiloes que sta registado na nha no
    //Cria na service grpc um service que ta busca no apartir de informaçoes de um no
    //Tenta cria um leilao e faze broadcast del pa tudo no que sta na rede
    //Depos djobi se ta guarda leilao e routingTable na um ficheiro json e sempre que programa ta começa le quel ficheiro
}
