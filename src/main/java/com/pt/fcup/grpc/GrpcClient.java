package com.pt.fcup.grpc;


import com.pt.fcup.Auction.Auction;
import com.pt.fcup.Auction.Bid;
import com.pt.fcup.Auction.Product;
import com.pt.fcup.BlockChain.Block;
import com.pt.fcup.BlockChain.BlockChain;
import com.pt.fcup.Utils;
import com.pt.fcup.kademlia.Node;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import kademlia.Kademlia;
import kademlia.KademliaServiceGrpc;
import kademlia.Kademlia.PingRequest;
import kademlia.Kademlia.PingResponse;
import kademlia.Kademlia.AddNodeResponse;
import kademlia.Kademlia.FindRequest;
import kademlia.Kademlia.NodeGrpc;
import kademlia.Kademlia.AuctionGrpc;
import kademlia.Kademlia.BidGrpc;
import kademlia.Kademlia.AuctionResponse;
import kademlia.Kademlia.PrintRoutingTableResponse;
import kademlia.Kademlia.SendBidResponse;
import kademlia.Kademlia.Empty;
import kademlia.Kademlia.GetAuctionsResponse;
import kademlia.Kademlia.BlockChainMap;
import kademlia.Kademlia.AddBlockChainsResponse;
import kademlia.Kademlia.BlockGrpc;
import kademlia.Kademlia.HashGrpc;
import kademlia.Kademlia.SubscribeRequest;
import kademlia.Kademlia.BidNotification;

import java.io.IOException;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


public class GrpcClient {
    private final ManagedChannel channel;
    private final KademliaServiceGrpc.KademliaServiceBlockingStub stub;
    private final KademliaServiceGrpc.KademliaServiceStub asyncStub;
    public static PrivateKey privateKey;
    public static PublicKey publicKey;

    public GrpcClient(String targetIp, int targetPort) {
        this.channel = ManagedChannelBuilder.forAddress(targetIp, targetPort)
                .usePlaintext()
                .build();
        this.stub = KademliaServiceGrpc.newBlockingStub(channel);
        this.asyncStub =  KademliaServiceGrpc.newStub(channel);
    }

    public void ping(String myId) {
        PingRequest request = PingRequest.newBuilder().setId(myId).build();
        PingResponse response = stub.ping(request);
        System.out.println("Ping: " + response.getMessage());
    }

    public void sendNode(Node node) {
        NodeGrpc protoNode = Utils.convertNodeToProto(node);
        AddNodeResponse response = stub.addNode(protoNode);
        System.out.println("Add Node: " + response.getMessage());
    }

    public void addNodeToRoutingTable(Node node) {
        NodeGrpc protoNode = Utils.convertNodeToProto(node);
        AddNodeResponse response = stub.addNodeToRoutingTable(protoNode);
        System.out.println(response.getMessage());
    }

    public NodeGrpc findClosest(String targetId) {
        FindRequest request = FindRequest.newBuilder().setTargetId(targetId).build();
        return stub.findClosestNode(request);
    }

    public Node getTargetNode() {
        NodeGrpc protoNode = stub.getSelfNode(Empty.newBuilder().build());
        return new Node(protoNode.getId(), protoNode.getIp(), protoNode.getPort());
    }

    public void sendAuction(Block block){
        BlockGrpc convertedBlock = Utils.convertBlockToProto(block);
        AuctionResponse response = stub.broadcastAuction(convertedBlock);
        System.out.println(response.getMessage());
    }

    public void printRoutingTable() {
        System.out.println("Calling printRoutingTable()...");
        PrintRoutingTableResponse response = stub.printRoutingTable(Empty.newBuilder().build());
        System.out.println("TABLE: " + response.getMessage());
    }

    public List<Auction> getAuctions() {
        GetAuctionsResponse response = stub.getAuctions(Empty.newBuilder().build());
        List<Auction> auctions = new ArrayList<Auction>();
        //System.out.println("Auctions:");
        for (AuctionGrpc auction : response.getAuctionsList()) {
            auctions.add(Utils.convertAuctionFromProto(auction));
//            System.out.println("Auction ID: " + auction.getId());
//            for (ProductGrpc product : auction.getProductsList()) {
//                System.out.println("  Product: " + product.getName() + ", Initial: " + product.getInitialPrice() + ", Final: " + product.getFinalPrice());
//            }
//            for (BidGrpc bid : auction.getBidsList()) {
//                System.out.println("  Bid ID: " + bid.getId() + ", Product ID: " + bid.getProductId() + ", Bidder ID: " + bid.getBidId());
//            }
        }
        return auctions;
    }

