package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.service.MovieService;
import ch.uzh.ifi.hase.soprafs25.service.TMDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * MovieControllerServiceIntegrationTest
 * This is a WebMvc integration test for testing the integration between MovieController and MovieService.
 * Unlike unit tests, these tests involve multiple components working together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Using a test profile to ensure we don't affect production database
@Transactional // Each test gets its own transaction that is rolled back at the end
public class MovieControllerServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieService movieService;

    @MockBean
    private TMDbService tmdbService; // Mock the TMDb service to avoid external API calls

    private User testUser;
    private List<Movie> testMovies;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        // Set up test data - create a user with preferences and some test movies
        objectMapper = new ObjectMapper();

        // Clean up from previous tests
        movieRepository.deleteAll();
        userRepository.deleteAll();

        // Create test movies
        testMovies = createTestMovies();

        // Save movies to repository
        for (Movie movie : testMovies) {
            movieRepository.save(movie);
        }

        // Create test user with preferences
        testUser = createTestUser();
        userRepository.save(testUser);

        // Set up TMDb mock for suggestion generation
        when(tmdbService.searchMovies(any(Movie.class)))
                .thenReturn(testMovies);
    }

    /**
     * Test 5.1 - Test the entire request-response cycle from controller to service and back
     * This test verifies that a request to the controller correctly flows through to the service
     * and back to the client with the expected response.
     */
    @Test
    public void testGetMovieSuggestions_FullCycle() throws Exception {
        // Test the full request-response cycle with valid user ID

        // Given a valid user in the database

        // When making a request to the suggestions endpoint
        MvcResult result = mockMvc.perform(get("/movies/suggestions/{userId}", testUser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then the response contains movie suggestions
        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("\"movieId\":"));
        assertTrue(responseContent.contains("\"title\":"));

        // Verify that the service was called with the correct user ID
        verify(tmdbService, atLeastOnce()).searchMovies(any(Movie.class));
    }

    /**
     * Test 5.2 - Verify proper DTO conversion and response status codes
     * This test ensures that movie entities are correctly converted to DTOs and
     * the appropriate status code (200 OK) is returned for a successful request.
     */
    @Test
    public void testGetMovieSuggestions_DTOConversionAndStatusCode() throws Exception {
        // Test the conversion from Movie entities to MovieGetDTOs

        // When making a request to the suggestions endpoint
        mockMvc.perform(get("/movies/suggestions/{userId}", testUser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].movieId", notNullValue()))
                .andExpect(jsonPath("$[0].title", notNullValue()))
                .andExpect(jsonPath("$[0].posterURL", notNullValue()));

        // A successful request should return HTTP 200 OK, which is checked above
    }

    /**
     * Test 5.3 - Test with various valid and invalid path parameters
     * This test checks how the controller-service integration handles different types
     * of user IDs, including invalid ones.
     */
    @Test
    public void testGetMovieSuggestions_ValidAndInvalidParameters() throws Exception {
        // Test valid user ID, non-existent user ID, and invalid user ID format

        // Test with valid user ID
        mockMvc.perform(get("/movies/suggestions/{userId}", testUser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Test with non-existent user ID
        Long nonExistentUserId = 999999L;
        mockMvc.perform(get("/movies/suggestions/{userId}", nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Test with invalid user ID format (not a number)
        mockMvc.perform(get("/movies/suggestions/invalid-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Helper method to create test movies
     */
    private List<Movie> createTestMovies() {
        List<Movie> movies = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Movie movie = new Movie();
            movie.setMovieId(i);
            movie.setTitle("Test Movie " + i);
            movie.setPosterURL("https://example.com/poster" + i + ".jpg");

            List<String> genres = new ArrayList<>();
            if (i % 3 == 0) genres.add("Action");
            if (i % 4 == 0) genres.add("Science Fiction");
            if (i % 5 == 0) genres.add("Adventure");
            movie.setGenres(genres);

            movies.add(movie);
        }

        return movies;
    }

    /**
     * Helper method to create a test user with preferences
     */
    private User createTestUser() {
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("test.user@gmail.com");
        user.setPassword("password123");
        user.setBio("Test user bio");

        // Set favorite genres
        List<String> favoriteGenres = new ArrayList<>();
        favoriteGenres.add("Action");
        favoriteGenres.add("Science Fiction");
        favoriteGenres.add("Adventure");
        user.setFavoriteGenres(favoriteGenres);

        // Set favorite actors
        Map<String, String> favoriteActors = new HashMap<>();
        favoriteActors.put("6193", "Leonardo DiCaprio");
        favoriteActors.put("24045", "Joseph Gordon-Levitt");
        favoriteActors.put("1357546", "Ken Watanabe");
        favoriteActors.put("2524", "Tom Hardy");
        favoriteActors.put("27578", "Elliot Page");
        user.setFavoriteActors(favoriteActors);

        // Set favorite directors
        Map<String, String> favoriteDirectors = new HashMap<>();
        favoriteDirectors.put("525", "Christopher Nolan");
        favoriteDirectors.put("1408530", "Emma Thomas");
        user.setFavoriteDirectors(favoriteDirectors);

        // Set watchlist and watched movies
        Movie watchlistMovie1 = new Movie();
        watchlistMovie1.setMovieId(101);
        watchlistMovie1.setTitle("Watchlist Movie 1");
        movieRepository.save(watchlistMovie1);

        Movie watchlistMovie2 = new Movie();
        watchlistMovie2.setMovieId(102);
        watchlistMovie2.setTitle("Watchlist Movie 2");
        movieRepository.save(watchlistMovie2);

        Movie watchedMovie = new Movie();
        watchedMovie.setMovieId(103);
        watchedMovie.setTitle("Watched Movie");
        movieRepository.save(watchedMovie);

        user.setWatchlist(Arrays.asList(watchlistMovie1, watchlistMovie2));
        user.setWatchedMovies(Collections.singletonList(watchedMovie));

        return user;
    }
}
