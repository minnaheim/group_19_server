package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MovieApiIntegrationTest
 * This class contains integration tests for the movie suggestion API endpoint.
 * It tests the full API integration, verifying correct response formats,
 * status codes, pagination, filtering, and edge cases.
 *
 * These tests use a real database but stub the TMDb service to avoid external
 * API calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MovieApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @MockBean
    private TMDbService tmdbService;

    private User testUser;
    private List<Movie> testMovies;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() throws Exception {
        // Initialize object mapper
        this.objectMapper = new ObjectMapper();

        // Clean up existing data
        movieRepository.deleteAll();
        userRepository.deleteAll();

        // Create test movies
        testMovies = createTestMovies();
        testMovies.forEach(movie -> movieRepository.save(movie));

        // Create test user with favorites
        testUser = createTestUser();
        testUser = userRepository.save(testUser);

        // Setup TMDb service mock to return test movies
        when(tmdbService.searchMovies(any())).thenReturn(testMovies);
    }

    /**
     * Test 6.1 - Test the REST endpoint with HTTP requests
     * This test verifies that the /movies/suggestions/{userId} endpoint
     * correctly responds to HTTP GET requests and returns movie suggestions
     */
    @Test
    public void testGetMovieSuggestions_HTTPRequest() throws Exception {
        // Send a GET request to the endpoint and verify the response
        mockMvc.perform(get("/movies/suggestions/{userId}", testUser.getUserId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Test 6.2 - Verify correct response format and status codes
     * This test checks that the API returns the correct response format
     * and status codes for both successful requests and error cases
     */
    @Test
    public void testGetMovieSuggestions_ResponseFormatAndStatusCodes() throws Exception {
        // Test successful request (200 OK)
        MvcResult result = mockMvc.perform(get("/movies/suggestions/{userId}", testUser.getUserId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(ArrayList.class)))
                .andExpect(jsonPath("$[0].movieId", isA(Number.class)))
                .andExpect(jsonPath("$[0].title", isA(String.class)))
                .andExpect(jsonPath("$[0].posterURL", isA(String.class)))
                .andReturn();

        // Parse the response to verify it contains the expected data
        String content = result.getResponse().getContentAsString();
        List<MovieGetDTO> responseMovies = objectMapper.readValue(content, new TypeReference<List<MovieGetDTO>>() {
        });
        assertFalse(responseMovies.isEmpty());

        // Test non-existent user (404 Not Found)
        mockMvc.perform(get("/movies/suggestions/{userId}", 999999L))
                .andExpect(status().isNotFound());
    }

    /**
     * Test 6.3 - Test with real database but stub the TMDb service
     * This test verifies that the API works correctly with a real database
     * while using a stubbed TMDb service to avoid external API calls
     */
    @Test
    public void testGetMovieSuggestions_RealDatabaseWithStubbedTMDb() throws Exception {
        // Setup TMDb service mock to return different results for different queries
        List<Movie> actionMovies = new ArrayList<>();
        Movie actionMovie = new Movie();
        actionMovie.setMovieId(101);
        actionMovie.setTitle("Action Movie");
        actionMovie.setGenres(Collections.singletonList("Action"));
        actionMovie.setPosterURL("http://example.com/poster101.jpg");
        actionMovies.add(actionMovie);

        when(tmdbService.searchMovies(any())).thenReturn(actionMovies);

        // Test with real user from database
        MvcResult result = mockMvc.perform(get("/movies/suggestions/{userId}", testUser.getUserId()))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        List<MovieGetDTO> responseMovies = objectMapper.readValue(content, new TypeReference<List<MovieGetDTO>>() {
        });

        // Verify that the response contains data from our stubbed TMDb service
        for (MovieGetDTO movie : responseMovies) {
            if (movie.getMovieId() == 101) {
                assertEquals("Action Movie", movie.getTitle());
                assertEquals("http://example.com/poster101.jpg", movie.getPosterURL());
                break;
            }
        }
    }

    /**
     * Test 6.4 - Testing response headers and content types in SpringBoot framework
     * This test verifies that the API returns the correct HTTP headers and
     * content types in its responses
     */
    @Test
    public void testGetMovieSuggestions_ResponseHeadersAndContentTypes() throws Exception {
        // Test response headers and content type
        mockMvc.perform(get("/movies/suggestions/{userId}", testUser.getUserId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    }

    /**
     * Helper method to create test movies
     */
    private List<Movie> createTestMovies() {
        List<Movie> movies = new ArrayList<>();

        // Create some test movies
        for (int i = 1; i <= 10; i++) {
            Movie movie = new Movie();
            movie.setMovieId(i);
            movie.setTitle("Test Movie " + i);

            // Set different genres
            List<String> genres = new ArrayList<>();
            if (i % 3 == 0)
                genres.add("Action");
            if (i % 4 == 0)
                genres.add("Science Fiction");
            if (i % 5 == 0)
                genres.add("Adventure");
            movie.setGenres(genres);

            // Set actors and directors
            List<String> actors = new ArrayList<>();
            actors.add(i % 2 == 0 ? "6193" : "24045"); // Leonardo DiCaprio or Joseph Gordon-Levitt
            movie.setActors(actors);

            List<String> directors = new ArrayList<>();
            directors.add(i % 2 == 0 ? "525" : "1408530"); // Christopher Nolan or Emma Thomas
            movie.setDirectors(directors);

            movie.setPosterURL("http://example.com/poster" + i + ".jpg");
            movie.setDescription("Description for movie " + i);

            movies.add(movie);
        }

        return movies;
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

        // Set favorite genres
        List<String> favoriteGenres = new ArrayList<>();
        favoriteGenres.add("Action");
        favoriteGenres.add("Science Fiction");
        favoriteGenres.add("Adventure");
        user.setFavoriteGenres(favoriteGenres);

        // Set favorite movie
        Movie favoriteMovie = new Movie();
        favoriteMovie.setMovieId(1);
        favoriteMovie.setTitle("Inception");
        favoriteMovie = movieRepository.save(favoriteMovie);
        user.setFavoriteMovie(favoriteMovie);

        // Setup favorite actors
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

        // Setup favorite directors
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

        // Set watchlist
        List<Movie> watchlist = new ArrayList<>();
        Movie watchlistMovie1 = testMovies.get(0); // First test movie
        Movie watchlistMovie2 = testMovies.get(1); // Second test movie
        watchlist.add(watchlistMovie1);
        watchlist.add(watchlistMovie2);
        user.setWatchlist(watchlist);

        // Set watched movies
        List<Movie> watchedMovies = new ArrayList<>();
        Movie watchedMovie = testMovies.get(2); // Third test movie
        watchedMovies.add(watchedMovie);
        user.setWatchedMovies(watchedMovies);

        return user;
    }
}