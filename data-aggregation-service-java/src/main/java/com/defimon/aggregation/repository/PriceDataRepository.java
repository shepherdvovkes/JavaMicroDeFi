package com.defimon.aggregation.repository;

import com.defimon.aggregation.model.PriceData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceDataRepository extends MongoRepository<PriceData, String> {
    
    List<PriceData> findBySymbol(String symbol);
    
    List<PriceData> findBySymbolAndTimestampBetween(String symbol, LocalDateTime start, LocalDateTime end);
    
    @Query("{'timestamp': {$gte: ?0}}")
    List<PriceData> findRecentData(LocalDateTime since);
    
    @Query("{'symbol': ?0}")
    PriceData findLatestBySymbol(String symbol);
}