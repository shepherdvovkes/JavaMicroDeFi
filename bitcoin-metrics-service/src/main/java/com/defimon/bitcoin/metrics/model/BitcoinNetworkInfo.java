package com.defimon.bitcoin.metrics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bitcoin Network Information Model
 */
public class BitcoinNetworkInfo {
    
    private long version;
    private String subversion;
    private long protocolversion;
    private String localservices;
    private long localrelay;
    private long timeoffset;
    private long networkactive;
    private long connections;
    private String[] networks;
    private double relayfee;
    private double incrementalfee;
    private String[] localaddresses;
    private String warnings;
    
    // Getters and Setters
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    
    public String getSubversion() { return subversion; }
    public void setSubversion(String subversion) { this.subversion = subversion; }
    
    public long getProtocolversion() { return protocolversion; }
    public void setProtocolversion(long protocolversion) { this.protocolversion = protocolversion; }
    
    public String getLocalservices() { return localservices; }
    public void setLocalservices(String localservices) { this.localservices = localservices; }
    
    public long getLocalrelay() { return localrelay; }
    public void setLocalrelay(long localrelay) { this.localrelay = localrelay; }
    
    public long getTimeoffset() { return timeoffset; }
    public void setTimeoffset(long timeoffset) { this.timeoffset = timeoffset; }
    
    public long getNetworkactive() { return networkactive; }
    public void setNetworkactive(long networkactive) { this.networkactive = networkactive; }
    
    public long getConnections() { return connections; }
    public void setConnections(long connections) { this.connections = connections; }
    
    public String[] getNetworks() { return networks; }
    public void setNetworks(String[] networks) { this.networks = networks; }
    
    public double getRelayfee() { return relayfee; }
    public void setRelayfee(double relayfee) { this.relayfee = relayfee; }
    
    public double getIncrementalfee() { return incrementalfee; }
    public void setIncrementalfee(double incrementalfee) { this.incrementalfee = incrementalfee; }
    
    public String[] getLocaladdresses() { return localaddresses; }
    public void setLocaladdresses(String[] localaddresses) { this.localaddresses = localaddresses; }
    
    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }
}
