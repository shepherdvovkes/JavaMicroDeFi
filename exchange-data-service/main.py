"""
Exchange Data Service - CCXT Integration
========================================

A high-performance microservice for collecting real-time and historical
cryptocurrency exchange data using the CCXT library.

Features:
- Real-time market data from 100+ exchanges
- Historical OHLCV data collection
- Order book and trade data streaming
- Arbitrage opportunity detection
- Market depth analysis
- Integration with existing Kafka/MongoDB infrastructure
"""

import asyncio
import json
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from enum import Enum

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
data_collected_counter = Counter('exchange_data_collected_total', 'Total data points collected', ['exchange', 'symbol', 'data_type'])
collection_duration = Histogram('exchange_data_collection_duration_seconds', 'Time spent collecting data', ['exchange', 'operation'])
active_connections = Gauge('exchange_active_connections', 'Active exchange connections')
websocket_connections = Gauge('exchange_websocket_connections', 'Active WebSocket connections')

class ExchangeType(str, Enum):
    SPOT = "spot"
    FUTURES = "futures"
    OPTIONS = "options"
    MARGIN = "margin"

class DataType(str, Enum):
    TICKER = "ticker"
    OHLCV = "ohlcv"
    ORDERBOOK = "orderbook"
    TRADES = "trades"
    FUNDING_RATE = "funding_rate"
    OPEN_INTEREST = "open_interest"

@dataclass
class ExchangeConfig:
    name: str
    exchange_id: str
    api_key: Optional[str] = None
    secret: Optional[str] = None
    password: Optional[str] = None
    sandbox: bool = False
    rate_limit: int = 1200
    enable_rate_limit: bool = True
    timeout: int = 30000
    verbose: bool = False

class MarketData(BaseModel):
    symbol: str
    exchange: str
    timestamp: datetime
    data_type: DataType
    data: Dict[str, Any]
    
class OHLCVData(BaseModel):
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

class TickerData(BaseModel):
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

class OrderBookData(BaseModel):
    symbol: str
    exchange: str
    timestamp: datetime
    bids: List[List[float]]
    asks: List[List[float]]
    nonce: Optional[int] = None

class TradeData(BaseModel):
    symbol: str
    exchange: str
    timestamp: datetime
    id: str
    side: str
    amount: float
    price: float
    cost: float

class ArbitrageOpportunity(BaseModel):
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

