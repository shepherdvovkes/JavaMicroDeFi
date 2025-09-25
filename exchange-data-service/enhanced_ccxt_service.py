#!/usr/bin/env python3
"""
Enhanced CCXT Service for Ethereum Prediction Microservice
=========================================================

This service enhances your existing ~/ETHL2/ccxt implementation to:
1. Focus on ETH data collection (not just BTC)
2. Integrate with your Kafka/MongoDB infrastructure
3. Provide real-time streaming capabilities
4. Support multiple cryptocurrencies
5. Feed data to your Ethereum prediction service
"""

import asyncio
import json
import logging
import time
from datetime import datetime, timedelta
from typing import List, Dict, Optional, Any
from dataclasses import dataclass, asdict
import sys
import os

# Add your existing CCXT implementation to path
sys.path.append('/home/vovkes/ETHL2/ccxt')

import ccxt
import ccxt.async_support as ccxt_async
import pandas as pd
import numpy as np
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import structlog
from motor.motor_asyncio import AsyncIOMotorClient
from aiokafka import AIOKafkaProducer
import redis.asyncio as redis
from prometheus_client import Counter, Histogram, Gauge, start_http_server
import uvicorn

# Configure structured logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.processors.JSONRenderer()
    ],
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    wrapper_class=structlog.stdlib.BoundLogger,
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger()

# Prometheus metrics
eth_data_collected = Counter('eth_data_collected_total', 'ETH data points collected', ['exchange', 'symbol', 'data_type'])
collection_duration = Histogram('eth_collection_duration_seconds', 'Time spent collecting ETH data', ['exchange', 'operation'])
active_connections = Gauge('eth_exchange_connections', 'Active exchange connections for ETH')
arbitrage_opportunities = Counter('eth_arbitrage_opportunities_total', 'ETH arbitrage opportunities detected', ['symbol'])

@dataclass
class ETHMarketData:
    symbol: str
    exchange: str
    timestamp: datetime
    data_type: str  # 'ticker', 'ohlcv', 'orderbook', 'trades'
    data: Dict[str, Any]
    
@dataclass
class ETHOHLCV:
    symbol: str
    exchange: str
    timeframe: str
    timestamp: datetime
    open: float
    high: float
    low: float
    close: float
    volume: float
    quote_volume: Optional[float] = None

@dataclass
class ETHTicker:
    symbol: str
    exchange: str
    timestamp: datetime
    last: float
    bid: Optional[float] = None
    ask: Optional[float] = None
    high: Optional[float] = None
    low: Optional[float] = None
    volume: Optional[float] = None
    quote_volume: Optional[float] = None
    change: Optional[float] = None
    percentage: Optional[float] = None

@dataclass
class ETHArbitrage:
    symbol: str
    buy_exchange: str
    sell_exchange: str
    buy_price: float
    sell_price: float
    spread: float
    spread_percentage: float
    volume: float
    timestamp: datetime
    estimated_profit: float

