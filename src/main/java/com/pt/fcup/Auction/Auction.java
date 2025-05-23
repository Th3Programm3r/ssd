package com.pt.fcup.Auction;



import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private int id;
    private List<Bid> bids;
    private List<Product> products;
    private int hoursToCloseAuction;
    private String senderHash;
    private Instant creationTimeStamp;
    private List<String> participants;

    public Auction(int id, List<Bid> bids, List<Product> products, int hoursToCloseAuction, String senderHash, Instant creationTimeStamp, List<String> participants) {
        this.id = id;
        this.bids = bids;
        this.products = products;
        this.hoursToCloseAuction = hoursToCloseAuction;
        this.senderHash = senderHash;
        this.creationTimeStamp = creationTimeStamp;
        this.participants = participants;
    }

    public Auction(int id, List<Bid> bids, List<Product> products) {
        this.id = id;
        this.bids = bids;
        this.products = products;
        this.hoursToCloseAuction=0;
        this.senderHash="";
        this.creationTimeStamp=null;
        this.participants = new ArrayList<>();
    }

    public Auction() {
        this.id = 0;
        this.bids = new ArrayList<>();
        this.products = new ArrayList<>();
        this.hoursToCloseAuction=0;
        this.senderHash="";
        this.creationTimeStamp=null;
        this.participants = new ArrayList<>();
    }

    public Auction(List<Product> products, Instant creationTimeStamp, int hoursToCloseAuction,String senderHash) {
        this.products = products;
        this.creationTimeStamp = creationTimeStamp;
        this.hoursToCloseAuction = hoursToCloseAuction;
        this.senderHash=senderHash;
        this.id=(int) (creationTimeStamp.getEpochSecond());
        this.participants=new ArrayList<>();
        this.bids=new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }



    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public void addBid(Bid bid){bids.add(bid);}

    public void addProduct(Product product){products.add(product);}

    public List<Bid> getBids() {
        return bids;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }

    public int getHoursToCloseAuction() {
        return hoursToCloseAuction;
    }

    public void setHoursToCloseAuction(int hoursToCloseAuction) {
        this.hoursToCloseAuction = hoursToCloseAuction;
    }

    public String getSenderHash() {
        return senderHash;
    }

    public void setSenderHash(String senderHash) {
        this.senderHash = senderHash;
    }

    public Instant getCreationTimeStamp() {
        return creationTimeStamp;
    }

    public void setCreationTimeStamp(Instant creationTimeStamp) {
        this.creationTimeStamp = creationTimeStamp;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

}
