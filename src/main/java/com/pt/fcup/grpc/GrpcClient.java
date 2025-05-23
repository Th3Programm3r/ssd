package com.pt.fcup.grpc;


import com.pt.fcup.Auction.Auction;
import com.pt.fcup.Auction.Bid;
import com.pt.fcup.Auction.Product;
import com.pt.fcup.Utils;
import com.pt.fcup.kademlia.Node;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;


public class GrpcClient {
    private final ManagedChannel channel;
    private final KademliaServiceGrpc.KademliaServiceBlockingStub stub;

    public GrpcClient(String targetIp, int targetPort) {
        this.channel = ManagedChannelBuilder.forAddress(targetIp, targetPort)
                .usePlaintext()
                .build();
        this.stub = KademliaServiceGrpc.newBlockingStub(channel);
    }

    public void ping(String myId) {
        Kademlia.PingRequest request = PingRequest.newBuilder().setId(myId).build();
        PingResponse response = stub.ping(request);
        System.out.println("Ping: " + response.getMessage());
    }

    public void sendNode(Node node) {
        NodeGrpc protoNode = NodeGrpc.newBuilder()
                .setId(node.getId())
                .setIp(node.getIp())
                .setPort(node.getPort())
                .build();
        AddNodeResponse response = stub.addNode(protoNode);
        System.out.println("Add Node: " + response.getMessage());
    }

    public NodeGrpc findClosest(String targetId) {
        FindRequest request = FindRequest.newBuilder().setTargetId(targetId).build();
        return stub.findClosestNode(request);
    }

    public Node getTargetNode() {
        NodeGrpc protoNode = stub.getSelfNode(Kademlia.Empty.newBuilder().build());
        return new Node(protoNode.getId(), protoNode.getIp(), protoNode.getPort());
    }

    public void sendAuction(Auction auction){
        AuctionGrpc convertedAuction = Utils.convertAuctionToProto(auction);
        AuctionResponse response = stub.broadcastAuction(convertedAuction);
        System.out.println(response.getMessage());
    }

    public void printRoutingTable() {
        System.out.println("Calling printRoutingTable()...");
        PrintRoutingTableResponse response = stub.printRoutingTable(Kademlia.Empty.newBuilder().build());
        System.out.println("TABLE: " + response.getMessage());
    }

    public List<Auction> getAuctions() {
        Kademlia.GetAuctionsResponse response = stub.getAuctions(Kademlia.Empty.newBuilder().build());
        List<Auction> auctions = new ArrayList<Auction>();
        //System.out.println("Auctions:");
        for (Kademlia.AuctionGrpc auction : response.getAuctionsList()) {
            auctions.add(Utils.convertAuctionFromProto(auction));
//            System.out.println("Auction ID: " + auction.getId());
//            for (Kademlia.ProductGrpc product : auction.getProductsList()) {
//                System.out.println("  Product: " + product.getName() + ", Initial: " + product.getInitialPrice() + ", Final: " + product.getFinalPrice());
//            }
//            for (Kademlia.BidGrpc bid : auction.getBidsList()) {
//                System.out.println("  Bid ID: " + bid.getId() + ", Product ID: " + bid.getProductId() + ", Bidder ID: " + bid.getBidId());
//            }
        }
        return auctions;
    }

    public void sendBid(Bid bid){
        BidGrpc bidGrpc = Utils.convertBidToProto(bid);
        SendBidResponse response = stub.sendBid(bidGrpc);
        System.out.println(response.getMessage());
    }

    public String getBlock(Bid bid){
        BidGrpc bidGrpc = Utils.convertBidToProto(bid);
        SendBidResponse response = stub.sendBid(bidGrpc);
        System.out.println(response.getMessage());
        return "";
    }

    //Adcionar o auction o objeto bid, retirar do objeto bid o auction porque ja nao vai ser preciso
    //Fazer um Challenge, verificar se o hash esta correto, e verificar assinatura
    //Adcionar um auction e adcionar a blochain e propagar pela rede a blockChain e nao a auction, remover lista de auctions da blockchain
    //Quando é feito um novo lance ele adciona a rede blockchain o objecto auction e nele tem o bid
    //o ultimo bloco vai ter o bid mais recente
    //Quando um no é iniciado depois de ja ter sido feito um bid nao fazer nada
    //Mas se ele receber um bid o qual não tenha um hashAnterior ele pede ao no principal para poder mostrar os dados
    //Quando um no é iniciado ele pede da rede o estado atual da blockchain

