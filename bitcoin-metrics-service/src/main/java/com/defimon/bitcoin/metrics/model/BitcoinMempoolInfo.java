package com.defimon.bitcoin.metrics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bitcoin Mempool Information Model
 */
public class BitcoinMempoolInfo {
    
    private boolean loaded;
    private long size;
    private long bytes;
    private long usage;
    
    @JsonProperty("total_fee")
    private double totalFee;
    
    private long maxmempool;
    private double mempoolminfee;
    private double minrelaytxfee;
    private double incrementalfee;
    private long unbroadcastcount;
    private boolean fullrbf;
    
    // Getters and Setters
    public boolean isLoaded() { return loaded; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    
    public long getBytes() { return bytes; }
    public void setBytes(long bytes) { this.bytes = bytes; }
    
    public long getUsage() { return usage; }
    public void setUsage(long usage) { this.usage = usage; }
    
    public double getTotalFee() { return totalFee; }
    public void setTotalFee(double totalFee) { this.totalFee = totalFee; }
    
    public long getMaxmempool() { return maxmempool; }
    public void setMaxmempool(long maxmempool) { this.maxmempool = maxmempool; }
    
    public double getMempoolminfee() { return mempoolminfee; }
    public void setMempoolminfee(double mempoolminfee) { this.mempoolminfee = mempoolminfee; }
    
    public double getMinrelaytxfee() { return minrelaytxfee; }
    public void setMinrelaytxfee(double minrelaytxfee) { this.minrelaytxfee = minrelaytxfee; }
    
    public double getIncrementalfee() { return incrementalfee; }
    public void setIncrementalfee(double incrementalfee) { this.incrementalfee = incrementalfee; }
    
    public long getUnbroadcastcount() { return unbroadcastcount; }
    public void setUnbroadcastcount(long unbroadcastcount) { this.unbroadcastcount = unbroadcastcount; }
    
    public boolean isFullrbf() { return fullrbf; }
    public void setFullrbf(boolean fullrbf) { this.fullrbf = fullrbf; }
}
