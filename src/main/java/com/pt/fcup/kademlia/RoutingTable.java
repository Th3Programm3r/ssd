package com.pt.fcup.kademlia;



import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pt.fcup.Auction.Auction;
import com.pt.fcup.Auction.Bid;


public class RoutingTable {
    private static final int ID_BITS = 160; // SHA-1 bit length
    private List<KBucket> buckets = new ArrayList<>();
    private Node localNode;
    private Map<String, InetSocketAddress> nodeIdToAddressMap = new HashMap<>();

    public List<KBucket> getBuckets() { return buckets; }
    public void setBuckets(List<KBucket> buckets) { this.buckets = buckets; }

    public Node getLocalNode() { return localNode; }
    public void setLocalNode(Node localNode) { this.localNode = localNode; }

    public RoutingTable() {}

    private List<Auction> auctions = new ArrayList<>();
    private List<Auction> participatingAuctions = new ArrayList<>();

    private HashMap<String,String> digitalSignatures=new HashMap<>();

    public RoutingTable(Node localNode) {
        this.localNode = localNode;
        for (int i = 0; i < ID_BITS; i++) {
            buckets.add(new KBucket());
        }
    }

    private int getBucketIndex(String nodeId) {
        BigInteger distance = new BigInteger(localNode.getId(), 16).xor(new BigInteger(nodeId, 16));
        return ID_BITS - distance.bitLength(); // Determines the prefix length
    }

    public void addNode(Node node) {
        nodeIdToAddressMap.put(node.getId(), new InetSocketAddress(node.getIp(), node.getPort()));
        int index = getBucketIndex(node.getId());
        buckets.get(index).addNode(node);
    }

    public void addNodeWithClosestNode(Node node, Node closestNode) {
        int closestNodeIndex = getBucketIndex(closestNode.getId());
        int newNodeIndex = getBucketIndex(node.getId());
        int bucketIndex = Math.min(closestNodeIndex, newNodeIndex);
        buckets.get(bucketIndex).addNode(node);
    }

    public Node findClosestNode(String targetId) {
        int index = getBucketIndex(targetId);
        KBucket closestBucket = buckets.get(index);

        if (!closestBucket.getNodes().isEmpty()) {
            return closestBucket.findClosestNode(targetId);
        }

        // If empty, search neighboring buckets
        for (KBucket bucket : buckets) {
            if (!bucket.getNodes().isEmpty()) {
                return bucket.findClosestNode(targetId);
            }
        }
        return null; // No nodes found
    }
    //ping
    //Store
    //FindNode
    //FindValue
    public Node findClosestNodeNotEqual(String targetId) {
        int index = getBucketIndex(targetId);
        KBucket closestBucket = buckets.get(index);

        Node closestNode = closestBucket.findClosestNodeNotEqual(targetId);

        // If empty, search neighboring buckets
        if (closestNode == null) {
            for (KBucket bucket : buckets) {
                closestNode = bucket.findClosestNodeNotEqual(targetId);
                if (closestNode != null) {
                    break;
                }
            }
        }

        return closestNode;
    }

    public BigInteger calculateDistanceBetweenNodes(String node1Key, String node2Key) {
        BigInteger id1 = new BigInteger(node1Key, 16);
        BigInteger id2 = new BigInteger(node2Key, 16);
        return id1.xor(id2);
    }


    public void printRoutingTable() {
        System.out.println("Routing Table for Node: " + localNode.getId());
        for (int i = 0; i < ID_BITS; i++) {
            KBucket bucket = buckets.get(i);
            if (!bucket.getNodes().isEmpty()) {
                System.out.println("Bucket " + i + ": ");
                for (Node node : bucket.getNodes()) {
                    System.out.println("  - " + node.getId()+","+node.getIp()+","+node.getPort());
                }
            }
        }
    }

    public String printRoutingTableToString() {
        String routingTableString="Routing Table for Node: " + localNode.getId()+"\n";

        for (int i = 0; i < ID_BITS; i++) {
            KBucket bucket = buckets.get(i);
            if (!bucket.getNodes().isEmpty()) {
                routingTableString+="Bucket " + i + ": ";
                for (Node node : bucket.getNodes()) {
                    routingTableString+="  - " + node.getId()+","+node.getIp()+","+node.getPort();
                }
            }
        }
        return routingTableString;
    }


