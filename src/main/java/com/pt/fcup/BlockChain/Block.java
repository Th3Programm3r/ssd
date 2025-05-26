package com.pt.fcup.BlockChain;

import com.pt.fcup.Auction.Auction;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Block {
    private int index;
    private long timestamp;
    private Auction auction;
    private String previousHash;
    private String hash;
    private String signature;
    private int nonce;

    public Block(int index, long timestamp, Auction auction, String previousHash, String hash, String signature) {
        this.index = index;
        this.timestamp = timestamp;
        this.auction = auction;
        this.previousHash = previousHash;
        this.hash = hash;
        this.signature = signature;
    }

    public Block() {
        this.index = 0;
        this.timestamp = 0L;
        this.auction = new Auction();
        this.previousHash = "";
        this.hash = "";
        this.signature = "";
    }

    public Block(int index, long timestamp, Auction auction, String previousHash) {
        this.index = index;
        this.timestamp = timestamp;
        this.auction = auction;
        this.previousHash = previousHash;
        this.hash = calculateHash();
        this.signature = "";
        this.nonce = 0;
    }

    public String calculateHash() {
        String auctionString = (auction != null) ? auction.toString() : "null";
        String data = index + timestamp + auctionString + previousHash + nonce;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public void mineBlock(int difficulty) {
        String target = "0".repeat(difficulty); // E.g., "0000"
        while (!hash.startsWith(target)) {
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block mined: " + hash);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public String toCanonicalString() {
        return "Block{" +
                "index=" + index +
                ", timestamp=" + timestamp +
                ", auction=" + auction.toString() + // use the full string
                ", previousHash='" + previousHash + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
