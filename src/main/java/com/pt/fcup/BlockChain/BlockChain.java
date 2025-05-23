package com.pt.fcup.BlockChain;

import com.pt.fcup.Auction.Auction;

import java.util.ArrayList;
import java.util.List;

public class BlockChain {
    private List<Block> chain = new ArrayList<>();

    public BlockChain() {
        chain.add(createGenesisBlock());
    }

    private Block createGenesisBlock() {
        return new Block(0, System.currentTimeMillis(), null, "0");
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public void addBlock(Auction auction) {
        Block lastBlock = getLatestBlock();
        Block newBlock = new Block(
                lastBlock.getIndex() + 1,
                System.currentTimeMillis(),
                auction,lastBlock
                .getHash()
        );
        chain.add(newBlock);
    }

    public List<Block> getChain() {
        return chain;
    }

    public boolean validateAndAdd(Block block) {
        Block latest = getLatestBlock();

        // Basic validations
        if (block.getIndex() != latest.getIndex() + 1) {
            System.out.println("Invalid index");
            return false;
        }

        if (!block.getPreviousHash().equals(latest.getHash())) {
            System.out.println("Previous hash mismatch");
            return false;
        }

        String recalculatedHash = block.calculateHash();
        if (!block.getHash().equals(recalculatedHash)) {
            System.out.println("Invalid hash");
            return false;
        }

        // Optionally validate the bid inside the block if needed

        chain.add(block);
        return true;
    }

    public void setChain(List<Block> chain) {
        this.chain = chain;
    }
}
