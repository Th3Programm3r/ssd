package com.pt.fcup.BlockChain;

import com.pt.fcup.Auction.Bid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Block {
    private int index;
    private long timestamp;
    private Bid bid;
    private String previousHash;
    private String hash;

    public Block(int index, long timestamp, Bid bid, String previousHash, String hash) {
        this.index = index;
        this.timestamp = timestamp;
        this.bid = bid;
        this.previousHash = previousHash;
        this.hash = hash;
    }

    public Block() {
        this.index = 0;
        this.timestamp = 0L;
        this.bid = new Bid();
        this.previousHash = "";
        this.hash = "";
    }

    public Block(int index, long timestamp, Bid bid, String previousHash) {
        this.index = index;
        this.timestamp = timestamp;
        this.bid = bid;
        this.previousHash = previousHash;
        this.hash = calculateHash();
    }

    public String calculateHash() {
        String data = index + timestamp + bid.toString() + previousHash;
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

    public Bid getBid() {
        return bid;
    }

    public void setBid(Bid bid) {
        this.bid = bid;
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
}