class ExchangeDataService:
    def __init__(self):
        self.exchanges: Dict[str, ccxt_async.Exchange] = {}
        self.exchange_configs: Dict[str, ExchangeConfig] = {}
        self.kafka_producer: Optional[AIOKafkaProducer] = None
        self.mongodb_client: Optional[AsyncIOMotorClient] = None
        self.redis_client: Optional[redis.Redis] = None
        self.running = False
        
    async def initialize(self):
        """Initialize connections to external services"""
        logger.info("Initializing Exchange Data Service")
        
        # Initialize Kafka producer
        self.kafka_producer = AIOKafkaProducer(
            bootstrap_servers=['localhost:9092'],
            value_serializer=lambda x: json.dumps(x, default=str).encode('utf-8'),
            key_serializer=lambda x: x.encode('utf-8') if x else None
        )
        await self.kafka_producer.start()
        
        # Initialize MongoDB client
        self.mongodb_client = AsyncIOMotorClient('mongodb://admin:defimon123@localhost:27017/exchange_data?authSource=admin')
        
        # Initialize Redis client
        self.redis_client = redis.Redis(
            host='localhost',
            port=6379,
            password='defimon123',
            decode_responses=True
        )
        
        # Initialize exchanges
        await self._initialize_exchanges()
        
        logger.info("Exchange Data Service initialized successfully")
    
    async def _initialize_exchanges(self):
        """Initialize CCXT exchange instances"""
        # Major exchanges for ETH trading
        exchange_configs = [
            ExchangeConfig("binance", "binance", sandbox=False),
            ExchangeConfig("coinbase", "coinbasepro", sandbox=False),
            ExchangeConfig("kraken", "kraken", sandbox=False),
            ExchangeConfig("huobi", "huobi", sandbox=False),
            ExchangeConfig("okx", "okx", sandbox=False),
            ExchangeConfig("bybit", "bybit", sandbox=False),
            ExchangeConfig("kucoin", "kucoin", sandbox=False),
            ExchangeConfig("gate", "gate", sandbox=False),
            ExchangeConfig("mexc", "mexc", sandbox=False),
            ExchangeConfig("bitget", "bitget", sandbox=False),
        ]
        
        for config in exchange_configs:
            try:
                exchange_class = getattr(ccxt_async, config.exchange_id)
                exchange = exchange_class({
                    'apiKey': config.api_key,
                    'secret': config.secret,
                    'password': config.password,
                    'sandbox': config.sandbox,
                    'rateLimit': config.rate_limit,
                    'enableRateLimit': config.enable_rate_limit,
                    'timeout': config.timeout,
                    'verbose': config.verbose,
                })
                
                # Test connection
                await exchange.load_markets()
                
                self.exchanges[config.name] = exchange
                self.exchange_configs[config.name] = config
                
                logger.info(f"Initialized exchange: {config.name}")
                
            except Exception as e:
                logger.error(f"Failed to initialize exchange {config.name}: {e}")
    
    async def collect_ticker_data(self, symbols: List[str] = None) -> List[TickerData]:
        """Collect ticker data from all exchanges"""
        if symbols is None:
            symbols = ['ETH/USDT', 'ETH/BTC', 'ETH/USD']
        
        ticker_data = []
        
        for exchange_name, exchange in self.exchanges.items():
            try:
                for symbol in symbols:
                    if symbol in exchange.markets:
                        ticker = await exchange.fetch_ticker(symbol)
                        
                        ticker_data.append(TickerData(
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
                        
                        data_collected_counter.labels(
                            exchange=exchange_name,
                            symbol=symbol,
                            data_type='ticker'
                        ).inc()
                        
            except Exception as e:
                logger.error(f"Error collecting ticker data from {exchange_name}: {e}")
        
        return ticker_data
    
    async def collect_ohlcv_data(self, symbol: str, timeframe: str = '1h', limit: int = 100) -> List[OHLCVData]:
        """Collect OHLCV data from all exchanges"""
        ohlcv_data = []
        
        for exchange_name, exchange in self.exchanges.items():
            try:
                if symbol in exchange.markets:
                    ohlcv = await exchange.fetch_ohlcv(symbol, timeframe, limit=limit)
                    
                    for candle in ohlcv:
                        ohlcv_data.append(OHLCVData(
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
                    
                    data_collected_counter.labels(
                        exchange=exchange_name,
                        symbol=symbol,
                        data_type='ohlcv'
                    ).inc()
                    
            except Exception as e:
                logger.error(f"Error collecting OHLCV data from {exchange_name}: {e}")
        
        return ohlcv_data
    
    async def collect_orderbook_data(self, symbol: str, limit: int = 20) -> List[OrderBookData]:
        """Collect order book data from all exchanges"""
        orderbook_data = []
        
        for exchange_name, exchange in self.exchanges.items():
            try:
                if symbol in exchange.markets:
                    orderbook = await exchange.fetch_order_book(symbol, limit)
                    
                    orderbook_data.append(OrderBookData(
                        symbol=symbol,
                        exchange=exchange_name,
                        timestamp=datetime.now(),
                        bids=orderbook['bids'],
                        asks=orderbook['asks'],
                        nonce=orderbook.get('nonce')
                    ))
                    
                    data_collected_counter.labels(
                        exchange=exchange_name,
                        symbol=symbol,
                        data_type='orderbook'
                    ).inc()
                    
            except Exception as e:
                logger.error(f"Error collecting orderbook data from {exchange_name}: {e}")
        
        return orderbook_data
    
    async def collect_trades_data(self, symbol: str, limit: int = 100) -> List[TradeData]:
        """Collect recent trades data from all exchanges"""
        trades_data = []
        
        for exchange_name, exchange in self.exchanges.items():
            try:
                if symbol in exchange.markets:
                    trades = await exchange.fetch_trades(symbol, limit=limit)
                    
                    for trade in trades:
                        trades_data.append(TradeData(
                            symbol=symbol,
                            exchange=exchange_name,
                            timestamp=datetime.fromtimestamp(trade['timestamp'] / 1000),
                            id=str(trade['id']),
                            side=trade['side'],
                            amount=trade['amount'],
                            price=trade['price'],
                            cost=trade['cost']
                        ))
                    
                    data_collected_counter.labels(
                        exchange=exchange_name,
                        symbol=symbol,
                        data_type='trades'
                    ).inc()
                    
            except Exception as e:
                logger.error(f"Error collecting trades data from {exchange_name}: {e}")
        
        return trades_data
    
    async def detect_arbitrage_opportunities(self, symbol: str, min_spread: float = 0.01) -> List[ArbitrageOpportunity]:
        """Detect arbitrage opportunities across exchanges"""
        opportunities = []
        
        # Collect ticker data from all exchanges
        tickers = await self.collect_ticker_data([symbol])
        
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
                opportunities.append(ArbitrageOpportunity(
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
        
        return opportunities
    
    async def stream_websocket_data(self, symbols: List[str], data_types: List[DataType]):
        """Stream real-time data via WebSocket connections"""
        websocket_tasks = []
        
        for exchange_name, exchange in self.exchanges.items():
            if hasattr(exchange, 'watch_ticker'):
                for symbol in symbols:
                    if symbol in exchange.markets:
                        task = asyncio.create_task(
                            self._stream_exchange_websocket(exchange_name, exchange, symbol, data_types)
                        )
                        websocket_tasks.append(task)
        
        if websocket_tasks:
            await asyncio.gather(*websocket_tasks, return_exceptions=True)
    
    async def _stream_exchange_websocket(self, exchange_name: str, exchange, symbol: str, data_types: List[DataType]):
        """Stream WebSocket data for a specific exchange"""
        try:
            websocket_connections.inc()
            
            while self.running:
                try:
                    if DataType.TICKER in data_types:
                        ticker = await exchange.watch_ticker(symbol)
                        await self._publish_to_kafka('ticker', {
                            'exchange': exchange_name,
                            'symbol': symbol,
                            'data': ticker,
                            'timestamp': datetime.now().isoformat()
                        })
                    
                    if DataType.ORDERBOOK in data_types:
                        orderbook = await exchange.watch_order_book(symbol)
                        await self._publish_to_kafka('orderbook', {
                            'exchange': exchange_name,
                            'symbol': symbol,
                            'data': orderbook,
                            'timestamp': datetime.now().isoformat()
                        })
                    
                    if DataType.TRADES in data_types:
                        trades = await exchange.watch_trades(symbol)
                        for trade in trades:
                            await self._publish_to_kafka('trades', {
                                'exchange': exchange_name,
                                'symbol': symbol,
                                'data': trade,
                                'timestamp': datetime.now().isoformat()
                            })
                
                except Exception as e:
                    logger.error(f"WebSocket error for {exchange_name} {symbol}: {e}")
                    await asyncio.sleep(5)
        
        finally:
            websocket_connections.dec()
    
    async def _publish_to_kafka(self, topic: str, data: Dict[str, Any]):
        """Publish data to Kafka topic"""
        try:
            await self.kafka_producer.send_and_wait(topic, value=data, key=data.get('exchange'))
        except Exception as e:
            logger.error(f"Error publishing to Kafka topic {topic}: {e}")
    
    async def store_to_mongodb(self, collection: str, data: List[Dict[str, Any]]):
        """Store data to MongoDB collection"""
        try:
            if data and self.mongodb_client:
                db = self.mongodb_client.exchange_data
                await db[collection].insert_many([asdict(item) if hasattr(item, '__dataclass_fields__') else item for item in data])
        except Exception as e:
            logger.error(f"Error storing to MongoDB collection {collection}: {e}")
    
    async def cache_to_redis(self, key: str, data: Any, ttl: int = 300):
        """Cache data in Redis"""
        try:
            if self.redis_client:
                await self.redis_client.setex(key, ttl, json.dumps(data, default=str))
        except Exception as e:
            logger.error(f"Error caching to Redis key {key}: {e}")
    
    async def shutdown(self):
        """Shutdown the service"""
        logger.info("Shutting down Exchange Data Service")
        self.running = False
        
        # Close all exchange connections
        for exchange in self.exchanges.values():
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
    title="Exchange Data Service",
    description="High-performance cryptocurrency exchange data collection using CCXT",
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
exchange_service = ExchangeDataService()

@app.on_event("startup")
async def startup_event():
    """Initialize the service on startup"""
    await exchange_service.initialize()
    exchange_service.running = True
    
    # Start Prometheus metrics server
    start_http_server(8090)

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    await exchange_service.shutdown()

# API Endpoints
@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "exchanges_connected": len(exchange_service.exchanges),
        "active_websockets": websocket_connections._value.get()
    }

@app.get("/exchanges")
async def list_exchanges():
    """List available exchanges"""
    return {
        "exchanges": list(exchange_service.exchanges.keys()),
        "total": len(exchange_service.exchanges)
    }

@app.get("/ticker/{symbol}")
async def get_ticker(symbol: str):
    """Get ticker data for a symbol across all exchanges"""
    with collection_duration.labels(operation='ticker').time():
        ticker_data = await exchange_service.collect_ticker_data([symbol])
    
    return {
        "symbol": symbol,
        "data": [ticker.dict() for ticker in ticker_data],
        "timestamp": datetime.now().isoformat()
    }

@app.get("/ohlcv/{symbol}")
async def get_ohlcv(symbol: str, timeframe: str = "1h", limit: int = 100):
    """Get OHLCV data for a symbol"""
    with collection_duration.labels(operation='ohlcv').time():
        ohlcv_data = await exchange_service.collect_ohlcv_data(symbol, timeframe, limit)
    
    return {
        "symbol": symbol,
        "timeframe": timeframe,
        "data": [ohlcv.dict() for ohlcv in ohlcv_data],
        "timestamp": datetime.now().isoformat()
    }

@app.get("/orderbook/{symbol}")
async def get_orderbook(symbol: str, limit: int = 20):
    """Get order book data for a symbol"""
    with collection_duration.labels(operation='orderbook').time():
        orderbook_data = await exchange_service.collect_orderbook_data(symbol, limit)
    
    return {
        "symbol": symbol,
        "data": [orderbook.dict() for orderbook in orderbook_data],
        "timestamp": datetime.now().isoformat()
    }

@app.get("/trades/{symbol}")
async def get_trades(symbol: str, limit: int = 100):
    """Get recent trades for a symbol"""
    with collection_duration.labels(operation='trades').time():
        trades_data = await exchange_service.collect_trades_data(symbol, limit)
    
    return {
        "symbol": symbol,
        "data": [trade.dict() for trade in trades_data],
        "timestamp": datetime.now().isoformat()
    }

@app.get("/arbitrage/{symbol}")
async def get_arbitrage_opportunities(symbol: str, min_spread: float = 0.01):
    """Get arbitrage opportunities for a symbol"""
    opportunities = await exchange_service.detect_arbitrage_opportunities(symbol, min_spread)
    
    return {
        "symbol": symbol,
        "opportunities": [opp.dict() for opp in opportunities],
        "timestamp": datetime.now().isoformat()
    }

@app.post("/stream/start")
async def start_streaming(background_tasks: BackgroundTasks, symbols: List[str] = ["ETH/USDT", "ETH/BTC"], data_types: List[str] = ["ticker", "orderbook", "trades"]):
    """Start WebSocket streaming for real-time data"""
    data_types_enum = [DataType(dt) for dt in data_types]
    background_tasks.add_task(exchange_service.stream_websocket_data, symbols, data_types_enum)
    
    return {
        "status": "streaming_started",
        "symbols": symbols,
        "data_types": data_types,
        "timestamp": datetime.now().isoformat()
    }

@app.get("/metrics")
async def get_metrics():
    """Get Prometheus metrics"""
    return {
        "data_collected": dict(data_collected_counter._metrics),
        "collection_duration": dict(collection_duration._metrics),
        "active_connections": active_connections._value.get(),
        "websocket_connections": websocket_connections._value.get()
    }

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8088,
        reload=True,
        log_level="info"
    )