class EnhancedCCXTService:
    """Enhanced CCXT service for ETH data collection"""
    
    def __init__(self):
        # ETH-focused symbols
        self.eth_symbols = [
            'ETH/USDT', 'ETH/BTC', 'ETH/USD', 'ETH/EUR', 'ETH/GBP',
            'ETH/JPY', 'ETH/KRW', 'ETH/CNY'
        ]
        
        # Initialize exchanges (using your existing config)
        self.exchanges = {}
        self.async_exchanges = {}
        self.init_exchanges()
        
        # External service connections
        self.kafka_producer = None
        self.mongodb_client = None
        self.redis_client = None
        self.running = False
        
    def init_exchanges(self):
        """Initialize CCXT exchanges for ETH data collection"""
        exchange_configs = {
            'binance': {'symbol': 'ETH/USDT', 'rate_limit': 1200},
            'coinbase': {'symbol': 'ETH/USD', 'rate_limit': 1000},
            'kraken': {'symbol': 'ETH/USD', 'rate_limit': 3000},
            'bitfinex': {'symbol': 'ETH/USD', 'rate_limit': 1500},
            'huobi': {'symbol': 'ETH/USDT', 'rate_limit': 1000},
            'okx': {'symbol': 'ETH/USDT', 'rate_limit': 1000},
            'bybit': {'symbol': 'ETH/USDT', 'rate_limit': 1000},
            'kucoin': {'symbol': 'ETH/USDT', 'rate_limit': 1000},
            'gate': {'symbol': 'ETH/USDT', 'rate_limit': 1000},
            'mexc': {'symbol': 'ETH/USDT', 'rate_limit': 1000},
        }
        
        for name, config in exchange_configs.items():
            try:
                # Sync exchange
                exchange_class = getattr(ccxt, name)
                self.exchanges[name] = exchange_class({
                    'rateLimit': config['rate_limit'],
                    'enableRateLimit': True,
                    'timeout': 30000,
                })
                
                # Async exchange
                async_exchange_class = getattr(ccxt_async, name)
                self.async_exchanges[name] = async_exchange_class({
                    'rateLimit': config['rate_limit'],
                    'enableRateLimit': True,
                    'timeout': 30000,
                })
                
                logger.info(f"Initialized {name} exchange for ETH data")
                
            except Exception as e:
                logger.error(f"Failed to initialize {name}: {e}")
    
    async def initialize(self):
        """Initialize connections to external services"""
        logger.info("Initializing Enhanced CCXT Service")
        
        # Initialize Kafka producer
        self.kafka_producer = AIOKafkaProducer(
            bootstrap_servers=['localhost:9092'],
            value_serializer=lambda x: json.dumps(x, default=str).encode('utf-8'),
            key_serializer=lambda x: x.encode('utf-8') if x else None
        )
        await self.kafka_producer.start()
        
        # Initialize MongoDB client
        self.mongodb_client = AsyncIOMotorClient('mongodb://admin:defimon123@localhost:27017/eth_exchange_data?authSource=admin')
        
        # Initialize Redis client
        self.redis_client = redis.Redis(
            host='localhost',
            port=6379,
            password='defimon123',
            decode_responses=True
        )
        
        logger.info("Enhanced CCXT Service initialized successfully")
    
    async def collect_eth_ticker_data(self, symbols: List[str] = None) -> List[ETHTicker]:
        """Collect ETH ticker data from all exchanges"""
        if symbols is None:
            symbols = ['ETH/USDT', 'ETH/BTC', 'ETH/USD']
        
        ticker_data = []
        
        for exchange_name, exchange in self.async_exchanges.items():
            try:
                for symbol in symbols:
                    if symbol in exchange.markets:
                        ticker = await exchange.fetch_ticker(symbol)
                        
                        ticker_data.append(ETHTicker(
                            symbol=symbol,
                            exchange=exchange_name,
                            timestamp=datetime.now(),
                            last=ticker['last'],
                            bid=ticker.get('bid'),
                            ask=ticker.get('ask'),
                            high=ticker.get('high'),
                            low=ticker.get('low'),
                            volume=ticker.get('baseVolume'),
                            quote_volume=ticker.get('quoteVolume'),
                            change=ticker.get('change'),
                            percentage=ticker.get('percentage')
                        ))
                        
                        eth_data_collected.labels(
                            exchange=exchange_name,
                            symbol=symbol,
                            data_type='ticker'
                        ).inc()
                        
            except Exception as e:
                logger.error(f"Error collecting ETH ticker from {exchange_name}: {e}")
        
        return ticker_data
    
    async def collect_eth_ohlcv_data(self, symbol: str = 'ETH/USDT', timeframe: str = '1h', limit: int = 100) -> List[ETHOHLCV]:
        """Collect ETH OHLCV data from all exchanges"""
        ohlcv_data = []
        
        for exchange_name, exchange in self.async_exchanges.items():
            try:
                if symbol in exchange.markets:
                    ohlcv = await exchange.fetch_ohlcv(symbol, timeframe, limit=limit)
                    
                    for candle in ohlcv:
                        ohlcv_data.append(ETHOHLCV(
                            symbol=symbol,
                            exchange=exchange_name,
                            timeframe=timeframe,
                            timestamp=datetime.fromtimestamp(candle[0] / 1000),
                            open=candle[1],
                            high=candle[2],
                            low=candle[3],
                            close=candle[4],
                            volume=candle[5]
                        ))
                    
                    eth_data_collected.labels(
                        exchange=exchange_name,
                        symbol=symbol,
                        data_type='ohlcv'
                    ).inc()
                    
            except Exception as e:
                logger.error(f"Error collecting ETH OHLCV from {exchange_name}: {e}")
        
        return ohlcv_data
    
    async def detect_eth_arbitrage(self, symbol: str = 'ETH/USDT', min_spread: float = 0.5) -> List[ETHArbitrage]:
        """Detect ETH arbitrage opportunities across exchanges"""
        opportunities = []
        
        # Collect ticker data from all exchanges
        tickers = await self.collect_eth_ticker_data([symbol])
        
        if len(tickers) < 2:
            return opportunities
        
        # Group by symbol
        symbol_tickers = {t.symbol: t for t in tickers if t.symbol == symbol}
        
        if len(symbol_tickers) < 2:
            return opportunities
        
        # Find best buy and sell prices
        best_buy = min(symbol_tickers.values(), key=lambda x: x.ask or x.last)
        best_sell = max(symbol_tickers.values(), key=lambda x: x.bid or x.last)
        
        if best_buy.exchange != best_sell.exchange:
            buy_price = best_buy.ask or best_buy.last
            sell_price = best_sell.bid or best_sell.last
            
            spread = sell_price - buy_price
            spread_percentage = (spread / buy_price) * 100
            
            if spread_percentage >= min_spread:
                opportunities.append(ETHArbitrage(
                    symbol=symbol,
                    buy_exchange=best_buy.exchange,
                    sell_exchange=best_sell.exchange,
                    buy_price=buy_price,
                    sell_price=sell_price,
                    spread=spread,
                    spread_percentage=spread_percentage,
                    volume=min(best_buy.volume or 0, best_sell.volume or 0),
                    timestamp=datetime.now(),
                    estimated_profit=spread * min(best_buy.volume or 0, best_sell.volume or 0)
                ))
                
                arbitrage_opportunities.labels(symbol=symbol).inc()
        
        return opportunities
    
    async def stream_eth_data(self, symbols: List[str], data_types: List[str]):
        """Stream real-time ETH data via WebSocket"""
        websocket_tasks = []
        
        for exchange_name, exchange in self.async_exchanges.items():
            if hasattr(exchange, 'watch_ticker'):
                for symbol in symbols:
                    if symbol in exchange.markets:
                        task = asyncio.create_task(
                            self._stream_eth_websocket(exchange_name, exchange, symbol, data_types)
                        )
                        websocket_tasks.append(task)
        
        if websocket_tasks:
            await asyncio.gather(*websocket_tasks, return_exceptions=True)
    
    async def _stream_eth_websocket(self, exchange_name: str, exchange, symbol: str, data_types: List[str]):
        """Stream WebSocket data for ETH"""
        try:
            active_connections.inc()
            
            while self.running:
                try:
                    if 'ticker' in data_types:
                        ticker = await exchange.watch_ticker(symbol)
                        await self._publish_eth_data('eth_ticker', {
                            'exchange': exchange_name,
                            'symbol': symbol,
                            'data': ticker,
                            'timestamp': datetime.now().isoformat()
                        })
                    
                    if 'orderbook' in data_types:
                        orderbook = await exchange.watch_order_book(symbol)
                        await self._publish_eth_data('eth_orderbook', {
                            'exchange': exchange_name,
                            'symbol': symbol,
                            'data': orderbook,
                            'timestamp': datetime.now().isoformat()
                        })
                    
                    if 'trades' in data_types:
                        trades = await exchange.watch_trades(symbol)
                        for trade in trades:
                            await self._publish_eth_data('eth_trades', {
                                'exchange': exchange_name,
                                'symbol': symbol,
                                'data': trade,
                                'timestamp': datetime.now().isoformat()
                            })
                
                except Exception as e:
                    logger.error(f"ETH WebSocket error for {exchange_name} {symbol}: {e}")
                    await asyncio.sleep(5)
        
        finally:
            active_connections.dec()
    
    async def _publish_eth_data(self, topic: str, data: Dict[str, Any]):
        """Publish ETH data to Kafka topic"""
        try:
            await self.kafka_producer.send_and_wait(topic, value=data, key=data.get('exchange'))
        except Exception as e:
            logger.error(f"Error publishing ETH data to Kafka topic {topic}: {e}")
    
    async def store_eth_data_mongodb(self, collection: str, data: List[Any]):
        """Store ETH data to MongoDB collection"""
        try:
            if data and self.mongodb_client:
                db = self.mongodb_client.eth_exchange_data
                await db[collection].insert_many([asdict(item) if hasattr(item, '__dataclass_fields__') else item for item in data])
        except Exception as e:
            logger.error(f"Error storing ETH data to MongoDB collection {collection}: {e}")
    
    async def cache_eth_data_redis(self, key: str, data: Any, ttl: int = 300):
        """Cache ETH data in Redis"""
        try:
            if self.redis_client:
                await self.redis_client.setex(key, ttl, json.dumps(data, default=str))
        except Exception as e:
            logger.error(f"Error caching ETH data to Redis key {key}: {e}")
    
    async def get_eth_market_summary(self) -> Dict[str, Any]:
        """Get comprehensive ETH market summary"""
        try:
            tickers = await self.collect_eth_ticker_data()
            arbitrage_opps = await self.detect_eth_arbitrage()
            
            # Calculate market metrics
            prices = [t.last for t in tickers if t.last]
            avg_price = np.mean(prices) if prices else 0
            price_std = np.std(prices) if len(prices) > 1 else 0
            
            total_volume = sum([t.volume or 0 for t in tickers])
            
            return {
                'timestamp': datetime.now().isoformat(),
                'average_price': float(avg_price),
                'price_volatility': float(price_std),
                'total_volume_24h': float(total_volume),
                'exchange_count': len(set(t.exchange for t in tickers)),
                'arbitrage_opportunities': len(arbitrage_opps),
                'exchanges': [t.exchange for t in tickers],
                'best_buy_price': min([t.ask or t.last for t in tickers if t.ask or t.last]),
                'best_sell_price': max([t.bid or t.last for t in tickers if t.bid or t.last])
            }
            
        except Exception as e:
            logger.error(f"Error generating ETH market summary: {e}")
            return {}
    
    async def shutdown(self):
        """Shutdown the service"""
        logger.info("Shutting down Enhanced CCXT Service")
        self.running = False
        
        # Close all exchange connections
        for exchange in self.async_exchanges.values():
            if hasattr(exchange, 'close'):
                await exchange.close()
        
        # Close external connections
        if self.kafka_producer:
            await self.kafka_producer.stop()
        
        if self.mongodb_client:
            self.mongodb_client.close()
        
        if self.redis_client:
            await self.redis_client.close()

