package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.User; 
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList; 
import java.util.Arrays; 
import java.util.Collections; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch; 
import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors; 
import java.util.concurrent.TimeUnit; 
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TMDbIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MovieService movieService;

    @Autowired
    private TMDbService tmdbService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    private User testUser;
    private List<Movie> testWatchlistMovies;
    private List<Movie> testWatchedMovies;

    @BeforeEach
    void setup() throws JsonProcessingException {
        movieRepository.deleteAll();
        userRepository.deleteAll();

        testWatchlistMovies = createTestWatchlistMovies();
        testWatchedMovies = createTestWatchedMovies();

        testUser = createTestUser();

        testWatchlistMovies.forEach(movie -> movieRepository.save(movie));
        testWatchedMovies.forEach(movie -> movieRepository.save(movie));
        userRepository.save(testUser);
    }

    @AfterEach
    void teardown() {
        userRepository.deleteAll();
        movieRepository.deleteAll();
    }

    /**
     * Test 7.1 - Test the interaction between MovieService and real TMDbService
     * This test verifies that the MovieService can successfully interact with the
     * real TMDbService
     * to retrieve movie suggestions based on user favorites.
     */
    // @Test
    // public void testRealTMDbServiceInteraction() {
    // // Testing real integration between MovieService and TMDbService
    // // Get movie suggestions with the real TMDbService
    // List<Movie> suggestions =
    // movieService.getMovieSuggestions(testUser.getUserId(), 10);

    // // Verify we got results
    // assertNotNull(suggestions);
    // assertFalse(suggestions.isEmpty());

    // // Verify basic properties of returned movies
    // suggestions.forEach(movie -> {
    // assertNotNull(movie.getTitle());
    // assertTrue(movie.getMovieId() > 0);
    // // Movies should have at least some of the required properties
    // assertFalse(movie.getGenres() == null || movie.getGenres().isEmpty());
    // });

    // // Verify that movies from watchlist and watched are not included
    // Set<Long> watchlistAndWatchedIds = new HashSet<>();
    // testUser.getWatchlist().forEach(m ->
    // watchlistAndWatchedIds.add(m.getMovieId()));
    // testUser.getWatchedMovies().forEach(m ->
    // watchlistAndWatchedIds.add(m.getMovieId()));

    // for (Movie movie : suggestions) {
    // assertFalse(watchlistAndWatchedIds.contains(movie.getMovieId()),
    // "Suggestion contains a movie that is in watchlist or watched: " +
    // movie.getTitle());
    // }
    // }

    /**
     * Test 7.2 - Verify the service can handle real TMDb responses
     * This test checks that the service can properly parse and process the
     * responses from the real TMDb API.
     */
    @Test
    public void testHandlingRealTMDbResponses() {
        // Testing the processing of real TMDb responses
        // Create search parameters based on user favorites
        Movie searchParams = new Movie();

        // Set some specific parameters from user favorites
        if (!testUser.getFavoriteGenres().isEmpty()) {
            searchParams.setGenres(testUser.getFavoriteGenres());
        }

        // Get a list of actor IDs from the user's favorite actors map
        String actorsJson = testUser.getFavoriteActorsJson();
        if (actorsJson != null && !actorsJson.isEmpty()) {
            try {
                List<ActorDTO> actorDTOs = objectMapper.readValue(actorsJson, new TypeReference<List<ActorDTO>>() {});
                if (actorDTOs != null && !actorDTOs.isEmpty()) {
                    searchParams.setActors(actorDTOs.stream().map(ActorDTO::getId).map(String::valueOf).collect(Collectors.toList()));
                }
            } catch (JsonProcessingException e) {
                fail("Error parsing favorite actors JSON: " + e.getMessage());
            }
        }

        // Get a list of director IDs from the user's favorite directors map
        String directorsJson = testUser.getFavoriteDirectorsJson();
        if (directorsJson != null && !directorsJson.isEmpty()) {
            try {
                List<DirectorDTO> directorDTOs = objectMapper.readValue(directorsJson, new TypeReference<List<DirectorDTO>>() {});
                if (directorDTOs != null && !directorDTOs.isEmpty()) {
                    searchParams.setDirectors(directorDTOs.stream().map(DirectorDTO::getId).map(String::valueOf).collect(Collectors.toList()));
                }
            } catch (JsonProcessingException e) {
                fail("Error parsing favorite directors JSON: " + e.getMessage());
            }
        }

        // Get movies directly from TMDbService
        List<Movie> movies = tmdbService.searchMovies(searchParams);

        // Verify response handling
        assertNotNull(movies);

        // If we got results, verify they contain expected data
        if (!movies.isEmpty()) {
            for (Movie movie : movies) {
                assertNotNull(movie.getTitle());
                assertTrue(movie.getMovieId() > 0);
                assertNotNull(movie.getPosterURL());

                // These might be null depending on the API response, but if present, verify
                // correctness
                if (movie.getYear() != null) {
                    assertTrue(movie.getYear() > 1888); // First movie ever made
                }
            }
        }
    }

    /**
     * Test 7.3 - Test error handling for API failures
     * This test verifies that the service can properly handle various types of API
     * failures
     * including network errors, rate limiting, and invalid requests.
     */
    @Test
    public void testErrorHandlingForAPIFailures() {
        // Testing error handling of API failures

        // Create a test case with invalid parameters that should cause an API error
        Movie invalidSearchParams = new Movie();
        invalidSearchParams.setYear(-1000); // Clearly invalid year

        try {
            // This should fail but be handled gracefully
            List<Movie> results = tmdbService.searchMovies(invalidSearchParams);

            // If we get here, the API might have handled the invalid parameter differently
            // than expected
            // but we should still have a valid (possibly empty) result
            assertNotNull(results);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // These are expected exceptions, but our service should generally handle them
            // internally
            // If we see them here, it means the service is propagating errors to the caller
            fail("TMDbService should handle API errors internally but threw: " + e.getMessage());
        } catch (Exception e) {
            // Other exceptions are fine as long as they're properly handled
            // This test is primarily to ensure the application doesn't crash
            assertNotNull(e.getMessage());
        }

        // Test that the movie service handles TMDb failures gracefully
        try {
            // Even with potential errors, the movie service should still return some
            // results
            // from alternative search permutations or just return an empty list
            List<Movie> suggestions = movieService.getMovieSuggestions(testUser.getUserId(), 10);
            assertNotNull(suggestions); // Should never be null, even if empty
        } catch (ResponseStatusException e) {
            // This is acceptable - service might throw a controlled exception
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
        }
    }

    /**
     * Test 7.4 - Testing pagination behavior
     * This test verifies that the service correctly handles TMDb pagination to
     * retrieve
     * all available movies for a search query.
     */
    // @Test
    // public void testPaginationBehavior() {
    // // Testing pagination support for API calls

    // // Choose broad search parameters that would typically return many results
    // Movie searchParams = new Movie();
    // searchParams.setGenres(Arrays.asList("Action", "Adventure")); // Popular
    // genres

    // // Request more movies than would fit on a single page (TMDb typically uses
    // 20 per page)
    // int requestedCount = 45; // Should require at least 3 pages

    // Get a specific user's favorites to simulate the movie suggestion flow
    // List<Movie> results = movieService.getMovieSuggestions(testUser.getUserId(),
    // requestedCount);

    // // Verify we got multiple pages worth of results
    // // We may not get exactly the requested count, but should get more than a
    // single page
    // assertNotNull(results);
    // assertTrue(results.size() > 20,
    // "Expected more than 20 results to verify pagination, got: " +
    // results.size());

    // // If we requested many movies but got few, pagination might not be working
    // correctly
    // if (requestedCount > 20 && results.size() < 20) {
    // fail("Pagination may not be working correctly. Requested " + requestedCount +
    // " movies but got only " + results.size());
    // }

    // // Verify that results don't contain duplicates (pagination should handle
    // this)
    // Set<Long> movieIds = new HashSet<>();
    // for (Movie movie : results) {
    // assertFalse(movieIds.contains(movie.getMovieId()),
    // "Duplicate movie found: " + movie.getTitle());
    // movieIds.add(movie.getMovieId());
    // }
    // }

    /**
     * Test 7.5 - Testing rate limiting
     * This test verifies that the service can handle TMDb rate limiting constraints
     * when making many requests in rapid succession.
     */
    @Test
    public void testRateLimitingHandling() {
        // Testing proper handling of API rate limiting

        // Number of concurrent requests to simulate
        int concurrentRequests = 5;
        // Create a countdown latch to synchronize the threads
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        // Create a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);

        // List to collect exceptions from the threads
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Submit concurrent tasks
        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    // Make a search request that might trigger rate limiting
                    List<Movie> results = movieService.getMovieSuggestions(testUser.getUserId(), 50);
                    assertNotNull(results);
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // Wait for all threads to complete or timeout after 30 seconds
            boolean completed = latch.await(30, TimeUnit.SECONDS);

            if (!completed) {
                fail("Concurrent requests did not complete within the timeout");
            }

            // Verify that no unexpected exceptions occurred
            if (!exceptions.isEmpty()) {
                // Some exceptions are expected with rate limiting, but they should be handled
                boolean hasUnexpectedExceptions = exceptions.stream()
                        .anyMatch(e -> !(e instanceof ResponseStatusException) &&
                                !(e instanceof ResourceAccessException) &&
                                !(e instanceof HttpClientErrorException));

                if (hasUnexpectedExceptions) {
                    fail("Unexpected exceptions occurred: " +
                            exceptions.stream()
                                    .map(Exception::getMessage)
                                    .collect(Collectors.joining(", ")));
                }
            }
        } catch (InterruptedException e) {
            fail("Test was interrupted while waiting for concurrent requests");
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Helper method to create a test user with favorites
     */
    private User createTestUser() throws JsonProcessingException {
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("test.user@gmail.com");
        user.setPassword("dskjföaldskj^^32142");
        user.setBio("what kind of string is bio?");
        user.setStatus(UserStatus.ONLINE);

        // Set favorite genres
        user.setFavoriteGenres(Arrays.asList("Action", "Science Fiction", "Adventure"));

        // Set favorite actors
        Map<String, String> favoriteActors = new HashMap<>();
        favoriteActors.put("6193", "Leonardo DiCaprio");
        favoriteActors.put("24045", "Joseph Gordon-Levitt");
        favoriteActors.put("1357546", "Ken Watanabe");
        favoriteActors.put("2524", "Tom Hardy");
        favoriteActors.put("27578", "Elliot Page");
        List<ActorDTO> actorDTOs = favoriteActors.entrySet().stream()
            .map(entry -> {
                ActorDTO dto = new ActorDTO();
                try { dto.setId(Integer.parseInt(entry.getKey())); } catch (NumberFormatException e) { /* ID might be optional or set later */ }
                dto.setName(entry.getValue());
                return dto;
            }).collect(Collectors.toList());
        user.setFavoriteActorsJson(objectMapper.writeValueAsString(actorDTOs));

        // Set favorite directors
        Map<String, String> favoriteDirectors = new HashMap<>();
        favoriteDirectors.put("525", "Christopher Nolan");
        favoriteDirectors.put("1408530", "Emma Thomas");
        List<DirectorDTO> directorDTOs = favoriteDirectors.entrySet().stream()
            .map(entry -> {
                DirectorDTO dto = new DirectorDTO();
                try { dto.setId(Integer.parseInt(entry.getKey())); } catch (NumberFormatException e) { /* ID might be optional or set later */ }
                dto.setName(entry.getValue());
                return dto;
            }).collect(Collectors.toList());
        user.setFavoriteDirectorsJson(objectMapper.writeValueAsString(directorDTOs));

        // Set watchlist and watched movies
        user.setWatchlist(testWatchlistMovies);
        user.setWatchedMovies(testWatchedMovies);

        return user;
    }

    /**
     * Helper method to create test watchlist movies
     */
    private List<Movie> createTestWatchlistMovies() {
        List<Movie> watchlistMovies = new ArrayList<>();

        // Create movie 1 (Inception)
        Movie movie1 = new Movie();
        movie1.setMovieId(27205); // Inception's TMDb ID
        movie1.setTitle("Inception");
        movie1.setGenres(Arrays.asList("Action", "Science Fiction", "Adventure"));
        movie1.setYear(2010);
        movie1.setPosterURL("https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg");
        watchlistMovies.add(movie1);

        // Create movie 2 (The Dark Knight)
        Movie movie2 = new Movie();
        movie2.setMovieId(155); // The Dark Knight's TMDb ID
        movie2.setTitle("The Dark Knight");
        movie2.setGenres(Arrays.asList("Drama", "Action", "Crime", "Thriller"));
        movie2.setYear(2008);
        movie2.setPosterURL("https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg");
        watchlistMovies.add(movie2);

        return watchlistMovies;
    }

    /**
     * Helper method to create test watched movies
     */
    private List<Movie> createTestWatchedMovies() {
        List<Movie> watchedMovies = new ArrayList<>();

        // Create watched movie (Interstellar)
        Movie movie = new Movie();
        movie.setMovieId(157336); // Interstellar's TMDb ID
        movie.setTitle("Interstellar");
        movie.setGenres(Arrays.asList("Adventure", "Drama", "Science Fiction"));
        movie.setYear(2014);
        movie.setPosterURL("https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg");
        watchedMovies.add(movie);

        return watchedMovies;
    }
}