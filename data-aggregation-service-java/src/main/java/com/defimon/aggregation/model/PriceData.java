package com.defimon.aggregation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "price_data")
public class PriceData {
    
    @Id
    private String id;
    private String symbol;
    private BigDecimal price;
    private LocalDateTime timestamp;
    private String source;
    
    public PriceData() {}
    
    public PriceData(String symbol, BigDecimal price, LocalDateTime timestamp, String source) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
        this.source = source;
    }
    
    public PriceData(String id, String symbol, BigDecimal price, LocalDateTime timestamp, String source) {
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
        this.source = source;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}