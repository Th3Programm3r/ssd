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

    public void addBlockToBlockChain(Block block) {
        chain.add(block);
    }

    public List<Block> getChain() {
        return chain;
    }

    public String validateAndAdd(Block block) {
        int difficulty = 4; // Number of leading zeros required
        block.mineBlock(difficulty); // Only accept blocks that solve the challenge

        Block latest = getLatestBlock();

        // Basic validations
        if (block.getIndex() != latest.getIndex() + 1) {
            return ("Invalid index");
        }

        if (!block.getPreviousHash().equals(latest.getHash())) {
            return("Previous hash mismatch");
        }

        String recalculatedHash = block.calculateHash();
        if (!block.getHash().equals(recalculatedHash)) {
            return("Invalid hash");
        }

        // 4. Check proof-of-work: hash must start with required number of zeros
        String targetPrefix = "0".repeat(difficulty);
        if (!block.getHash().startsWith(targetPrefix)) {
            return "Block does not satisfy proof-of-work";
        }


        chain.add(block);
        return "";
    }

    public void setChain(List<Block> chain) {
        this.chain = chain;
    }
}
