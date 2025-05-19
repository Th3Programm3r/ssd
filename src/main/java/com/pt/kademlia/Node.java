package com.pt.kademlia;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pt.Auction.Auction;


import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;


public class Node {
    private final String id;
    private final String ip;
    private final int port;
    //Lista Nos vizinhos em hash
    //List dAs transações e quem fex
    //Validar se a hash ta correta de acordo com a lista interna das hashs das transaçoes
    //Lista das transaçoes recebidas
    //propagar para os membros que participam na transação

    //Blockchain
    //Receber uma transação
    //Minar as transacoes
    //Enviar de volta a transacap



    public Node(){
        this.ip = getLocalIp();
        this.port = getFreePort();
        this.id = generateNodeId(this.ip, this.port);
    }

    @JsonCreator
    public Node(@JsonProperty("ip") String ip, @JsonProperty("port") int port) {
        this.ip = ip;
        this.port = port;
        this.id = generateNodeId(ip, port);
    }

    @JsonCreator
    public Node(String id, String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    private String generateNodeId(String ip, int port) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update((ip + ":" + port).getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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