package com.pt.fcup.Auction;

public class Bid {
    private int id;
    private int productId;
    private int auctionId;
    private float bidValue;
    private String sender;
    //Criar assinatura digital quando se envia um novo bid


    public Bid(int id, int productId, float bidValue, String sender,int auctionId) {
        this.id = id;
        this.productId = productId;
        this.bidValue = bidValue;
        this.sender = sender;
        this.auctionId = auctionId;
    }

    public Bid() {
        this.id = 0;
        this.productId = 0;
        this.bidValue = 0;
        this.sender = "";
        this.auctionId = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public float getBidValue() {
        return bidValue;
    }

    public void setBidValue(float bidValue) {
        this.bidValue = bidValue;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    @Override
    public String toString() {
        return "Bid{" +
                "id=" + id +
                ", productId=" + productId +
                ", auctionId=" + auctionId +
                ", bidValue=" + bidValue +
                ", sender='" + sender + '\'' +
                '}';
    }
}

