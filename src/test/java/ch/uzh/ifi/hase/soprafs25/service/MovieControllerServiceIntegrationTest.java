package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MovieControllerServiceIntegrationTest
 * This is a WebMvc integration test for testing the integration between
 * MovieController and MovieService.
 * Unlike unit tests, these tests involve multiple components working together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Each test gets its own transaction that is rolled back at the end
public class MovieControllerServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    private User testUser;
    private List<Movie> testMovies;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() throws Exception {
        // Set up test data - create a user with favorites and some test movies
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

        // Create test user with favorites
        testUser = createTestUser();
        userRepository.save(testUser);
    }

    /**
     * Test 5.1 - Test the entire request-response cycle from controller to service
     * and back
     * This test verifies that a request to the controller correctly flows through
     * to the service
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
     * This test checks how the controller-service integration handles different
     * types
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
            if (i % 3 == 0)
                genres.add("Action");
            if (i % 4 == 0)
                genres.add("Science Fiction");
            if (i % 5 == 0)
                genres.add("Adventure");
            movie.setGenres(genres);

            movies.add(movie);
        }

        return movies;
    }

    /**
     * Helper method to create a test user with favorites
     */
    private User createTestUser() throws Exception {
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
        List<ActorDTO> actorDTOs = new ArrayList<>();
        for (Map.Entry<String, String> entry : favoriteActors.entrySet()) {
            ActorDTO actor = new ActorDTO();
            actor.setId(Integer.parseInt(entry.getKey()));
            actor.setName(entry.getValue());
            actorDTOs.add(actor);
        }
        user.setFavoriteActorsJson(objectMapper.writeValueAsString(actorDTOs));

        // Set favorite directors
        Map<String, String> favoriteDirectors = new HashMap<>();
        favoriteDirectors.put("525", "Christopher Nolan");
        favoriteDirectors.put("1408530", "Emma Thomas");
        List<DirectorDTO> directorDTOs = new ArrayList<>();
        for (Map.Entry<String, String> entry : favoriteDirectors.entrySet()) {
            DirectorDTO director = new DirectorDTO();
            director.setId(Integer.parseInt(entry.getKey()));
            director.setName(entry.getValue());
            directorDTOs.add(director);
        }
        user.setFavoriteDirectorsJson(objectMapper.writeValueAsString(directorDTOs));

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
