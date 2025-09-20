use anyhow::Result;
use mongodb::{bson::doc, Client, Collection, Database};
use chrono::Utc;

use crate::models::*;

#[derive(Clone)]
pub struct MongoDBService {
    database: Database,
}

impl MongoDBService {
    pub async fn new(uri: &str) -> Result<Self> {
        let client = Client::with_uri_str(uri).await?;
        let database = client.database("chaindata");
        
        Ok(Self { database })
    }

    pub async fn store_price_data(&self, price_data: &PriceDataPoint) -> Result<()> {
        let collection: Collection<PriceDataPoint> = self.database.collection("price_data");
        collection.insert_one(price_data, None).await?;
        Ok(())
    }

    pub async fn store_ohlcv_data(&self, ohlcv_data: &[OHLCVData]) -> Result<()> {
        let collection: Collection<OHLCVData> = self.database.collection("ohlcv_data");
        collection.insert_many(ohlcv_data, None).await?;
        Ok(())
    }

    pub async fn store_aggregated_data(&self, aggregated_data: &AggregatedData) -> Result<()> {
        let collection: Collection<AggregatedData> = self.database.collection("aggregated_data");
        collection.insert_one(aggregated_data, None).await?;
        Ok(())
    }

    pub async fn get_price_history(&self, symbol: &str, timeframe: &str, limit: usize) -> Result<Vec<PriceDataPoint>> {
        let collection: Collection<PriceDataPoint> = self.database.collection("price_data");
        
        let filter = doc! {
            "symbol": symbol
        };

        let options = mongodb::options::FindOptions::builder()
            .sort(doc! { "timestamp": -1 })
            .limit(limit as i64)
            .build();

        let mut cursor = collection.find(filter, Some(options)).await?;
        let mut price_data = Vec::new();

        while let Some(data) = cursor.next().await {
            price_data.push(data?);
        }

        price_data.reverse(); // Return in chronological order
        Ok(price_data)
    }

    pub async fn get_ohlcv_data(&self, symbol: &str, timeframe: &str, limit: usize) -> Result<Vec<OHLCVData>> {
        let collection: Collection<OHLCVData> = self.database.collection("ohlcv_data");
        
        let filter = doc! {
            "symbol": symbol,
            "timeframe": timeframe
        };

        let options = mongodb::options::FindOptions::builder()
            .sort(doc! { "timestamp": -1 })
            .limit(limit as i64)
            .build();

        let mut cursor = collection.find(filter, Some(options)).await?;
        let mut ohlcv_data = Vec::new();

        while let Some(data) = cursor.next().await {
            ohlcv_data.push(data?);
        }

        ohlcv_data.reverse(); // Return in chronological order
        Ok(ohlcv_data)
    }

    pub async fn get_volume_data(&self, symbol: &str, hours: u64) -> Result<Vec<PriceDataPoint>> {
        let collection: Collection<PriceDataPoint> = self.database.collection("price_data");
        
        let cutoff_time = Utc::now().timestamp() - (hours as i64 * 3600);
        let filter = doc! {
            "symbol": symbol,
            "timestamp": { "$gte": cutoff_time }
        };

        let options = mongodb::options::FindOptions::builder()
            .sort(doc! { "timestamp": 1 })
            .build();

        let mut cursor = collection.find(filter, Some(options)).await?;
        let mut volume_data = Vec::new();

        while let Some(data) = cursor.next().await {
            volume_data.push(data?);
        }

        Ok(volume_data)
    }

    pub async fn get_latest_prices(&self, symbols: &[String]) -> Result<Vec<PriceDataPoint>> {
        let collection: Collection<PriceDataPoint> = self.database.collection("price_data");
        
        let mut latest_prices = Vec::new();
        
        for symbol in symbols {
            let filter = doc! { "symbol": symbol };
            let options = mongodb::options::FindOptions::builder()
                .sort(doc! { "timestamp": -1 })
                .limit(1)
                .build();

            if let Some(price_data) = collection.find_one(filter, Some(options)).await? {
                latest_prices.push(price_data);
            }
        }

        Ok(latest_prices)
    }

    pub async fn get_aggregated_data(&self, symbol: &str, data_type: &str, timeframe: &str, limit: usize) -> Result<Vec<AggregatedData>> {
        let collection: Collection<AggregatedData> = self.database.collection("aggregated_data");
        
        let filter = doc! {
            "symbol": symbol,
            "data_type": data_type,
            "timeframe": timeframe
        };

        let options = mongodb::options::FindOptions::builder()
            .sort(doc! { "timestamp": -1 })
            .limit(limit as i64)
            .build();

        let mut cursor = collection.find(filter, Some(options)).await?;
        let mut aggregated_data = Vec::new();

        while let Some(data) = cursor.next().await {
            aggregated_data.push(data?);
        }

        aggregated_data.reverse(); // Return in chronological order
        Ok(aggregated_data)
    }

    pub async fn update_market_summary(&self, summary: &MarketSummaryResponse) -> Result<()> {
        let collection: Collection<serde_json::Value> = self.database.collection("market_summary");
        
        let summary_doc = serde_json::to_value(summary)?;
        let filter = doc! { "_id": "latest" };
        let update = doc! { "$set": summary_doc };
        
        let options = mongodb::options::UpdateOptions::builder()
            .upsert(true)
            .build();

        collection.update_one(filter, update, Some(options)).await?;
        Ok(())
    }

    pub async fn get_market_summary(&self) -> Result<Option<MarketSummaryResponse>> {
        let collection: Collection<serde_json::Value> = self.database.collection("market_summary");
        
        let filter = doc! { "_id": "latest" };
        
        if let Some(summary_doc) = collection.find_one(filter, None).await? {
            let summary: MarketSummaryResponse = serde_json::from_value(summary_doc)?;
            Ok(Some(summary))
        } else {
            Ok(None)
        }
    }
}
