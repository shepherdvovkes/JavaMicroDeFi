package com.defimon.bitcoin.metrics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bitcoin Blockchain Information Model
 */
public class BitcoinBlockchainInfo {
    
    private String chain;
    private long blocks;
    private long headers;
    private String bestblockhash;
    private String bits;
    private String target;
    private double difficulty;
    private long time;
    private long mediantime;
    
    @JsonProperty("verificationprogress")
    private double verificationProgress;
    
    @JsonProperty("initialblockdownload")
    private boolean initialBlockDownload;
    
    private String chainwork;
    
    @JsonProperty("size_on_disk")
    private long sizeOnDisk;
    
    private boolean pruned;
    private String[] warnings;
    
    // Getters and Setters
    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }
    
    public long getBlocks() { return blocks; }
    public void setBlocks(long blocks) { this.blocks = blocks; }
    
    public long getHeaders() { return headers; }
    public void setHeaders(long headers) { this.headers = headers; }
    
    public String getBestblockhash() { return bestblockhash; }
    public void setBestblockhash(String bestblockhash) { this.bestblockhash = bestblockhash; }
    
    public String getBits() { return bits; }
    public void setBits(String bits) { this.bits = bits; }
    
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    
    public double getDifficulty() { return difficulty; }
    public void setDifficulty(double difficulty) { this.difficulty = difficulty; }
    
    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
    
    public long getMediantime() { return mediantime; }
    public void setMediantime(long mediantime) { this.mediantime = mediantime; }
    
    public double getVerificationProgress() { return verificationProgress; }
    public void setVerificationProgress(double verificationProgress) { this.verificationProgress = verificationProgress; }
    
    public boolean isInitialBlockDownload() { return initialBlockDownload; }
    public void setInitialBlockDownload(boolean initialBlockDownload) { this.initialBlockDownload = initialBlockDownload; }
    
    public String getChainwork() { return chainwork; }
    public void setChainwork(String chainwork) { this.chainwork = chainwork; }
    
    public long getSizeOnDisk() { return sizeOnDisk; }
    public void setSizeOnDisk(long sizeOnDisk) { this.sizeOnDisk = sizeOnDisk; }
    
    public boolean isPruned() { return pruned; }
    public void setPruned(boolean pruned) { this.pruned = pruned; }
    
    public String[] getWarnings() { return warnings; }
    public void setWarnings(String[] warnings) { this.warnings = warnings; }
}
