package com.pt.Auction;

public class Product {
    private int id;
    private String name;
    private Float initialPrice;
    private Float finalPrice;

    public Product(int id, String name, Float initialPrice, Float finalPrice) {
        this.id = id;
        this.name = name;
        this.initialPrice = initialPrice;
        this.finalPrice = finalPrice;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(Float initialPrice) {
        this.initialPrice = initialPrice;
    }

    public Float getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(Float finalPrice) {
        this.finalPrice = finalPrice;
    }

    public Product(int id, String name, Float initialPrice) {
        this.id = id;
        this.name = name;
        this.initialPrice = initialPrice;
        this.finalPrice = 0f;
    }




}