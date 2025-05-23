package com.pt.fcup.Auction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Product {
    private int id;
    private String name;
    private Float initialPrice;
    private Float finalPrice;

    // Constructors
    public Product(int id, String name, Float initialPrice, Float finalPrice) {
        this.id = id;
        this.name = name;
        this.initialPrice = initialPrice;
        this.finalPrice = finalPrice;
    }

    public Product() {
        this.id=0;
        this.name="";
        this.initialPrice=0f;
        this.finalPrice=0f;
    }

    public Product(String name) {
        this.id=generateIdFromName(name);
        this.name=name;
        this.initialPrice=0f;
        this.finalPrice=0f;
    }

    public Product(int id, String name, Float initialPrice) {
        this(id, name, initialPrice, 0f);
    }

    // Getters and setters
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

    private int generateIdFromName(String name) {
        return name.toLowerCase().hashCode();
    }
}
