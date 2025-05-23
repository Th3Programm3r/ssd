package com.pt.fcup.BlockChain;

import com.pt.fcup.Auction.Auction;
import com.pt.fcup.Auction.Bid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Block {
    private int index;
    private long timestamp;
    private Auction auction;
    private String previousHash;
    private String hash;

    public Block(int index, long timestamp, Auction auction, String previousHash, String hash) {
        this.index = index;
        this.timestamp = timestamp;
        this.auction = auction;
        this.previousHash = previousHash;
        this.hash = hash;
    }

    public Block() {
        this.index = 0;
        this.timestamp = 0L;
        this.auction = new Auction();
        this.previousHash = "";
        this.hash = "";
    }

    public Block(int index, long timestamp, Auction auction, String previousHash) {
        this.index = index;
        this.timestamp = timestamp;
        this.auction = auction;
        this.previousHash = previousHash;
        this.hash = calculateHash();
    }

    public String calculateHash() {
        String data = index + timestamp + auction.toString() + previousHash;
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
}