    public void sendBid(Block block){
        BlockGrpc blockGrpc = Utils.convertBlockToProto(block);
        SendBidResponse response = stub.sendBid(blockGrpc);
        System.out.println(response.getMessage());
    }

    public void endAuction(Block block){
        BlockGrpc blockGrpc = Utils.convertBlockToProto(block);
        SendBidResponse response = stub.endAuction(blockGrpc);
        System.out.println(response.getMessage());
    }

    public void addBid(Block block){
        BlockGrpc blockGrpc = Utils.convertBlockToProto(block);
        SendBidResponse response = stub.addBid(blockGrpc);
        System.out.println(response.getMessage());
    }

//    public String getBlock(Bid bid){
//        BidGrpc bidGrpc = Utils.convertBidToProto(bid);
//        SendBidResponse response = stub.sendBid(bidGrpc);
//        System.out.println(response.getMessage());
//        return "";
//    }

    public BlockChainMap getBlockChains(){
        BlockChainMap response = stub.getBlockChains(Empty.newBuilder().build());
        return response;
    }

    public void setBlockChains(BlockChainMap blockChainMap){
        AddBlockChainsResponse response = stub.addBlockChains(blockChainMap);
        System.out.println(response.getMessage());
    }

    public Node findNodeByHash(String hash){
        HashGrpc hashGrpc = HashGrpc.newBuilder().setHash(hash).build();
        NodeGrpc response = stub.findNodeByHash(hashGrpc);
        Node node = Utils.convertNodeFromProto(response);
        return node;
    }

    public Block getLastBlockFromAuction(Auction auction){
        AuctionGrpc auctionGrpc = Utils.convertAuctionToProto(auction);
        BlockGrpc response = stub.getLastBlockFromAuction(auctionGrpc);
        Block block = Utils.convertBlockFromProto(response);
        return block;
    }

    public void addAuction(Block block) {
        try {
            BlockGrpc convertedBlock = Utils.convertBlockToProto(block);
            AuctionResponse response = stub.addAuction(convertedBlock);  // This may throw an exception
            System.out.println("Response from server: " + response.getMessage());
        } catch (Exception e) {
            System.err.println("gRPC call failed: " + e.getMessage());
            e.printStackTrace();  // This will give us the real root cause
        }
    }


