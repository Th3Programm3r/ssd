package com.pt.fcup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import com.pt.fcup.Auction.Auction;
import com.pt.fcup.Auction.Bid;
import com.pt.fcup.Auction.Product;
import com.pt.fcup.BlockChain.Block;
import com.pt.fcup.kademlia.Node;
import kademlia.Kademlia.AuctionGrpc;
import kademlia.Kademlia.BidGrpc;
import kademlia.Kademlia.ProductGrpc;
import kademlia.Kademlia.NodeGrpc;
import kademlia.Kademlia.BlockGrpc;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;


public class Utils {
    public static Auction convertAuctionFromProto(AuctionGrpc proto) {
        List<Bid> bids = proto.getBidsList().stream()
                .map(b -> new Bid(b.getId(), b.getProductId(), b.getBidValue(),b.getSender(),b.getAuctionId()))
                .collect(Collectors.toList());

        List<Product> products = proto.getProductsList().stream()
                .map(p -> new Product(p.getId(), p.getName(), p.getInitialPrice(), p.getFinalPrice()))
                .collect(Collectors.toList());

        Instant creationTimeStamp=
                Instant.ofEpochSecond(
                        proto.getCreationTimeStamp().getSeconds(),
                        proto.getCreationTimeStamp().getNanos()
                )
        ;


        return new Auction(proto.getId(), bids, products, proto.getHoursToCloseAuction(), proto.getSenderHash(),creationTimeStamp,proto.getParticipantsList());
    }

    public static AuctionGrpc convertAuctionToProto(Auction auction) {
        Timestamp protoTimestamp = Timestamp.newBuilder()
                .setSeconds(auction.getCreationTimeStamp().getEpochSecond())
                .setNanos(auction.getCreationTimeStamp().getNano())
                .build();
        AuctionGrpc.Builder builder = AuctionGrpc.newBuilder()
                .setId(auction.getId())
                .setSenderHash(auction.getSenderHash())
                .setCreationTimeStamp(protoTimestamp)
                .setHoursToCloseAuction(auction.getHoursToCloseAuction());


        for (Bid bid : auction.getBids()) {
            builder.addBids(BidGrpc.newBuilder()
                    .setId(bid.getId())
                    .setProductId(bid.getProductId())
                    .setBidValue(bid.getBidValue())
                    .setSender(bid.getSender())
                    .setAuctionId(bid.getAuctionId())
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
        for(String participant: auction.getParticipants()){
            builder.addParticipants(participant);
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

    public static BidGrpc convertBidToProto(Bid bid) {
        BidGrpc response = BidGrpc.newBuilder()
                .setId(bid.getId())
                .setProductId(bid.getProductId())
                .setBidValue(bid.getBidValue())
                .setSender(bid.getSender())
                .setAuctionId(bid.getAuctionId())
                .build();

        return response;
    }

    public static Bid convertBidFromProto(BidGrpc proto) {
        Bid bid = new Bid(proto.getId(),proto.getProductId(),proto.getBidValue(),proto.getSender(), proto.getAuctionId());
        return bid;
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

    // Save a list of products to a JSON file
    public static void saveProductToJsonFile(String filename, List<Product> productList) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File(filename), productList);
            System.out.println("Products saved to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load a list of products from a JSON file
    public static List<Product> loadProductFromJsonFile(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(filename), new TypeReference<List<Product>>() {});
        } catch (IOException e) {
            System.out.println("Failed to load products from " + filename + ": " + e.getMessage());
            return null;
        }
    }

    public static BlockGrpc convertBlockToProto(Block block) {
        BidGrpc bidGrpc = convertBidToProto(block.getBid());
        BlockGrpc response = BlockGrpc.newBuilder()
                .setIndex(block.getIndex())
                .setTimestamp(block.getTimestamp())
                .setBid(bidGrpc)
                .setPreviousHash(block.getPreviousHash())
                .setHash(block.getHash())
                .build();

        return response;
    }

}