    public void ping() {
        System.out.println("Pinging all nodes");
        for (int i = 0; i < ID_BITS; i++) {
            KBucket bucket = buckets.get(i);
            if (!bucket.getNodes().isEmpty()) {
                for (Node node : bucket.getNodes()) {
                    if(!localNode.getId().equals(node.getId()))
                        sendPing(node);
                }
            }
        }
    }

    public void sendPing(Node node){
        String url = "http://"+node.getIp()+":"+node.getPort();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Ping successful! Status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (Exception e) {
            System.out.println("Ping failed: " + e.getMessage());
            removeNode(node);
        }
    }

    public void removeNode(Node node) {
        int index = getBucketIndex(node.getId());
        buckets.get(index).removeNode(node);
        sendDeleteNotificationSameBucket(index,node);
    }

    public void saveToJsonFile(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File(filename), this);
            System.out.println("Routing table saved to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RoutingTable loadFromJsonFile(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(filename), RoutingTable.class);
        } catch (IOException e) {
            System.out.println("No previous routing table found or failed to load.");
            return null;
        }
    }

    public void sendDeleteNotificationSameBucket(int index, Node nodeDeleted){
        LinkedList<Node> nodes = buckets.get(index).getNodes();

        for (Node node : nodes) {
            String url = "http://"+node.getIp()+":"+node.getPort()+"/removedNode?node="+nodeDeleted.getIp()+":"+nodeDeleted.getPort();

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.out.println("Notification wasnt sent to node " + node.getIp()+":"+node.getPort());
            }
        }
    }

    public void sendAddNotificationSameBucket(int index, Node nodeDeleted){
        LinkedList<Node> nodes = buckets.get(index).getNodes();

        for (Node node : nodes) {
            String url = "http://"+node.getIp()+":"+node.getPort()+"/newNode?node="+nodeDeleted.getIp()+":"+nodeDeleted.getPort();

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.out.println("Notification wasnt sent to node " + node.getIp()+":"+node.getPort());
            }
        }
    }

    public Node findNodeByIpAndPort(String ip, int port){
        Node node = new Node(ip, port);
        int index = getBucketIndex(node.getId());
        Node nodeToBeReturned = buckets.get(index).findNode(node);
        return nodeToBeReturned;
    }

    public Node findNodeByHash(String hash){
        InetSocketAddress address = nodeIdToAddressMap.get(hash);
        if (address != null) {
            String ip = address.getHostString();
            int port = address.getPort();
            Node node = findNodeByIpAndPort(ip,port);
            return node;
        }

        return null;
    }

    public List<Auction> getAuctions() {
        return auctions;
    }

    public void setAuctions(List<Auction> auctions) {
        this.auctions = auctions;
    }

    public List<Auction> getParticipatingAuctions() {
        return participatingAuctions;
    }

    public void setParticipatingAuctions(List<Auction> participatingAuctions) {
        this.participatingAuctions = participatingAuctions;
    }

    public void addAuction(Auction auction) {
        auctions.add(auction);
    }

    public void addParticipatingAuction(Auction auction) {
        participatingAuctions.add(auction);
    }

    public void addBidToAuction(Bid newBid) {
        Auction targetAuction = auctions.stream()
                .filter(a -> a.getId() == newBid.getAuctionId())
                .findFirst()
                .orElse(null);

        boolean bidAlreadyExists = targetAuction.getBids().stream().anyMatch(b ->
                b.getAuctionId() == newBid.getAuctionId() &&
                        b.getProductId() == newBid.getProductId() &&
                        b.getBidValue() == newBid.getBidValue() &&
                        b.getSender().equals(newBid.getSender())
        );
        if(!bidAlreadyExists) {
            //check if an auction has any bid if not it initializes the bid array
            if (targetAuction.getBids() == null) {
                targetAuction.setBids(new ArrayList<>());
            }
            targetAuction.getBids().add(newBid);
            //add the user to current list of participants of an auction
            if (targetAuction.getParticipants() == null || targetAuction.getParticipants().isEmpty()) {
                targetAuction.setParticipants(new ArrayList<>());
            }

            // Add sender if not already a participant
            if (!targetAuction.getParticipants().contains(newBid.getSender())) {
                targetAuction.getParticipants().add(newBid.getSender());
            }
        }
    }

    public Auction getAuctionById(int auctionId){
        return this.auctions.stream().filter(auction->
            auction.getId()==auctionId
        )
        .findFirst()
        .orElse(null);
    }

    public void addDigitalSignature(String key,String signature){

    }

}
