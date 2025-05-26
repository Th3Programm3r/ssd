package com.pt.fcup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import com.pt.fcup.Auction.Auction;
import com.pt.fcup.Auction.Bid;
import com.pt.fcup.Auction.Product;
import com.pt.fcup.BlockChain.Block;
import com.pt.fcup.BlockChain.BlockChain;
import com.pt.fcup.kademlia.Node;
import kademlia.Kademlia;
import kademlia.Kademlia.AuctionGrpc;
import kademlia.Kademlia.BidGrpc;
import kademlia.Kademlia.ProductGrpc;
import kademlia.Kademlia.NodeGrpc;
import kademlia.Kademlia.BlockGrpc;
import kademlia.Kademlia.BlockChainMap;
import kademlia.Kademlia.BlockChainGrpc;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


public class Utils {
    public static final int difficulty = 4;
    public static final String bootstrapIp = "127.0.0.1";
    public static final int bootstrapPort = 50051;

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


        return new Auction(proto.getId(), bids, products, proto.getHoursToCloseAuction(), proto.getSenderHash(),creationTimeStamp,proto.getParticipantsList(), proto.getActive());
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
                .setHoursToCloseAuction(auction.getHoursToCloseAuction())
                .setActive(auction.isActive());


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
        Node node = new Node(proto.getId(),proto.getIp(),proto.getPort(),proto.getPublicKey());

        return node;
    }

    public static NodeGrpc convertNodeToProto(Node node) {
        NodeGrpc response = NodeGrpc.newBuilder()
                .setId(node.getId())
                .setIp(node.getIp())
                .setPort(node.getPort())
                .setPublicKey(node.getPublicKey())
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
        AuctionGrpc auctionGrpc = AuctionGrpc.newBuilder().build();
        if(block.getAuction()!=null)
            auctionGrpc = convertAuctionToProto(block.getAuction());

        BlockGrpc response = BlockGrpc.newBuilder()
                .setIndex(block.getIndex())
                .setTimestamp(block.getTimestamp())
                .setAuction(auctionGrpc)
                .setPreviousHash(block.getPreviousHash())
                .setHash(block.getHash())
                .setSignature(block.getSignature())
                .build();

        return response;
    }

    public static Block convertBlockFromProto(BlockGrpc block) {
        Block response = new Block(
                block.getIndex(),
                block.getTimestamp(),
                convertAuctionFromProto(block.getAuction()),
                block.getPreviousHash(),
                block.getHash(),
                block.getSignature()
        );

        return response;
    }


    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // secure size
        return keyGen.generateKeyPair();
    }

    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static PublicKey decodePublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static BlockChainGrpc convertBlockChainToProto(BlockChain blockchain) {
        BlockChainGrpc.Builder builder = BlockChainGrpc.newBuilder();

        for(Block block: blockchain.getChain()){
            BlockGrpc blockGrpc = convertBlockToProto(block);
            builder.addChain(blockGrpc);
        }

        return builder.build();
    }

    public static BlockChain convertBlockChainFromProto(BlockChainGrpc blockchain) {
        BlockChain response = new BlockChain();
        List<Block> blocks = new ArrayList<>();
        for(BlockGrpc block: blockchain.getChainList()){
            blocks.add(convertBlockFromProto(block));
        }
        response.setChain(blocks);
        return response;
    }

    public static BlockChainMap convertBlockChainMapToProto(Map<Integer, BlockChain> blockchains) {
        BlockChainMap.Builder requestBuilder = BlockChainMap.newBuilder();
        for (Map.Entry<Integer, BlockChain> entry : blockchains.entrySet()) {
            int key = entry.getKey();
            BlockChainGrpc blockchain = Utils.convertBlockChainToProto(entry.getValue());
            requestBuilder.putBlockChain(key, blockchain);
        }


        return requestBuilder.build();
    }

    public static Map<Integer, BlockChain> convertBlockChainMapFromProto(Map<Integer, BlockChainGrpc> protoMap) {
        Map<Integer, BlockChain> blockchainMap = new HashMap<>();
        for (Map.Entry<Integer, BlockChainGrpc> entry : protoMap.entrySet()) {
            int key = entry.getKey();
            BlockChain blockchain = convertBlockChainFromProto(entry.getValue());
            blockchainMap.put(key,blockchain);
        }


        return blockchainMap;
    }

    public static String signBlock(Block block, PrivateKey privateKey) throws Exception {
        String data = block.toCanonicalString(); // instead of .toString()
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }

    public static boolean verifyBlock(Block block, String signatureBase64, PublicKey publicKey) throws Exception {
        String data = block.toCanonicalString(); // match exactly what was signed
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        return signature.verify(signatureBytes);
    }



}