    public void listenForBidNotifications(String nodeId) {
        SubscribeRequest request = SubscribeRequest.newBuilder().setNodeId(nodeId).build();

        asyncStub.listenForBidNotifications(request, new StreamObserver<BidNotification>() {
            @Override
            public void onNext(BidNotification notification) {
                System.out.println(notification.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Disconnected from notifications: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Notification stream closed");
            }
        });
    }


    //Quando um no é iniciado ele pede da rede o estado atual da blockchain****
    //Adcionar o auction o objeto bid, retirar do objeto bid o auction porque ja nao vai ser preciso****
    //Adcionar um auction e adcionar a blochain e propagar pela rede a blockChain e nao a auction, remover lista de auctions da blockchain****
    //Quando é feito um novo lance ele adciona a rede blockchain o objecto auction e nele tem o bid****
    //o ultimo bloco vai ter o bid mais recente****
    //Mas se ele receber um bid o qual não tenha um hashAnterior ele pede ao no principal para poder mostrar os dados****
    //Fazer um Challenge, verificar se o hash esta correto, e verificar assinatura****
    //Testar o envio de lances************
    //Verificar o erro que acontece quando se liga a rede depois de ja ter sido criado algum leilao******************
    //Criar funcionalidade para listar totas as blockchains **********
    //Quando o adcionar bid funcionar atualizar o codigo para o adcionar bid para os leiloes que participas************
    //Ver se tem como reduzir o tamanho da routing table**********
    //Mostrar quando se visualiza o leilao, qual blockchain ele pertence, qual bloco gerou o leilao e quem fez o leilao ou bid******************

    //Criar um script para correr a cada x minutos para terminar o leilao caso aquele leilao ja tenha acabado o tempo
    //Criar funcionalidade para terminar um leilao que tenha o id X para testar o que acontece quando um leilao acaba
    //Ver como fazer para receber notificação cada vez que se envia um novo lance a um leilão a que o utilizador pertence ou quando o leilao termina
    //Ver se é preciso apagar um no quando não se consegue chegar a ele quando se adciona um novo no, um novo leilao e um novo lance

    //TESTES
    //Ver se ao criar um leilao com um utilizador em que um outro for registado apos se ele busca este no no no bootstrap
    //Ver se ao criar um leilao com uma assinatura falsa o que acontece


    public static void main(String[] args) throws Exception {
        // Generate RSA key pair
        KeyPair keyPair = Utils.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        Scanner input = new Scanner(System.in);
        // 1. Define this node’s IP and port
        String localIp = Utils.getLocalIp();
        int localPort = Utils.getFreePort();
        String encondedPulicKey=Utils.encodePublicKey(publicKey);
        Node localNode = new Node(localIp, localPort, encondedPulicKey);

        // 2. Start gRPC server for this node
        GrpcServer server = new GrpcServer(localIp, localPort);
        server.start(); // async
        System.out.printf("Node started: %s:%d (ID: %s)%n", localIp, localPort, localNode.getId());

        // 3. Bootstrap contact
        GrpcClient bootstrapClient = new GrpcClient(Utils.bootstrapIp, Utils.bootstrapPort);
        if (localPort != Utils.bootstrapPort) {
            bootstrapClient.sendNode(localNode);
            bootstrapClient.listenForBidNotifications(localNode.getId());
        }

        GrpcClient selfClient = new GrpcClient(localIp,localPort);
        BlockChainMap blockChainMap = bootstrapClient.getBlockChains();
        selfClient.setBlockChains(blockChainMap);


        while (true) {
            Thread.sleep(1000);
            System.out.println("Selecione a opção pretendida ou 0 para terminar");
            System.out.println("1-Adcionar um produto");
            System.out.println("2-Criar um leilão");
            System.out.println("3-Visualizar leilões");
            System.out.println("4-Visualizar o routing table");
            System.out.println("5-Visualizar a blocking chain");
            System.out.println("6-Terminar leilão");
            int option = input.nextInt();
            if(option==1){
                input.nextLine();
                System.out.println("Introduza o nome do produto");
                String name = input.nextLine();
                Product product = new Product(name);
                //ADD Product to products json
                List<Product> productsSaved=Utils.loadProductFromJsonFile("Products.json");
                if(productsSaved!=null)
                    productsSaved.add(product);
                else {
                    productsSaved=new ArrayList<>();
                    productsSaved.add(product);
                }

                Utils.saveProductToJsonFile("Products.json",productsSaved);
            }
            else if(option==2){
                System.out.println("Introduza a duração do leilao em horas");
                int tempo = input.nextInt();
                System.out.println("Selecione os produtos que queres dar lance");
                //Read JSON AND LIST ALL PRODUCTS

                List<Product> products = Utils.loadProductFromJsonFile("Products.json");
                List<Product> auctionProducts = new ArrayList<>();
                int i=1;
                for (Product product:products){
                    System.out.println(i + "-" + product.getName());
                    i++;
                }

                while(true){
                    System.out.println("Selecione o produto a adcionar no leilao ou 0 para terminar");
                    System.out.println("Produto:");
                    int choice=input.nextInt();
                    if(choice==0)
                        break;
                    else if(choice>products.size() || choice<0){
                        System.out.println("Opção não encontrada");
                        break;
                    }
                    System.out.println("Introduza o preço inicial do produto");
                    Float intialPrice = input.nextFloat();
                    Product product = products.get(choice-1);
                    product.setInitialPrice(intialPrice);
                    auctionProducts.add(product);
                }
                //Fazer um sendAuction para o no bootstrap
                Instant currentTimestamp = Instant.now();
                Auction auction = new Auction(auctionProducts,currentTimestamp,tempo,localNode.getId());
                Block block = new Block(0,currentTimestamp.toEpochMilli(),auction,"0");
                block.mineBlock(Utils.difficulty);
                String signature = Utils.signBlock(block,privateKey);
                block.setSignature(signature);
                bootstrapClient.sendAuction(block);

            }
            else if(option==3){
                System.out.println("Selecione a opção pretendida");
                System.out.println("1-Ver todos os leiloes");
                System.out.println("2-Ver todos os leiloes em que participas");
                int choice=input.nextInt();
                if(choice==1){
                    List<Auction> auctions=selfClient.getAuctions();
                    for(Auction auction:auctions){
                        System.out.println("Leilão "+auction.getId()+" criado por "+auction.getSenderHash()+" ,estado:"+(auction.isActive()?"ativo":"inativo"));
                        for(Product product:auction.getProducts()){
                            System.out.println("Produto:" +product.getName());
                        }
                    }
                    if(auctions.size()>0) {
                        System.out.println("Selecione o Leilao que pretende dar lance ou 0 para sair");
                        while (true) {
                            int choice2 = input.nextInt();
                            if (choice2 == 0)
                                break;
                            else if(!auctions.stream().anyMatch(auction -> auction.getId()==choice2)){
                                System.out.println("Opção não encontrada");
                                break;
                            }

                            Auction selectedAuction = auctions.stream().filter(auction -> auction.getId()==choice2).findFirst().get();
                            if(!selectedAuction.isActive()){
                                System.out.println("Leilão selecionado encontra-se inativo");
                                break;
                            }
                            List<Bid> auctionBids = selectedAuction.getBids();
                            for (Product product : selectedAuction.getProducts()) {
                                Block lastBlock = selfClient.getLastBlockFromAuction(selectedAuction);
                                Bid lastBid=null;
                                Optional<Bid> lastBidOptional = auctionBids.stream()
                                        .filter(bid -> bid.getProductId() == product.getId())
                                        .reduce((first, second) -> second); // keeps only the last element

                                if (lastBidOptional.isPresent()) {
                                    lastBid = lastBidOptional.get();
                                    System.out.println("Produto:" + product.getName() + ", lance atual " + lastBid.getBidValue());
                                    System.out.println("Minado do bloco "+lastBlock.getHash()+",criado por "+lastBid.getSender());
                                } else {
                                    System.out.println("Produto:" + product.getName() + ", lance atual " + product.getInitialPrice());
                                    System.out.println("Minado do bloco "+lastBlock.getHash()+",criado por "+selectedAuction.getSenderHash());
                                }

                                System.out.println("Introduza o valor do teu lance para o produto ou 0 caso não esteja interessado");
                                Float valor = input.nextFloat();
                                if (valor>0f && lastBid!=null && valor > lastBid.getBidValue() || valor>0f && valor > product.getInitialPrice()) {
                                    Bid newBid = new Bid((int) (Instant.now().getEpochSecond()), product.getId(), valor,localNode.getId(),selectedAuction.getId());
                                    //SEND BID USING GRPC
                                    Instant currentTimestamp = Instant.now();

                                    Auction updatedAuction = lastBlock.getAuction();
                                    updatedAuction.addBid(newBid);

                                    Block block = new Block(lastBlock.getIndex()+1,currentTimestamp.toEpochMilli(),updatedAuction,lastBlock.getHash());
                                    block.mineBlock(Utils.difficulty);
                                    String signature = Utils.signBlock(block,privateKey);
                                    block.setSignature(signature);

                                    bootstrapClient.sendBid(block);
                                } else {
                                    System.out.println("Valor do lance tem de ser maior do que o lance atual");
                                }
                            }

                            break;
                        }
                    }
                }
                else if(choice==2){
                    List<Auction> allAuctions=selfClient.getAuctions();
                    List<Auction> auctions=
                            allAuctions.stream().filter(auction -> auction.getSenderHash().equals(localNode.getId()) ||
                                    auction.getBids().stream().anyMatch(bid -> localNode.getId().equals(bid.getSender()))
                            ).collect(Collectors.toList());

                    for(Auction auction:auctions){
                        System.out.println("Leilão "+auction.getId()+" criado por "+auction.getSenderHash()+" ,estado:"+(auction.isActive()?"ativo":"inativo"));
                        for(Product product:auction.getProducts()){
                            System.out.println("Produto:" +product.getName());
                        }
                    }
                    if(auctions.size()>0) {
                        System.out.println("Selecione o Leilao que pretende dar lance ou 0 para sair");
                        while (true) {
                            int choice2 = input.nextInt();
                            if (choice2 == 0)
                                break;
                            else if(!auctions.stream().anyMatch(auction -> auction.getId()==choice2)){
                                System.out.println("Opção não encontrada");
                                break;
                            }

                            Auction selectedAuction = auctions.stream().filter(auction -> auction.getId()==choice2).findFirst().get();
                            if(!selectedAuction.isActive()){
                                System.out.println("Leilão selecionado encontra-se inativo");
                                break;
                            }
                            List<Bid> auctionBids = selectedAuction.getBids();

                            for (Product product : selectedAuction.getProducts()) {
                                Block lastBlock = selfClient.getLastBlockFromAuction(selectedAuction);
                                Bid lastBid=null;
                                Optional<Bid> lastBidOptional = auctionBids.stream()
                                        .filter(bid -> bid.getProductId() == product.getId())
                                        .reduce((first, second) -> second); // keeps only the last element

                                if (lastBidOptional.isPresent()) {
                                    lastBid = lastBidOptional.get();
                                    System.out.println("Produto:" + product.getName() + ", lance atual " + lastBid.getBidValue());
                                    System.out.println("Minado do bloco "+lastBlock.getHash()+",criado por "+lastBid.getSender());
                                } else {
                                    System.out.println("Produto:" + product.getName() + ", lance atual " + product.getInitialPrice());
                                    System.out.println("Minado do bloco "+lastBlock.getHash()+",criado por "+selectedAuction.getSenderHash());
                                }

                                System.out.println("Introduza o valor do teu lance para o produto ou 0 caso não esteja interessado");
                                Float valor = input.nextFloat();
                                if (valor>0f && lastBid!=null && valor > lastBid.getBidValue() || valor>0f && valor > product.getInitialPrice()) {
                                    Bid newBid = new Bid((int) (Instant.now().getEpochSecond()), product.getId(), valor,localNode.getId(),selectedAuction.getId());
                                    //SEND BID USING GRPC
                                    Instant currentTimestamp = Instant.now();

                                    Auction updatedAuction = lastBlock.getAuction();
                                    updatedAuction.addBid(newBid);

                                    Block block = new Block(lastBlock.getIndex()+1,currentTimestamp.toEpochMilli(),updatedAuction,lastBlock.getHash());
                                    block.mineBlock(Utils.difficulty);
                                    String signature = Utils.signBlock(block,privateKey);
                                    block.setSignature(signature);

                                    bootstrapClient.sendBid(block);

                                } else {
                                    System.out.println("Valor do lance tem de ser maior do que o lance atual");
                                }
                            }

                            break;
                        }
                    }
                }
            }
            else if(option==4){
                selfClient.printRoutingTable();
            }
            else if(option==5){
                BlockChainMap currentBlockChainMap = bootstrapClient.getBlockChains();
                Map<Integer,BlockChain> blockChains = Utils.convertBlockChainMapFromProto(currentBlockChainMap.getBlockChainMap());
                for (Map.Entry<Integer, BlockChain> entry : blockChains.entrySet()) {
                    System.out.println("--------Auction "+entry.getKey()+"--------");
                    for(Block block : entry.getValue().getChain()){
                        if(block.getAuction()!=null) {
                            String generatedBy = block.getAuction().getBids().size() > 0 ? block.getAuction().getBids().get(block.getAuction().getBids().size() - 1).getSender() : block.getAuction().getSenderHash();
                            System.out.println("Block:" + block.getHash() + ",generated by" + generatedBy);
                        }
                        else
                            System.out.println("Genesis Block:" + block.getHash());
                    }
                }
            }
            else if(option == 6){
                List<Auction> auctions=selfClient.getAuctions();

                for(Auction auction:auctions){
                    System.out.println("Leilão "+auction.getId()+" criado por "+auction.getSenderHash()+" ,estado:"+(auction.isActive()?"ativo":"inativo"));
                }
                if(auctions.size()>0) {
                    System.out.println("Selecione o Leilao que pretende terminar ou 0 para sair");
                    while (true) {
                        int choice2 = input.nextInt();
                        if (choice2 == 0)
                            break;
                        else if(!auctions.stream().anyMatch(auction -> auction.getId()==choice2)){
                            System.out.println("Opção não encontrada");
                            break;
                        }

                        Auction selectedAuction = auctions.stream().filter(auction -> auction.getId()==choice2).findFirst().get();
                        if(!selectedAuction.isActive()){
                            System.out.println("Leilão selecionado encontra-se inativo");
                            break;
                        }
                        Block lastBlock = selfClient.getLastBlockFromAuction(selectedAuction);
                        Auction updatedAuction = lastBlock.getAuction();
                        updatedAuction.setActive(false);

                        Instant currentTimestamp = Instant.now();
                        Block block = new Block(lastBlock.getIndex()+1,currentTimestamp.toEpochMilli(),updatedAuction,lastBlock.getHash());
                        block.mineBlock(Utils.difficulty);
                        String signature = Utils.signBlock(block,privateKey);
                        block.setSignature(signature);

                        bootstrapClient.endAuction(block);


                        break;
                    }
                }
            }
            else{
                System.out.println("Opção não encontrada");
                break;
            }
        }





        // 4. Keep server running
        server.blockUntilShutdown();



    }

}
