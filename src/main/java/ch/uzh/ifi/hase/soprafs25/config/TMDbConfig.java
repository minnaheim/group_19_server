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
        String envToken = System.getenv("TMDB_API_TOKEN");
        logger.info("Checking for TMDB_API_TOKEN environment variable...");

        if (envToken != null && !envToken.isBlank()) {
            logger.info("TMDB_API_TOKEN is available as an environment variable.");
            // Use the environment variable value directly
            apiKey = envToken;
        } else {
            logger.info("TMDB_API_TOKEN is not set as an environment variable. Using local properties.");
            logger.debug("Current apiKey from @Value injection: '{}'", apiKey);
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


    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}