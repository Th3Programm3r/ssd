package com.pt.Auction;

public class Bid {
    private int id;
    private int productId;
    private int bidId;

    public Bid(int id, int productId, int bidId) {
        this.id = id;
        this.productId = productId;
        this.bidId = bidId;
    }

    public Bid() {
        this.id = 0;
        this.productId = 0;
        this.bidId = 0;
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

    public int getBidId() {
        return bidId;
    }

    public void setBidId(int bidId) {
        this.bidId = bidId;
    }
}

