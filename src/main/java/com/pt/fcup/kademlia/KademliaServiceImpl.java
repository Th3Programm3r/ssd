package com.pt.fcup.kademlia;


import com.pt.fcup.Auction.Auction;
import com.pt.fcup.Auction.Bid;
import com.pt.fcup.BlockChain.Block;
import com.pt.fcup.BlockChain.BlockChain;
import com.pt.fcup.Utils;
import com.pt.fcup.grpc.ConnectedClient;
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
import kademlia.Kademlia.BidNotification;
import kademlia.Kademlia.SubscribeRequest;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class KademliaServiceImpl  extends KademliaServiceGrpc.KademliaServiceImplBase{
    private final RoutingTable routingTable;
    private final List<StreamObserver<BidNotification>> observers = new CopyOnWriteArrayList<>();
    List<ConnectedClient> connectedClients = new CopyOnWriteArrayList<>();

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
        try {
            Node newNode = Utils.convertNodeFromProto(request);
            routingTable.addNode(newNode);

            for (KBucket kBucket : routingTable.getBuckets()) {
                if (kBucket.getNodes().isEmpty()) {
                    continue; // Skip empty buckets
                }
                for (Node peer : kBucket.getNodes()) {
                    if (!peer.getId().equals(newNode.getId())) {
                        GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                        try {
                            try {
                                client.addNodeToRoutingTable(newNode);
                                System.out.println("Novo nó enviado para o nó: " + peer.getId());
                            }
                            finally {
                                client.shutdown();
                            }
                        } catch (Exception e) {
                            client.shutdown();
                            System.err.println("Erro ao notificar o nó " + peer.getId() + ": " + e.getMessage());
                            GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                            try {
                                bootstrapClient.broadcastRemoveNode(peer);
                            }
                            finally {
                                bootstrapClient.shutdown();
                            }
                        }
                    }
                }
            }

            AddNodeResponse response = AddNodeResponse.newBuilder()
                    .setMessage("Nó adcionado: " + newNode.getId() + " e propagado")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao processar o envio do novo no: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void addNodeToRoutingTable(NodeGrpc request, StreamObserver<AddNodeResponse> responseObserver) {
        Node newNode = Utils.convertNodeFromProto(request);
        routingTable.addNode(newNode);


        AddNodeResponse response = AddNodeResponse.newBuilder()
                .setMessage("Nó: " + newNode.getId()+" adcionado na tabela de rotas")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void removeNode(NodeGrpc request, StreamObserver<RemoveNodeResponse> responseObserver) {
        Node node = Utils.convertNodeFromProto(request);
        routingTable.removeNode(node);
        RemoveNodeResponse response = RemoveNodeResponse.newBuilder()
                .setMessage("Nó removido: " + node.getId())
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
            System.out.println("Bloco recebido "+block.getHash());
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
                try{
                    Node node = bootstrapClient.findNodeByHash(blockHash);
                    if (node != null) {
                        routingTable.addNode(node);
                        publicKeyString = routingTable.getPublicKeyByHash(blockHash);
                        publicKey = Utils.decodePublicKey(publicKeyString);
                    }
                }
                finally {
                    bootstrapClient.shutdown();
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
                System.err.println("Assinatura do bloco invalido ou chave publica inexistente");
            }

            // Broadcast to peers
            for (KBucket kBucket : routingTable.getBuckets()) {
                if (kBucket.getNodes().isEmpty()) {
                    continue; // Skip empty buckets
                }
                for (Node peer : kBucket.getNodes()) {
                    GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                    try {
                        try {
                            client.addAuction(block);
                            System.out.println("Novo leilão enviado para o nó: " + peer.getId());
                        }
                        finally {
                            client.shutdown();
                        }
                    } catch (Exception e) {
                        client.shutdown();
                        System.err.println("Envio para o nó " + peer.getId() + " falhou: " + e.getMessage());
                        GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                        try {
                            bootstrapClient.broadcastRemoveNode(peer);
                        }
                        finally {
                            bootstrapClient.shutdown();
                        }
                    }
                }
            }




            notifyAllClients(block,1);

            // Send success response
            AuctionResponse response = AuctionResponse.newBuilder()
                    .setMessage("Leilão adcionado em todos os nós")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao processar o envio de leilões: " + e.getMessage())
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
                try {
                    Node node = bootstrapClient.findNodeByHash(blockHash);
                    if (node != null) {
                        routingTable.addNode(node);
                        publicKeyString = routingTable.getPublicKeyByHash(blockHash);
                        publicKey = Utils.decodePublicKey(publicKeyString);
                    }
                }
                finally {
                    bootstrapClient.shutdown();
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
                        if (kBucket.getNodes().isEmpty()) {
                            continue; // Skip empty buckets
                        }
                        for (Node peer : kBucket.getNodes()) {
                            GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                            try {
                                try {
                                    client.addBid(block);
                                    System.out.println("Novo lance enviado para o nó: " + peer.getId());
                                }
                                finally {
                                    client.shutdown();
                                }

                            } catch (Exception e) {
                                client.shutdown();
                                System.err.println("Erro ao enviar o lance para o nó " + peer.getId() + " " + e.getMessage());
                                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                                try {
                                    bootstrapClient.broadcastRemoveNode(peer);
                                }
                                finally {
                                    bootstrapClient.shutdown();
                                }

                            }
                        }
                    }

                    notifyAllClients(block,2);

                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage("Lance enviado para todos os nós")
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
                        .setMessage("Assinatura invalida no bloco ou chave publica inexistente")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }


        } catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao processar envios de lances: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }

    }


    @Override
    public void addBid(BlockGrpc blockGrpc, StreamObserver<SendBidResponse> responseObserver) {
        try {


            Block block = Utils.convertBlockFromProto(blockGrpc);


            String blockHash = block.getAuction().getSenderHash();
            String publicKeyString = routingTable.getPublicKeyByHash(blockHash);



            // If public key is missing, request from bootstrap first
        if ( !routingTable.getLocalNode().getId().equals(block.getAuction().getSenderHash()) && publicKeyString == null ||
            !routingTable.getLocalNode().getId().equals(block.getAuction().getSenderHash()) && publicKeyString.isEmpty() )
        {
                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                try {
                    Node node = bootstrapClient.findNodeByHash(blockHash);
                    if (node != null) {
                        routingTable.addNode(node);
                    }
                }
                finally {
                    bootstrapClient.shutdown();
                }
            }


            String result = routingTable.addBlockToBlockChain(block.getAuction().getId(),block);
            if(result.equals("")){
                SendBidResponse response = SendBidResponse.newBuilder()
                        .setMessage("Lance adcionado do bloco "+block.getHash()+", enviado por "+block.getAuction().getSenderHash())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
            else if(result.equals("Hash anterior errado")){
                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                Block lastBlock = bootstrapClient.getLastBlockFromAuction(block.getAuction());
                if(lastBlock!=null && lastBlock.getHash().equals(block.getPreviousHash())){
                    routingTable.addBlockToBlockChain(block.getAuction().getId(),lastBlock);
                    routingTable.addBlockToBlockChain(block.getAuction().getId(),block);


                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage("Lance enviado para todos os nós")
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
         catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao processar o envio do lance: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
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

        responseObserver.onNext(AddBlockChainsResponse.newBuilder().setSuccess(true).setMessage("BlockChain interno buscado do no bootstrap e atualizado com sucesso").build());
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
        try {
            Block block = Utils.convertBlockFromProto(blockGrpc);

            String blockHash = block.getAuction().getSenderHash();
            String publicKeyString = routingTable.getPublicKeyByHash(blockHash);

            // If public key is missing, request from bootstrap first
            if ( !routingTable.getLocalNode().getId().equals(block.getAuction().getSenderHash()) && publicKeyString == null ||
                    !routingTable.getLocalNode().getId().equals(block.getAuction().getSenderHash()) && publicKeyString.isEmpty() ) {
                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                try {
                    Node node = bootstrapClient.findNodeByHash(blockHash);
                    if (node != null) {
                        routingTable.addNode(node);
                    }
                }
                finally {
                    bootstrapClient.shutdown();
                }
            }


            BlockChain blockChain = new BlockChain();
            blockChain.addBlockToBlockChain(block);
            routingTable.addToBlockChains(block.getAuction().getId(),blockChain);



            // Send success response
            AuctionResponse response = AuctionResponse.newBuilder()
                    .setMessage("Leilão adcionado do bloco "+block.getHash()+" criado pelo nó "+block.getAuction().getSenderHash())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao processar a adição do leilão do bloco "+blockGrpc.getHash()+" no nó "+routingTable.getLocalNode().getId()+": " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }

    }

    @Override
    public void listenForBidNotifications(SubscribeRequest request, StreamObserver<BidNotification> responseObserver) {
        connectedClients.add(new ConnectedClient(request.getNodeId(), responseObserver));

        // Optional welcome
        responseObserver.onNext(BidNotification.newBuilder()
                .setMessage("👋 Subscrito no canal de notificação de atualização de lances e leilões.")
                .build());
    }


    // Notify all listeners when a new bid is received
    public void notifyAllClients(Block block,int type) {
        Auction auction = block.getAuction();
        List<Bid> bids = auction.getBids();
        Bid lastBid = bids.size()>0?bids.get(bids.size()-1):null;
        String message = "";
        //1 se for um leilao a ser criado
        //2 se for um novo lance
        //3 se for o fim do leilao
        if(type == 1)
            message = "📢 Novo leilão " + auction.getId()+ " criado por " + auction.getSenderHash()+" gerado pelo bloco "+block.getHash();
        else if(type == 2)
            message = "📢 Novo lance para o leilão " + auction.getId()+ ": com o valor de " + lastBid.getBidValue() + " feito por " + lastBid.getSender()+" gerado pelo bloco "+block.getHash();
        else
            message = "📢 Leilão " + auction.getId()+ " terminado ";


        BidNotification notification = BidNotification.newBuilder()
                .setMessage(message)
                .build();



        for (ConnectedClient client : connectedClients) {
            if(lastBid==null){
                if (client.nodeId.equals(block.getAuction().getSenderHash()))
                    continue;
            }
            else {
                if (lastBid!=null && client.nodeId.equals(lastBid.getSender()))
                    continue;
                if(!client.nodeId.equals(block.getAuction().getSenderHash())
                        && !block.getAuction().getParticipants().contains(client.nodeId)
                       // && !block.getAuction().getBids().stream().anyMatch(bid -> bid.getSender().equals(client.nodeId))
                )
                    continue;
            }


            try {
                client.observer.onNext(notification);
            } catch (Exception e) {
                connectedClients.remove(client);
            }
        }
    }

    @Override
    public void endAuction(BlockGrpc blockGrpc, StreamObserver<SendBidResponse> responseObserver) {
        try {
            Block block = Utils.convertBlockFromProto(blockGrpc);
            String blockSignature = block.getSignature();
            Auction auction = block.getAuction();
            int lastIndex = auction.getBids().size()-1;
            String blockHash = "";
            if(auction.getBids().size()>0){
                blockHash = auction.getBids().get(lastIndex).getSender();
            }
            else{
                blockHash = auction.getSenderHash();
            }
            String publicKeyString = routingTable.getPublicKeyByHash(blockHash);

            PublicKey publicKey = null;
            if (publicKeyString != null && !publicKeyString.isEmpty()) {
                publicKey = Utils.decodePublicKey(publicKeyString);
            }

            // If public key is missing, request from bootstrap first
            if (publicKey == null && !routingTable.getLocalNode().getIp().equals(Utils.bootstrapIp) && routingTable.getLocalNode().getPort()!=Utils.bootstrapPort) {
                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);

                try {
                    Node node = bootstrapClient.findNodeByHash(blockHash);
                    if (node != null) {
                        routingTable.addNode(node);
                        publicKeyString = routingTable.getPublicKeyByHash(blockHash);
                        publicKey = Utils.decodePublicKey(publicKeyString);
                    }
                }
                finally {
                    bootstrapClient.shutdown();
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
                        if (kBucket.getNodes().isEmpty()) {
                            continue; // Skip empty buckets
                        }
                        for (Node peer : kBucket.getNodes()) {
                            try {
                                GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                                try {
                                    client.addBid(block);
                                    System.out.println("Envio de fim de leilão enviado ao nó: " + peer.getId());
                                }
                                finally {
                                    client.shutdown();
                                }

                            } catch (Exception e) {
                                System.err.println("Envio de fim de leilão falhou para o nó " + peer.getId() + ": " + e.getMessage());
                                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                                try {
                                    bootstrapClient.broadcastRemoveNode(peer);
                                }
                                finally {
                                    bootstrapClient.shutdown();
                                }
                            }
                        }
                    }

                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage("Bloco "+block.getHash()+" enviado para todos os nos e leilao "+block.getAuction().getId()+" terminado")
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                    notifyAllClients(block,3);
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
                        .setMessage("Assinatura do bloco invalido ou chave publica inexistente")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }


        } catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao processar o fim do leilão "+blockGrpc.getAuction().getId()+": " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }

    }


    @Override
    public void broadcastRemoveNode(NodeGrpc request, StreamObserver<RemoveNodeResponse> responseObserver) {
        Node node = Utils.convertNodeFromProto(request);
        routingTable.removeNode(node);
        for (KBucket kBucket : routingTable.getBuckets()) {
            if (kBucket.getNodes().isEmpty()) {
                continue; // Skip empty buckets
            }
            for (Node peer : kBucket.getNodes()) {
                try {
                    if(!peer.getId().equals(node.getId())){
                        GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                        try {
                            client.removeNode(node);
                            System.out.println("Nó removido da tabela de rotas do nó: " + peer.getId());
                        }
                        finally {
                            client.shutdown();
                        }

                    }
                } catch (Exception e) {
                    System.err.println("Remoção no nó " + peer.getId() + " falhou: " + e.getMessage());
                }
            }
        }

        notifyAllClientsNodeRemoved(node);

        RemoveNodeResponse response = RemoveNodeResponse.newBuilder()
                .setMessage("Nó "+node.getId()+" removido do bootstrap e dos restantes nós participantes da rede")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void notifyAllClientsNodeRemoved(Node node) {


        String message = "📢 Nó " + node.getId()+ " removido do no bootstrap e de todos os nós participantes no kadelmia por falhar o ping";


        BidNotification notification = BidNotification.newBuilder()
                .setMessage(message)
                .build();



        for (ConnectedClient client : connectedClients) {
            try {
                client.observer.onNext(notification);
            } catch (Exception e) {
                connectedClients.remove(client);
            }
        }
    }



    @Override
    public void endAuctionFromScript(BlockGrpc blockGrpc, StreamObserver<SendBidResponse> responseObserver) {
        try {
            Block block = Utils.convertBlockFromProto(blockGrpc);

            Block lastBlock = routingTable.getLastBlockFromAuction(block.getAuction().getId());
            if(lastBlock.getAuction().isActive()){
                String result = routingTable.addBlockToBlockChain(block.getAuction().getId(),block);
                if(result.equals("")){
                    // Broadcast to peers
                    for (KBucket kBucket : routingTable.getBuckets()) {
                        if (kBucket.getNodes().isEmpty()) {
                            continue; // Skip empty buckets
                        }
                        for (Node peer : kBucket.getNodes()) {
                            try {
                                GrpcClient client = new GrpcClient(peer.getIp(), peer.getPort());
                                try {
                                    client.addBid(block);
                                    System.out.println("Fim do leilão enviado para o nó: " + peer.getId());
                                }
                                finally {
                                    client.shutdown();
                                }

                            } catch (Exception e) {
                                System.err.println("Erro ao enviar o fim de leilão para o nó " + peer.getId() + ": " + e.getMessage());
                                GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
                                try {
                                    bootstrapClient.broadcastRemoveNode(peer);
                                }
                                finally {
                                    bootstrapClient.shutdown();
                                }
                            }
                        }
                    }

                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage("Fim do envio do fim de leilão para todos os nós")
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                    notifyAllClients(block,3);
                }
                else {
                    SendBidResponse response = SendBidResponse.newBuilder()
                            .setMessage(result)
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }

            }




        } catch (Exception e) {
            // Proper gRPC error handling
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao processar o fim do leilão: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }

    }

}
