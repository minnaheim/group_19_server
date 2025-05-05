package ch.uzh.ifi.hase.soprafs25.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "file:./local.properties", ignoreResourceNotFound = true)
public class TMDbConfig implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(TMDbConfig.class);

    @Value("${TMDB_API_TOKEN:${tmdb.api.key:}}")
    private String apiKey;

    @Value("${tmdb.api.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    @Override
    public void run(String... args) {
        String envToken = getEnvironmentVariable("TMDB_API_TOKEN");
        logger.info("Checking for TMDB_API_TOKEN environment variable...");

        if (envToken != null) {
            logger.info("TMDB_API_TOKEN environment variable is present with length: {}", envToken.length());
            if (envToken.isBlank()) {
                logger.warn("TMDB_API_TOKEN is present but is blank");
            } else {
                logger.info("TMDB_API_TOKEN is available as an environment variable.");
                // Use the environment variable value directly
                apiKey = envToken;
            }
        } else {
            logger.info("TMDB_API_TOKEN environment variable is null");
            logger.info("Using local properties, apiKey length: {}",
                    (apiKey != null) ? apiKey.length() : 0);
        }

        if (apiKey == null || apiKey.isBlank()) {
            logger.error("TMDb API key is not configured! Neither environment variable nor local property is available.");
        } else {
            // Don't log the full key in production, just confirm it's set
            logger.info("TMDb API key is configured successfully. Key begins with: {}",
                    apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "<too short>");
        }

        logger.info("TMDb Base URL is set to: {}", baseUrl);
    }

    // Protected method for testing
    protected String getEnvironmentVariable(String name) {
        return System.getenv(name);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}