    public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException {
        Scanner input = new Scanner(System.in);
        // 1. Define this node’s IP and port
        String localIp = Utils.getLocalIp();
        int localPort = Utils.getFreePort();
        Node localNode = new Node(localIp, localPort);

        // 2. Start gRPC server for this node
        GrpcServer server = new GrpcServer(localIp, localPort);
        server.start(); // async
        System.out.printf("Node started: %s:%d (ID: %s)%n", localIp, localPort, localNode.getId());

        // 3. Bootstrap contact
        String bootstrapIp = "127.0.0.1";
        int bootstrapPort = 50051;

//        KeyPair privateKey=localNode.generateKeyPair();
//        Signature signature = Signature.getInstance("SHA256withRSA");
//        signature.initSign(privateKey);
//        signature.update(bidData.getBytes());
//        byte[] signedData = signature.sign();


        GrpcClient bootstrapClient = new GrpcClient(bootstrapIp, bootstrapPort);
        if (localPort != bootstrapPort) { // Don't ping yourself
            bootstrapClient.sendNode(localNode);   // Add self to bootstrap’s routing table
        }

        GrpcClient selfClient = new GrpcClient(localIp,localPort);
        //selfClient.printRoutingTable();
//        Auction auction = new Auction();
//        Product product = new Product(1,"Phone",100.0f,150.0f);
//        Bid bid = new Bid(1,1,2);
//        auction.addProduct(product);
//        auction.addBid(bid);
//        bootstrapClient.sendAuction(auction);
//        System.out.println("BOOSTRAP AUCTION");
//        bootstrapClient.getAuctions();
//        System.out.println("SELF AUCTIONS");
//        selfClient.getAuctions();

        while (true) {
            Thread.sleep(1000);
            System.out.println("Selecione a opção pretendida ou 0 para terminar");
            System.out.println("1-Adcionar um produto");
            System.out.println("2-Criar um leilão");
            System.out.println("3-Visualizar leilões");
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

                System.out.println("Selecione o produto a adcionar no leilao ou 0 para terminar");
                while(true){
                    System.out.println("Produto:");
                    int choice=input.nextInt();
                    if(choice==0)
                        break;
                    System.out.println("Introduza o preço inicial do produto");
                    Float intialPrice = input.nextFloat();
                    Product product = products.get(choice-1);
                    product.setInitialPrice(intialPrice);
                    auctionProducts.add(product);
                }
                //Fazer um sendAuction para o no bootstrap
                Instant currentTimestamp = Instant.now();
                Auction auction = new Auction(auctionProducts,currentTimestamp,tempo,localNode.getId());
                bootstrapClient.sendAuction(auction);
            }
            else if(option==3){
                System.out.println("Selecione a opção pretendida");
                System.out.println("1-Ver todos os leiloes");
                System.out.println("2-Ver todos os leiloes em que participas");
                int choice= input.nextInt();
                if(choice==1){
                    List<Auction> auctions=bootstrapClient.getAuctions();
                    int auctionID=1;
                    for(Auction auction:auctions){
                        System.out.println("Leilão "+auctionID);
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


                            List<Bid> auctionBids = auctions.get(choice2-1).getBids();
                            //List<Bid> newBids = new ArrayList<>();
                            for (Product product : auctions.get(choice2-1).getProducts()) {
                                Bid lastBid=null;
                                Optional<Bid> lastBidOptional = auctionBids.stream()
                                        .filter(bid -> bid.getProductId() == product.getId())
                                        .reduce((first, second) -> second); // keeps only the last element

                                if (lastBidOptional.isPresent()) {
                                    lastBid = lastBidOptional.get();
                                    System.out.println("Produto:" + product.getName() + ", lance atual " + lastBid.getBidValue());

                                } else {
                                    System.out.println("Produto:" + product.getName() + ", lance atual " + product.getInitialPrice());
                                }

                                System.out.println("Introduza o valor do teu lance para o produto ou 0 caso não esteja interessado");
                                Float valor = input.nextFloat();
                                if (valor>0f && lastBid!=null && valor > lastBid.getBidValue() || valor>0f && valor > product.getInitialPrice()) {
                                    Bid newBid = new Bid((int) (Instant.now().getEpochSecond()), product.getId(), valor,localNode.getId(),auctions.get(choice2-1).getId());
                                    //SEND BID USING GRPC
                                    bootstrapClient.sendBid(newBid);
                                } else {
                                    System.out.println("Valor do lance tem de ser maior do que o lance atual");
                                }
                            }

                            break;
                        }
                    }
                }
                else if(choice==2){
                    List<Auction> allAuctions=bootstrapClient.getAuctions();
                    List<Auction> auctions=allAuctions.stream().filter(auction -> auction.getSenderHash().equals(localNode.getId()) || auction.getParticipants().contains(localNode.getId())).collect(Collectors.toList());
                    int auctionID=1;
                    for(Auction auction:auctions){
                        System.out.println("Leilão "+auctionID);
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


                            List<Bid> auctionBids = auctions.get(choice2-1).getBids();
                            //List<Bid> newBids = new ArrayList<>();
                            for (Product product : auctions.get(choice2-1).getProducts()) {
                                Bid lastBid=null;
                                Optional<Bid> lastBidOptional = auctionBids.stream()
                                        .filter(bid -> bid.getProductId() == product.getId())
                                        .reduce((first, second) -> second); // keeps only the last element

                                if (lastBidOptional.isPresent()) {
                                    lastBid = lastBidOptional.get();
                                    System.out.println("Produto:" + product.getName() + ", lance atual " + lastBid.getBidValue());

                                } else {
                                    System.out.println("Produto:" + product.getName() + ", lance atual " + product.getInitialPrice());
                                }

                                System.out.println("Introduza o valor do teu lance para o produto ou 0 caso não esteja interessado");
                                Float valor = input.nextFloat();
                                if (valor>0f && lastBid!=null && valor > lastBid.getBidValue() || valor>0f && valor > product.getInitialPrice()) {
                                    Bid newBid = new Bid((int) (Instant.now().getEpochSecond()), product.getId(), valor,localNode.getId(),auctions.get(choice2-1).getId());
                                    //SEND BID USING GRPC
                                    //Ao iniciar um novo no criar a sua assinatura digital e envia para o bootstrap para ele poder guardar no seu hashmap
                                    //Antes de enviar um bid buscar o hash do ultimo bloco para poder criar um novo bloco e enviar no bid a assinatura em vez de
                                    //enviar o seu id
                                    //Para buscar a ultima assinatura adcionan a chave publica dele no que ele vai enviar
                                    //para buscar a ultima transcação
                                    //quando ele envia um novo bloco ele verifica se a hash é valida
                                    //depois implementar tmb uma forma de verificar se o ultimo bid para uma auction foi feito pela mesma pessoa
                                    //e nao propagar e criar bloco para evitar ataques de repetição
                                    bootstrapClient.sendBid(newBid);
                                } else {
                                    System.out.println("Valor do lance tem de ser maior do que o lance atual");
                                }
                            }

                            break;
                        }
                    }
                }
            }
            else if(option==0){
                List<Product> productsSaved=Utils.loadProductFromJsonFile("Products.json");
                for (Product product:productsSaved)
                    System.out.println("Carrosell "+product.getId()+":"+product.getName());
                break;
            }
            else{
                System.out.println("Opção não encontrada");
            }
        }





        // 4. Keep server running
        server.blockUntilShutdown();



    }

    //Cria id de leilao apartir de hash de timestamp atual
    //Ora que ta envia um leilao adciona na objecto leilao no de quenha que envia leilao asi pa na ora de
    //Ora ki é pa lista leilao, busca de service leiloes que sta registado na nha no
    //Cria na service grpc um service que ta busca no apartir de informaçoes de um no
    //Tenta cria um leilao e faze broadcast del pa tudo no que sta na rede
    //Depos djobi se ta guarda leilao e routingTable na um ficheiro json e sempre que programa ta começa le quel ficheiro
}
