package com.defimon.bitcoin.metrics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Bitcoin RPC Request Model
 */
public class BitcoinRpcRequest {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "1.0";
    
    private String id;
    private String method;
    private List<Object> params;
    
    public BitcoinRpcRequest() {}
    
    public BitcoinRpcRequest(String id, String method, List<Object> params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }
    
    // Getters and Setters
    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public List<Object> getParams() { return params; }
    public void setParams(List<Object> params) { this.params = params; }
}
