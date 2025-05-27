package com.pt.fcup.grpc;
import io.grpc.stub.StreamObserver;
import kademlia.Kademlia.BidNotification;
public class ConnectedClient {
    public String nodeId;
    public StreamObserver<BidNotification> observer;

    public ConnectedClient(String nodeId, StreamObserver<BidNotification> observer) {
        this.nodeId = nodeId;
        this.observer = observer;
    }
}