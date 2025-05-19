package com.pt.Auction;

import java.util.ArrayList;
import java.util.List;

public class Auction {
    private int id;
    private List<Bid> lances;
    private List<Product> products;
    private int hours;

    public Auction(int id, List<Bid> lances, List<Product> products) {
        this.id = id;
        this.lances = lances;
        this.products = products;
    }

    public Auction() {
        this.id = 0;
        this.lances = new ArrayList<>();
        this.products = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Bid> getLances() {
        return lances;
    }

    public void setLances(List<Bid> lances) {
        this.lances = lances;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