# FastAPI application
app = FastAPI(
    title="Enhanced CCXT Service for ETH",
    description="Enhanced CCXT service focused on Ethereum data collection",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global service instance
eth_service = EnhancedCCXTService()

@app.on_event("startup")
async def startup_event():
    """Initialize the service on startup"""
    await eth_service.initialize()
    eth_service.running = True
    
    # Start Prometheus metrics server
    start_http_server(8091)

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    await eth_service.shutdown()

# API Endpoints
@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "exchanges_connected": len(eth_service.async_exchanges),
        "eth_symbols": eth_service.eth_symbols,
        "active_connections": active_connections._value.get()
    }

@app.get("/eth/ticker")
async def get_eth_ticker():
    """Get ETH ticker data from all exchanges"""
    with collection_duration.labels(operation='eth_ticker').time():
        ticker_data = await eth_service.collect_eth_ticker_data()
    
    return {
        "symbol": "ETH",
        "data": [asdict(ticker) for ticker in ticker_data],
        "timestamp": datetime.now().isoformat()
    }

@app.get("/eth/ohlcv")
async def get_eth_ohlcv(symbol: str = "ETH/USDT", timeframe: str = "1h", limit: int = 100):
    """Get ETH OHLCV data"""
    with collection_duration.labels(operation='eth_ohlcv').time():
        ohlcv_data = await eth_service.collect_eth_ohlcv_data(symbol, timeframe, limit)
    
    return {
        "symbol": symbol,
        "timeframe": timeframe,
        "data": [asdict(ohlcv) for ohlcv in ohlcv_data],
        "timestamp": datetime.now().isoformat()
    }

