package ch.uzh.ifi.hase.soprafs25.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@PropertySource(value = "file:./local.properties", ignoreResourceNotFound = true)
public class TMDbConfig {

    private final Logger log = LoggerFactory.getLogger(TMDbConfig.class);

    @Value("${TMDB_API_TOKEN:${tmdb.api.key:}}")
    private String apiKey;

    @Value("${tmdb.api.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    public String getApiKey(){
        log.info("TMDb API Key length: {}", apiKey != null ? apiKey.length() : 0);
        log.info("First few characters of API key: {}", apiKey != null && apiKey.length() > 5 ? apiKey.substring(0, 5) : "none");
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
