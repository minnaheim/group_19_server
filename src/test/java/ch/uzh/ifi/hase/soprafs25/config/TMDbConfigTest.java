package ch.uzh.ifi.hase.soprafs25.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TMDbConfigTest {

    private Logger mockLogger;

    @Spy
    private TMDbConfig tmdbConfig;

    @BeforeEach
    void setUp() {
        mockLogger = mock(Logger.class);
        ReflectionTestUtils.setField(tmdbConfig, "logger", mockLogger);
    }


    @Test
    void testRunWhenEnvTokenIsNull() throws Exception {
        // Setup
        ReflectionTestUtils.setField(tmdbConfig, "apiKey", "test-api-key-from-properties");
        ReflectionTestUtils.setField(tmdbConfig, "baseUrl", "https://test-url.com");

        // Execute
        tmdbConfig.run();

        System.out.println("Testing environment token null scenario");
        System.out.println("API Key: " + ReflectionTestUtils.getField(tmdbConfig, "apiKey"));
        System.out.println("Base URL: " + ReflectionTestUtils.getField(tmdbConfig, "baseUrl"));


        // Verify
        verify(mockLogger).info("Checking for TMDB_API_TOKEN environment variable...");
        verify(mockLogger).info("TMDB_API_TOKEN environment variable is null");
        verify(mockLogger).info(eq("Using local properties, apiKey length: {}"),
                eq(28));
        verify(mockLogger).info(eq("TMDb API key is configured successfully. Key begins with: {}"),
                eq("test-api-k..."));
        verify(mockLogger).info(eq("TMDb Base URL is set to: {}"), eq("https://test-url.com"));
    }

    @Test
    void testRunWhenNoApiKeyIsAvailable() throws Exception {
        // Configure
        ReflectionTestUtils.setField(tmdbConfig, "logger", mockLogger);
        ReflectionTestUtils.setField(tmdbConfig, "apiKey", null);

        System.out.println("Testing no API key available scenario");
        System.out.println("API Key before run: " + ReflectionTestUtils.getField(tmdbConfig, "apiKey"));

        // Act
        tmdbConfig.run();

        // Assert
        verify(mockLogger).error("TMDb API key is not configured! Neither environment variable nor local property is available.");
    }

    @Test
    void testRunWhenApiKeyIsBlank() throws Exception {
        // Configure
        ReflectionTestUtils.setField(tmdbConfig, "logger", mockLogger);
        ReflectionTestUtils.setField(tmdbConfig, "apiKey", "   ");

        System.out.println("Testing blank API key scenario");
        System.out.println("API Key before run: '" + ReflectionTestUtils.getField(tmdbConfig, "apiKey") + "'");

        // Act
        tmdbConfig.run();

        // Assert
        verify(mockLogger).error("TMDb API key is not configured! Neither environment variable nor local property is available.");
    }
}