package com.pt.fcup.kademlia;


import com.pt.fcup.Auction.Auction;
import com.pt.fcup.Auction.Bid;
import com.pt.fcup.BlockChain.Block;
import com.pt.fcup.BlockChain.BlockChain;
import com.pt.fcup.Utils;
import com.pt.fcup.grpc.GrpcClient;
import io.grpc.Status;
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
import kademlia.Kademlia.PrintRoutingTableResponse;
import kademlia.Kademlia.BidGrpc;
import kademlia.Kademlia.SendBidResponse;
import kademlia.Kademlia.BlockGrpc;
import kademlia.Kademlia.BlockAck;
import kademlia.Kademlia.BlockHash;
import kademlia.Kademlia.BlockChainMap;
import kademlia.Kademlia.Empty;
import kademlia.Kademlia.AddBlockChainsResponse;
import kademlia.Kademlia.BlockChainGrpc;
import kademlia.Kademlia.HashGrpc;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
        Node newNode = Utils.convertNodeFromProto(request);
        routingTable.addNode(newNode);

        for(KBucket kBucket: routingTable.getBuckets()) {
            for (Node peer : kBucket.getNodes()) {
                if (!peer.getId().equals(newNode.getId())) {
                    try {
                        GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                        client.addNodeToRoutingTable(newNode);
                        System.out.println("Forwarded new node to: " + peer);
                    } catch (Exception e) {
                        System.err.println("Failed to notify " + peer + ": " + e.getMessage());
                    }
                }
            }
        }

        AddNodeResponse response = AddNodeResponse.newBuilder()
                .setMessage("Node added: " + newNode.getId()+" and propagated")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void addNodeToRoutingTable(NodeGrpc request, StreamObserver<AddNodeResponse> responseObserver) {
        Node newNode = Utils.convertNodeFromProto(request);
        routingTable.addNode(newNode);


        AddNodeResponse response = AddNodeResponse.newBuilder()
                .setMessage("Node added: " + newNode.getId()+" to routing table")
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
    public void getSelfNode(Empty request, StreamObserver<NodeGrpc> responseObserver) {
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
    public void broadcastAuction(BlockGrpc blockGrpc, StreamObserver<AuctionResponse> responseObserver) {
        try {
            Block block = Utils.convertBlockFromProto(blockGrpc);
            String blockSignature = block.getSignature();

            String blockHash = block.getAuction().getSenderHash();
            String publicKeyString = routingTable.getPublicKeyByHash(blockHash);

            PublicKey publicKey = null;
            if (publicKeyString != null && !publicKeyString.isEmpty()) {
                publicKey = Utils.decodePublicKey(publicKeyString);
            }

            // If public key is missing, request from bootstrap first
            if (publicKey == null && !routingTable.getLocalNode().getIp().equals(Utils.bootstrapIp) && routingTable.getLocalNode().getPort()!=Utils.bootstrapPort) {
                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);

                Node node = bootstrapClient.findNodeByHash(blockHash);
                if (node != null) {
                    routingTable.addNode(node);
                    publicKeyString = routingTable.getPublicKeyByHash(blockHash);
                    publicKey = Utils.decodePublicKey(publicKeyString);
                }
            }

            Block blockToVerify=block;
            blockToVerify.setSignature("");
            boolean verify=publicKey!=null?Utils.verifyBlock(blockToVerify, blockSignature, publicKey):false;

            // Only add to blockchain if verification is successful
            if (verify) {
                BlockChain blockChain = new BlockChain();
                blockChain.addBlockToBlockChain(block);
                routingTable.addToBlockChains(block.getAuction().getId(),blockChain);
            } else {
                System.err.println("Invalid block signature or missing public key");
            }

            // Broadcast to peers
            for (KBucket kBucket : routingTable.getBuckets()) {
                for (Node peer : kBucket.getNodes()) {
                    try {
                        GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                        client.addAuction(block);
                        System.out.println("Forwarded new node to: " + peer);
                    } catch (Exception e) {
                        System.err.println("Broadcast to " + peer.getId() + " failed: " + e.getMessage());
                    }
                }
            }

            // Send success response
            AuctionResponse response = AuctionResponse.newBuilder()
                    .setMessage("Broadcasted Auction to all nodes")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to process broadcastAuction: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }



    @Override
    public void findNode(NodeGrpc nodeGrpc, StreamObserver<NodeGrpc> responseObserver) {
        Node node = Utils.convertNodeFromProto(nodeGrpc);
        Node responseNode = routingTable.findNodeByIpAndPort(node.getIp(), node.getPort());
        NodeGrpc response = Utils.convertNodeToProto(responseNode);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void printRoutingTable(Empty request, StreamObserver<PrintRoutingTableResponse> responseObserver) {
        PrintRoutingTableResponse response = PrintRoutingTableResponse.newBuilder()
                .setMessage(routingTable.printRoutingTableToString())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getAuctions(Empty request, StreamObserver<Kademlia.GetAuctionsResponse> responseObserver) {
        List<Auction> auctions = new ArrayList<>();
        for (Map.Entry<Integer, BlockChain> entry : routingTable.getBlockchains().entrySet()) {
            Block block = entry.getValue().getLatestBlock();
            auctions.add(block.getAuction());
        }


        Kademlia.GetAuctionsResponse.Builder responseBuilder = Kademlia.GetAuctionsResponse.newBuilder();
        for (Auction auction : auctions) {
            AuctionGrpc auctionGrpcs = Utils.convertAuctionToProto(auction);
            responseBuilder.addAuctions(auctionGrpcs.toBuilder().build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendBid(BlockGrpc blockGrpc, StreamObserver<SendBidResponse> responseObserver) {
        try {
            Block block = Utils.convertBlockFromProto(blockGrpc);
            String blockSignature = block.getSignature();
            Auction auction = block.getAuction();
            int lastIndex = auction.getBids().size()-1;
            String blockHash = auction.getBids().get(lastIndex).getSender();
            String publicKeyString = routingTable.getPublicKeyByHash(blockHash);

            PublicKey publicKey = null;
            if (publicKeyString != null && !publicKeyString.isEmpty()) {
                publicKey = Utils.decodePublicKey(publicKeyString);
            }

            // If public key is missing, request from bootstrap first
            if (publicKey == null && !routingTable.getLocalNode().getIp().equals(Utils.bootstrapIp) && routingTable.getLocalNode().getPort()!=Utils.bootstrapPort) {
                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);

                Node node = bootstrapClient.findNodeByHash(blockHash);
                if (node != null) {
                    routingTable.addNode(node);
                    publicKeyString = routingTable.getPublicKeyByHash(blockHash);
                    publicKey = Utils.decodePublicKey(publicKeyString);
                }
            }

            Block blockToVerify=block;
            blockToVerify.setSignature("");
            boolean verify=publicKey!=null?Utils.verifyBlock(blockToVerify, blockSignature, publicKey):false;

            // Only add to blockchain if verification is successful
            if (verify) {
                String result = routingTable.addBlockToBlockChain(block.getAuction().getId(),block);
                if(result.equals("")){
                    // Broadcast to peers
                    for (KBucket kBucket : routingTable.getBuckets()) {
                        for (Node peer : kBucket.getNodes()) {
                            try {
                                GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                                client.addBid(block);
                                System.out.println("Forwarded new node to: " + peer);
                            } catch (Exception e) {
                                System.err.println("Broadcast to " + peer.getId() + " failed: " + e.getMessage());
                            }
                        }
                    }

                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage("Broadcasted Bid to all nodes")
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
                else {
                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage(result)
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            } else {
                SendBidResponse response = SendBidResponse.newBuilder()
                        .setMessage("Invalid block signature or missing public key")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }


        } catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to process broadcastAuction: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }

    }


    @Override
    public void addBid(BlockGrpc blockGrpc, StreamObserver<SendBidResponse> responseObserver) {

            Block block = Utils.convertBlockFromProto(blockGrpc);


            String blockHash = block.getAuction().getSenderHash();
            String publicKeyString = routingTable.getPublicKeyByHash(blockHash);



            // If public key is missing, request from bootstrap first
        if ( !routingTable.getLocalNode().getId().equals(block.getAuction().getSenderHash()) && publicKeyString == null ||
            !routingTable.getLocalNode().getId().equals(block.getAuction().getSenderHash()) && publicKeyString.isEmpty() )
        {
                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);

                Node node = bootstrapClient.findNodeByHash(blockHash);
                if (node != null) {
                    routingTable.addNode(node);
                }
            }


            String result = routingTable.addBlockToBlockChain(block.getAuction().getId(),block);
            if(result.equals("")){
                SendBidResponse response = SendBidResponse.newBuilder()
                        .setMessage("Bid added from block "+block.getHash()+", sended by "+block.getAuction().getSenderHash())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
            else if(result.equals("Previous hash mismatch")){
                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                Block lastBlock = bootstrapClient.getLastBlockFromAuction(block.getAuction());
                if(lastBlock!=null && lastBlock.getHash().equals(block.getPreviousHash())){
                    routingTable.addBlockToBlockChain(block.getAuction().getId(),lastBlock);
                    routingTable.addBlockToBlockChain(block.getAuction().getId(),block);


                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage("Broadcasted Bid to all nodes")
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
                else{
                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage(result)
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            }
            else {
                SendBidResponse response = SendBidResponse.newBuilder()
                        .setMessage(result)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }



    }



    @Override
    public void getLastBlockFromAuction(AuctionGrpc auctionGrpc, StreamObserver<BlockGrpc> responseObserver) {
        Auction auction = Utils.convertAuctionFromProto(auctionGrpc);
        Block block = routingTable.getLastBlockFromAuction(auction.getId());
        BlockGrpc blockGrpc = Utils.convertBlockToProto(block);

        responseObserver.onNext(blockGrpc);
        responseObserver.onCompleted();
    }


    @Override
    public void getBlockChains(Empty empty, StreamObserver<BlockChainMap> responseObserver) {
        Map<Integer, BlockChain> blockchains = routingTable.getBlockchains();
        BlockChainMap blockChains = Utils.convertBlockChainMapToProto(blockchains);

        responseObserver.onNext(blockChains);
        responseObserver.onCompleted();
    }

    @Override
    public void addBlockChains(BlockChainMap blockChainMap, StreamObserver<AddBlockChainsResponse> responseObserver) {
        Map<Integer, BlockChainGrpc> protoMap = blockChainMap.getBlockChainMap();
        Map<Integer, BlockChain> blockchains = Utils.convertBlockChainMapFromProto(protoMap);
        routingTable.setBlockchains(blockchains);

        responseObserver.onNext(AddBlockChainsResponse.newBuilder().setSuccess(true).setMessage("BlockChain interno buscado do no bootstrap atualizado com sucesso").build());
        responseObserver.onCompleted();
    }

    @Override
    public void findNodeByHash(HashGrpc hashGrpc, StreamObserver<NodeGrpc> responseObserver) {
        Node node = routingTable.findNodeByHash(hashGrpc.getHash());
        NodeGrpc nodeGrpc = Utils.convertNodeToProto(node);

        responseObserver.onNext(nodeGrpc);
        responseObserver.onCompleted();
    }


    @Override
    public void addAuction(BlockGrpc blockGrpc, StreamObserver<AuctionResponse> responseObserver) {
        Block block = Utils.convertBlockFromProto(blockGrpc);

        String blockHash = block.getAuction().getSenderHash();
        String publicKeyString = routingTable.getPublicKeyByHash(blockHash);

        // If public key is missing, request from bootstrap first
        if ( !routingTable.getLocalNode().getId().equals(block.getAuction().getSenderHash()) && publicKeyString == null ||
                !routingTable.getLocalNode().getId().equals(block.getAuction().getSenderHash()) && publicKeyString.isEmpty() ) {
            GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);

            Node node = bootstrapClient.findNodeByHash(blockHash);
            if (node != null) {
                routingTable.addNode(node);
            }
        }


        BlockChain blockChain = new BlockChain();
        blockChain.addBlockToBlockChain(block);
        routingTable.addToBlockChains(block.getAuction().getId(),blockChain);



        // Send success response
        AuctionResponse response = AuctionResponse.newBuilder()
                .setMessage("Added Auction from block "+block.getHash()+" created by"+block.getAuction().getSenderHash())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

}
