package com.pt;

import com.pt.Auction.Auction;
import com.pt.Auction.Bid;
import com.pt.Auction.Product;
import kademlia.Kademlia.AuctionGrpc;
import kademlia.Kademlia.BidGrpc;
import kademlia.Kademlia.ProductGrpc;

import java.util.List;
import java.util.stream.Collectors;


public class Utils {
    public static Auction convertAuctionFromProto(AuctionGrpc proto) {
        List<Bid> bids = proto.getBidsList().stream()
                .map(b -> new Bid(b.getId(), b.getProductId(), b.getBidId()))
                .collect(Collectors.toList());

        List<Product> products = proto.getProductsList().stream()
                .map(p -> new Product(p.getId(), p.getName(), p.getInitialPrice(), p.getFinalPrice()))
                .collect(Collectors.toList());

        return new Auction(proto.getId(), bids, products);
    }

    public static AuctionGrpc convertAutctionToProto(Auction auction) {
        AuctionGrpc.Builder builder = AuctionGrpc.newBuilder()
                .setId(auction.getId());

        for (Bid bid : auction.getLances()) {
            builder.addBids(BidGrpc.newBuilder()
                    .setId(bid.getId())
                    .setProductId(bid.getProductId())
                    .setBidId(bid.getBidId())
                    .build());
        }

        for (Product product : auction.getProducts()) {
            builder.addProducts(ProductGrpc.newBuilder()
                    .setId(product.getId())
                    .setName(product.getName())
                    .setInitialPrice(product.getInitialPrice())
                    .setFinalPrice(product.getFinalPrice())
                    .build());
        }

        return builder.build();
    }
}
