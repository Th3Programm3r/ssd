package com.pt;

import com.pt.Auction.Auction;
import com.pt.Auction.Bid;
import com.pt.Auction.Product;
import com.pt.kademlia.Node;
import kademlia.Kademlia.AuctionGrpc;
import kademlia.Kademlia.BidGrpc;
import kademlia.Kademlia.ProductGrpc;
import kademlia.Kademlia.NodeGrpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;


public class Utils {
    public static Auction convertAuctionFromProto(AuctionGrpc proto) {
        List<Bid> bids = proto.getBidsList().stream()
                .map(b -> new Bid(b.getId(), b.getProductId(), b.getBidId()))
                .collect(Collectors.toList());

        List<Product> products = proto.getProductsList().stream()
                .map(p -> new Product(p.getId(), p.getName(), p.getInitialPrice(), p.getFinalPrice()))
                .collect(Collectors.toList());

        return new Auction(proto.getId(), bids, products);
    }

    public static AuctionGrpc convertAuctionToProto(Auction auction) {
        AuctionGrpc.Builder builder = AuctionGrpc.newBuilder()
                .setId(auction.getId());

        for (Bid bid : auction.getLances()) {
            builder.addBids(BidGrpc.newBuilder()
                    .setId(bid.getId())
                    .setProductId(bid.getProductId())
                    .setBidId(bid.getBidId())
                    .build());
        }

        for (Product product : auction.getProducts()) {
            builder.addProducts(ProductGrpc.newBuilder()
                    .setId(product.getId())
                    .setName(product.getName())
                    .setInitialPrice(product.getInitialPrice())
                    .setFinalPrice(product.getFinalPrice())
                    .build());
        }

        return builder.build();
    }

    public static Node convertNodeFromProto(NodeGrpc proto) {
        Node node = new Node(proto.getId(),proto.getIp(),proto.getPort());

        return node;
    }

    public static NodeGrpc convertNodeToProto(Node node) {
        NodeGrpc response = NodeGrpc.newBuilder()
                .setId(node.getId())
                .setIp(node.getIp())
                .setPort(node.getPort())
                .build();

        return response;
    }


    public static int getFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("No free port found", e);
        }
    }

    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to get IP", e);
        }
    }
}
