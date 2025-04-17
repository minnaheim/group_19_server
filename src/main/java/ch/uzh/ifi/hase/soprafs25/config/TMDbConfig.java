package ch.uzh.ifi.hase.soprafs25.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "file:./local.properties", ignoreResourceNotFound = true)
public class TMDbConfig {

    @Value("${TMDB_API_TOKEN:${tmdb.api.key:}}")
    //    @Value("${tmdb.api.key:}")
    private String apiKey;

    @Value("${tmdb.api.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