@app.get("/eth/arbitrage")
async def get_eth_arbitrage(symbol: str = "ETH/USDT", min_spread: float = 0.5):
    """Get ETH arbitrage opportunities"""
    opportunities = await eth_service.detect_eth_arbitrage(symbol, min_spread)
    
    return {
        "symbol": symbol,
        "opportunities": [asdict(opp) for opp in opportunities],
        "timestamp": datetime.now().isoformat()
    }

@app.get("/eth/market-summary")
async def get_eth_market_summary():
    """Get comprehensive ETH market summary"""
    summary = await eth_service.get_eth_market_summary()
    
    return summary

@app.post("/eth/stream/start")
async def start_eth_streaming(background_tasks: BackgroundTasks, symbols: List[str] = ["ETH/USDT", "ETH/BTC"], data_types: List[str] = ["ticker", "orderbook", "trades"]):
    """Start WebSocket streaming for real-time ETH data"""
    background_tasks.add_task(eth_service.stream_eth_data, symbols, data_types)
    
    return {
        "status": "eth_streaming_started",
        "symbols": symbols,
        "data_types": data_types,
        "timestamp": datetime.now().isoformat()
    }

@app.get("/metrics")
async def get_metrics():
    """Get Prometheus metrics"""
    return {
        "eth_data_collected": dict(eth_data_collected._metrics),
        "collection_duration": dict(collection_duration._metrics),
        "active_connections": active_connections._value.get(),
        "arbitrage_opportunities": dict(arbitrage_opportunities._metrics)
    }

if __name__ == "__main__":
    uvicorn.run(
        "enhanced_ccxt_service:app",
        host="0.0.0.0",
        port=8089,
        reload=True,
        log_level="info"
    )
