package com.defimon.aggregation.controller;

import com.defimon.aggregation.model.PriceData;
import com.defimon.aggregation.repository.PriceDataRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final PriceDataRepository priceDataRepository;

    public DataController(PriceDataRepository priceDataRepository) {
        this.priceDataRepository = priceDataRepository;
    }

    @GetMapping("/prices/{symbol}")
    public ResponseEntity<List<PriceData>> getPricesBySymbol(@PathVariable String symbol) {
        List<PriceData> prices = priceDataRepository.findBySymbol(symbol);
        return ResponseEntity.ok(prices);
    }

    @GetMapping("/prices/{symbol}/latest")
    public ResponseEntity<PriceData> getLatestPrice(@PathVariable String symbol) {
        PriceData latestPrice = priceDataRepository.findLatestBySymbol(symbol);
        return latestPrice != null ? ResponseEntity.ok(latestPrice) : ResponseEntity.notFound().build();
    }

    @GetMapping("/prices/{symbol}/range")
    public ResponseEntity<List<PriceData>> getPricesInRange(
            @PathVariable String symbol,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        List<PriceData> prices = priceDataRepository.findBySymbolAndTimestampBetween(symbol, start, end);
        return ResponseEntity.ok(prices);
    }

    @PostMapping("/prices")
    public ResponseEntity<PriceData> createPriceData(@RequestBody PriceData priceData) {
        PriceData savedData = priceDataRepository.save(priceData);
        return ResponseEntity.ok(savedData);
    }
}
