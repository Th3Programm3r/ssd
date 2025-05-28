package com.pt.fcup.kademlia;



import java.math.BigInteger;
import java.util.LinkedList;


public class KBucket {
    private static final int K = 20; // Max nodes per bucket
    private final LinkedList<Node> nodes = new LinkedList<>();

    public void addNode(Node node) {
        // If node exists, move to the end (LRU mechanism)
        nodes.remove(node);
        nodes.addLast(node);

        // If bucket is full, remove the least recently seen node
        if (nodes.size() > K) {
            nodes.removeFirst();
        }
    }

    public Node findClosestNodeNotEqual(String targetId) {
        Node closest = null;
        BigInteger minDistance = null;
        BigInteger target = new BigInteger(targetId, 16);

        for (Node node : nodes) {
            if (node.getId().equals(targetId)) continue; // Skip if same node

            BigInteger nodeId = new BigInteger(node.getId(), 16);
            BigInteger distance = target.xor(nodeId);

            if (minDistance == null || distance.compareTo(minDistance) < 0) {
                minDistance = distance;
                closest = node;
            }
        }
        return closest;
    }

    public Node findClosestNode(String targetId) {
        Node closest = null;
        BigInteger minDistance = null;
        BigInteger target = new BigInteger(targetId, 16);

        for (Node node : nodes) {
            BigInteger nodeId = new BigInteger(node.getId(), 16);
            BigInteger distance = target.xor(nodeId);

            if (minDistance == null || distance.compareTo(minDistance) < 0) {
                minDistance = distance;
                closest = node;
            }
        }
        return closest;
    }

    public LinkedList<Node> getNodes() {
        return nodes;
    }

    public void removeNode(Node node) {
        nodes.removeIf(_node -> _node.getId().equals(node.getId()));
    }

    public Node findNode(Node node){
        Node nodeToFind=nodes.stream().filter(_node->
                _node.getId().equals(node.getId())
        ).findFirst().get();

        return nodeToFind;
    }
}