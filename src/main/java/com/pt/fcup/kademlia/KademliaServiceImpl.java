package com.pt.fcup.kademlia;


import com.pt.fcup.Auction.Bid;
import com.pt.fcup.BlockChain.BlockChain;
import com.pt.fcup.Utils;
import com.pt.fcup.grpc.GrpcClient;
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
                        client.sendNode(newNode);
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
    public void broadcastAuction(AuctionGrpc auction, StreamObserver<AuctionResponse> responseObserver) {
//        Auction newAuction = Utils.convertAuctionFromProto(auction);
//        boolean exists = routingTable.getAuctions().stream()
//                .anyMatch(a -> a.getId() == newAuction.getId());
//
//        if (!exists) {
//            routingTable.addAuction(newAuction);
//        }
//        if(routingTable.getLocalNode().getId().equals(newAuction.getSenderHash())){
//            routingTable.addParticipatingAuction(newAuction);
//        }
//
//        for(KBucket kBucket: routingTable.getBuckets()) {
//            for (Node peer : kBucket.getNodes()) {
//                try {
//                    GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
//                    client.sendAuction(newAuction);
//                    System.out.println("Forwarded new node to: " + peer);
//                } catch (Exception e) {
//                    System.err.println("Broadcast to " + peer.getId() + " failed: " + e.getMessage());
//                }
//            }
//        }

        AuctionResponse response = AuctionResponse.newBuilder()
                .setMessage("Broadcasted Auction to all nodes")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
//        List<Auction> auctions = routingTable.getAuctions(); // Your local auction list
        Kademlia.GetAuctionsResponse.Builder responseBuilder = Kademlia.GetAuctionsResponse.newBuilder();
//        for (Auction auction : auctions) {
//            AuctionGrpc auctionGrpcs = Utils.convertAuctionToProto(auction);
//            responseBuilder.addAuctions(auctionGrpcs.toBuilder().build());
//        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendBid(BidGrpc bidGrpc, StreamObserver<SendBidResponse> responseObserver) {
        Bid newBid = Utils.convertBidFromProto(bidGrpc);
//        routingTable.addBidToAuction(newBid);

        SendBidResponse response = SendBidResponse.newBuilder()
                .setMessage("Broadcasted Bid to all nodes")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastBlock(BlockGrpc request, StreamObserver<BlockAck> responseObserver) {
        // Convert from proto Block to local Block class
//        Bid bid = Utils.convertBidFromProto(request.getBid());
//        Block block = new Block(
//                request.getIndex(),
//                request.getTimestamp(),
//                bid,
//                request.getPreviousHash()
//        );
//        Auction auction = routingTable.getAuctionById(bid.getAuctionId());
//        Blockchain blockchain = auction.getBlockchain();
//        if (blockchain.validateAndAdd(block)) {
//            responseObserver.onNext(BlockAck.newBuilder().setSuccess(true).build());
//        } else {
//            responseObserver.onNext(BlockAck.newBuilder().setSuccess(false).build());
//        }

        responseObserver.onNext(BlockAck.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLastBlockHashFromAuction(BidGrpc bidGrpc, StreamObserver<BlockHash> responseObserver) {
//        Bid bid = Utils.convertBidFromProto(bidGrpc);
//        Auction auction = routingTable.getAuctionById(bid.getAuctionId());
//        Blockchain blockchain = auction.getBlockchain();
//        Block block = blockchain.getLatestBlock();
//
//        responseObserver.onNext(BlockHash.newBuilder().setBlockHash(block.getHash()).build());
        responseObserver.onNext(BlockHash.newBuilder().setBlockHash("").build());
        responseObserver.onCompleted();
    }


    @Override
    public void getBlockChains(Empty empty, StreamObserver<BlockChainMap> responseObserver) {
        Map<String, BlockChain> blockchains = routingTable.getBlockchains();
        BlockChainMap blockChains = Utils.convertBlockChainMapToProto(blockchains);

        responseObserver.onNext(blockChains);
        responseObserver.onCompleted();
    }

    @Override
    public void addBlockChains(BlockChainMap blockChainMap, StreamObserver<AddBlockChainsResponse> responseObserver) {
        Map<String, BlockChainGrpc> protoMap = blockChainMap.getBlockChainMap();
        Map<String, BlockChain> blockchains = Utils.convertBlockChainMapFromProto(protoMap);
        routingTable.setBlockchains(blockchains);

        responseObserver.onNext(AddBlockChainsResponse.newBuilder().setSuccess(true).setMessage("BlockChain interno buscado do no bootstrap atualizado com sucesso").build());
        responseObserver.onCompleted();
    }


}
