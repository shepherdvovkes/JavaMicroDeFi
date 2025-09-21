package com.defimon.bitcoin.metrics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bitcoin RPC Response Model
 */
public class BitcoinRpcResponse<T> {
    
    private T result;
    private BitcoinRpcError error;
    private String id;
    
    // Getters and Setters
    public T getResult() { return result; }
    public void setResult(T result) { this.result = result; }
    
    public BitcoinRpcError getError() { return error; }
    public void setError(BitcoinRpcError error) { this.error = error; }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public static class BitcoinRpcError {
        private int code;
        private String message;
        
        // Getters and Setters
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
