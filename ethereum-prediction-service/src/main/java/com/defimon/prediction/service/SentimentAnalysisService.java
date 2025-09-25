package com.defimon.prediction.service;

import com.defimon.prediction.model.SentimentData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sentiment analysis from various sources
 * 
 * Collects sentiment data from:
 * - Social media platforms (Twitter, Reddit)
 * - News sources
 * - Community forums
 * - Influencer opinions
 */
@Service
public class SentimentAnalysisService {

    private final WebClient webClient;
    private final String twitterApiKey;
    private final String redditApiKey;
    private final String newsApiKey;

    public SentimentAnalysisService(WebClient.Builder webClientBuilder,
                                  @Value("${twitter.api.key:}") String twitterApiKey,
                                  @Value("${reddit.api.key:}") String redditApiKey,
                                  @Value("${news.api.key:}") String newsApiKey) {
        this.webClient = webClientBuilder.build();
        this.twitterApiKey = twitterApiKey;
        this.redditApiKey = redditApiKey;
        this.newsApiKey = newsApiKey;
    }

    /**
     * Analyze overall sentiment for Ethereum
     */
    public SentimentData analyzeSentiment() {
        try {
            // Collect sentiment from multiple sources
            BigDecimal twitterSentiment = analyzeTwitterSentiment();
            BigDecimal redditSentiment = analyzeRedditSentiment();
            BigDecimal newsSentiment = analyzeNewsSentiment();
            Map<String, BigDecimal> sentimentBreakdown = new HashMap<>();
            
            sentimentBreakdown.put("twitter", twitterSentiment);
            sentimentBreakdown.put("reddit", redditSentiment);
            sentimentBreakdown.put("news", newsSentiment);

            // Calculate weighted overall sentiment
            BigDecimal overallSentiment = calculateOverallSentiment(sentimentBreakdown);

            // Collect engagement metrics
            Map<String, Long> engagementMetrics = collectEngagementMetrics();

            // Influencer sentiment
            Map<String, BigDecimal> influencerSentiment = analyzeInfluencerSentiment();

            return new SentimentData(
                LocalDateTime.now(),
                "multi_source",
                overallSentiment,
                sentimentBreakdown,
                calculateMentionCount(),
                engagementMetrics,
                influencerSentiment,
                newsSentiment,
                calculateSocialMediaSentiment(twitterSentiment, redditSentiment)
            );

        } catch (Exception e) {
            // Return neutral sentiment if analysis fails
            return createNeutralSentimentData();
        }
    }

    /**
     * Analyze sentiment from Twitter
     */
    public BigDecimal analyzeTwitterSentiment() {
        try {
            // This would integrate with Twitter API v2
            // For now, return a mock calculation
            return calculateMockSentiment("twitter");
        } catch (Exception e) {
            return BigDecimal.ZERO; // Neutral sentiment
        }
    }

    /**
     * Analyze sentiment from Reddit
     */
    public BigDecimal analyzeRedditSentiment() {
        try {
            // This would integrate with Reddit API
            // For now, return a mock calculation
            return calculateMockSentiment("reddit");
        } catch (Exception e) {
            return BigDecimal.ZERO; // Neutral sentiment
        }
    }

    /**
     * Analyze sentiment from news sources
     */
    public BigDecimal analyzeNewsSentiment() {
        try {
            // This would integrate with NewsAPI or similar
            // For now, return a mock calculation
            return calculateMockSentiment("news");
        } catch (Exception e) {
            return BigDecimal.ZERO; // Neutral sentiment
        }
    }

    /**
     * Analyze sentiment from crypto influencers
     */
    public Map<String, BigDecimal> analyzeInfluencerSentiment() {
        Map<String, BigDecimal> influencerSentiment = new HashMap<>();
        
        // Mock influencer sentiment data
        influencerSentiment.put("vitalik_buterin", new BigDecimal("0.7"));
        influencerSentiment.put("naval", new BigDecimal("0.6"));
        influencerSentiment.put("balajis", new BigDecimal("0.8"));
        influencerSentiment.put("elon_musk", new BigDecimal("0.3"));
        
        return influencerSentiment;
    }

    /**
     * Collect engagement metrics
     */
    public Map<String, Long> collectEngagementMetrics() {
        Map<String, Long> engagementMetrics = new HashMap<>();
        
        // Mock engagement data
        engagementMetrics.put("twitter_mentions", 15420L);
        engagementMetrics.put("reddit_posts", 342L);
        engagementMetrics.put("reddit_comments", 2847L);
        engagementMetrics.put("news_articles", 156L);
        engagementMetrics.put("youtube_videos", 23L);
        
        return engagementMetrics;
    }

    /**
     * Calculate total mention count across platforms
     */
    public Long calculateMentionCount() {
        Map<String, Long> metrics = collectEngagementMetrics();
        return metrics.values().stream().mapToLong(Long::longValue).sum();
    }

    private BigDecimal calculateOverallSentiment(Map<String, BigDecimal> sentimentBreakdown) {
        if (sentimentBreakdown.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Weighted average sentiment
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;

        // Twitter has higher weight due to crypto community activity
        BigDecimal twitterWeight = new BigDecimal("0.4");
        BigDecimal redditWeight = new BigDecimal("0.3");
        BigDecimal newsWeight = new BigDecimal("0.3");

        if (sentimentBreakdown.containsKey("twitter")) {
            weightedSum = weightedSum.add(sentimentBreakdown.get("twitter").multiply(twitterWeight));
            totalWeight = totalWeight.add(twitterWeight);
        }

        if (sentimentBreakdown.containsKey("reddit")) {
            weightedSum = weightedSum.add(sentimentBreakdown.get("reddit").multiply(redditWeight));
            totalWeight = totalWeight.add(redditWeight);
        }

        if (sentimentBreakdown.containsKey("news")) {
            weightedSum = weightedSum.add(sentimentBreakdown.get("news").multiply(newsWeight));
            totalWeight = totalWeight.add(newsWeight);
        }

        return totalWeight.compareTo(BigDecimal.ZERO) > 0 
            ? weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    }

    private BigDecimal calculateSocialMediaSentiment(BigDecimal twitterSentiment, BigDecimal redditSentiment) {
        return twitterSentiment.add(redditSentiment).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMockSentiment(String source) {
        // Mock sentiment calculation based on source
        // In real implementation, this would use actual API calls and NLP analysis
        return switch (source) {
            case "twitter" -> new BigDecimal("0.65"); // Slightly positive
            case "reddit" -> new BigDecimal("0.55"); // Neutral-positive
            case "news" -> new BigDecimal("0.45"); // Slightly negative
            default -> BigDecimal.ZERO;
        };
    }

    private SentimentData createNeutralSentimentData() {
        Map<String, BigDecimal> neutralBreakdown = new HashMap<>();
        neutralBreakdown.put("twitter", BigDecimal.ZERO);
        neutralBreakdown.put("reddit", BigDecimal.ZERO);
        neutralBreakdown.put("news", BigDecimal.ZERO);

        Map<String, Long> emptyEngagement = new HashMap<>();
        Map<String, BigDecimal> emptyInfluencer = new HashMap<>();

        return new SentimentData(
            LocalDateTime.now(),
            "fallback",
            BigDecimal.ZERO,
            neutralBreakdown,
            0L,
            emptyEngagement,
            emptyInfluencer,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
    }
}
