package com.pt.kademlia;

import com.pt.Auction.Auction;
import com.pt.Utils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import kademlia.Kademlia;
import kademlia.Kademlia.PingRequest;
import kademlia.Kademlia.PingResponse;
import kademlia.KademliaServiceGrpc;
import kademlia.Kademlia.AddNodeResponse;
import kademlia.Kademlia.RemoveNodeResponse;
import kademlia.Kademlia.FindRequest;
import kademlia.Kademlia.NodeGrpc;
import kademlia.Kademlia.AuctionGrpc;
import kademlia.Kademlia.AuctionResponse;


public class KademliaServiceImpl  extends KademliaServiceGrpc.KademliaServiceImplBase{
    private final RoutingTable routingTable;


    public KademliaServiceImpl(RoutingTable routingTable) {
        this.routingTable = routingTable;
    }


    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        PingResponse response = PingResponse.newBuilder()
                .setMessage("Alive: " + routingTable.getLocalNode().getId())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void addNode(NodeGrpc request, StreamObserver<AddNodeResponse> responseObserver) {
        Node node = new Node(request.getIp(), request.getPort());
        routingTable.addNode(node);
        AddNodeResponse response = AddNodeResponse.newBuilder()
                .setMessage("Node added: " + node.getId())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void removeNode(NodeGrpc request, StreamObserver<RemoveNodeResponse> responseObserver) {
        Node node = new Node(request.getIp(), request.getPort());
        routingTable.removeNode(node);
        RemoveNodeResponse response = RemoveNodeResponse.newBuilder()
                .setMessage("Node removed: " + node.getId())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void findClosestNode(FindRequest request, StreamObserver<NodeGrpc> responseObserver) {
        Node closest = routingTable.findClosestNodeNotEqual(request.getTargetId());
        NodeGrpc response = NodeGrpc.newBuilder()
                .setId(closest.getId())
                .setIp(closest.getIp())
                .setPort(closest.getPort())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getSelfNode(Kademlia.Empty request, StreamObserver<NodeGrpc> responseObserver) {
        Node local = routingTable.getLocalNode();
        NodeGrpc response = NodeGrpc.newBuilder()
                .setId(local.getId())
                .setIp(local.getIp())
                .setPort(local.getPort())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastAuction(AuctionGrpc auction, StreamObserver<AuctionResponse> responseObserver) {
        for(KBucket kBucket: routingTable.getBuckets()) {
            for (Node node : kBucket.getNodes()) {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress(node.getIp(), node.getPort())
                            .usePlaintext()
                            .build();

                    KademliaServiceGrpc.KademliaServiceBlockingStub stub =
                            KademliaServiceGrpc.newBlockingStub(channel);

                    stub.receiveAuction(auction); // Call client method

                    channel.shutdownNow();
                } catch (Exception e) {
                    System.err.println("Broadcast to " + node.getId() + " failed: " + e.getMessage());
                }
            }
        }

        AuctionResponse response = AuctionResponse.newBuilder()
                .setMessage("Broadcasted Auction to all nodes")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void receiveAuction(AuctionGrpc request, StreamObserver<AuctionResponse> responseObserver) {
        // Convert proto Auction to your local Auction object
        Auction auction = Utils.convertAuctionFromProto(request);

        // Save it locally
        //localNode.getAuctions().add(auction);

        // React to the auction (print/log/etc.)
        System.out.println("Received auction with ID: " + auction.getId());

        // Respond
        AuctionResponse response = AuctionResponse.newBuilder()
                //.setMessage("Auction received by node yrdy" + localNode.getId())
                .setMessage("Auction received by node yrdy")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
