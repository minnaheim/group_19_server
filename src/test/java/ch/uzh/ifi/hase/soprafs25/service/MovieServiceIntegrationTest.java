package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test class for the MovieService
 *
 * This is an integration test that:
 * - Uses the real UserRepository and database
 * - Mocks only the TMDbService to avoid external API calls
 * - Tests the movie suggestion functionality with real database entities
 */
@WebAppConfiguration
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MovieServiceIntegrationTest {

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Qualifier("movieRepository")
    @Autowired
    private MovieRepository movieRepository;

    @MockBean
    private TMDbService tmdbService;

    @Autowired
    private MovieService movieService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;

    @BeforeEach
    public void setupTestData() throws JsonProcessingException {
        // Clear repositories before each test
        userRepository.deleteAll();
        movieRepository.deleteAll();

        // Create a test user
        testUser = new User();
        testUser.setUsername("testUser" + new Random().nextInt(10000));
        testUser.setPassword("password123");
        testUser.setEmail("test" + new Random().nextInt(10000) + "@example.com");
        testUser.setStatus(UserStatus.ONLINE);

        // Set favorite genres
        List<String> favoriteGenres = new ArrayList<>(Arrays.asList("Action", "Comedy"));
        testUser.setFavoriteGenres(favoriteGenres);

        Map<String, String> actorsMap = new HashMap<>();
        actorsMap.put("123", "Tom Hanks");
        actorsMap.put("456", "Meryl Streep");
        List<ActorDTO> actorDTOs = actorsMap.entrySet().stream()
            .map(entry -> {
                ActorDTO dto = new ActorDTO();
                try { dto.setId(Integer.parseInt(entry.getKey())); } catch (NumberFormatException e) { System.err.println("Warning: Could not parse actor ID " + entry.getKey());}
                dto.setName(entry.getValue());
                return dto;
            }).collect(Collectors.toList());
        testUser.setFavoriteActorsJson(objectMapper.writeValueAsString(actorDTOs));

        Map<String, String> directorsMap = new HashMap<>();
        directorsMap.put("2323", "Steven Spielberg");
        directorsMap.put("421432156", "Christopher Nolan");
        List<DirectorDTO> directorDTOs = directorsMap.entrySet().stream()
            .map(entry -> {
                DirectorDTO dto = new DirectorDTO();
                try { dto.setId(Integer.parseInt(entry.getKey())); } catch (NumberFormatException e) { System.err.println("Warning: Could not parse director ID " + entry.getKey()); }
                dto.setName(entry.getValue());
                return dto;
            }).collect(Collectors.toList());
        testUser.setFavoriteDirectorsJson(objectMapper.writeValueAsString(directorDTOs));

        // Initialize empty collections
        testUser.setWatchedMovies(new ArrayList<>());
        testUser.setWatchlist(new ArrayList<>());

        // Save the user to ensure it exists in the database
        testUser = userRepository.save(testUser);
        userRepository.flush(); // Ensure data is persisted before tests run
    }

    @AfterEach
    public void cleanup() {
        userRepository.deleteAll();
        movieRepository.deleteAll();
    }

    /**
     * Test 4.1 Test that getMovieSuggestions fetches real user data from database
     * and correctly processes user favorites
     */
    @Test
    public void testGetMovieSuggestions_WithRealUserFromDatabase() throws JsonProcessingException {
        // Create movies
        Movie watchedMovie = new Movie();
        watchedMovie.setMovieId(101);
        watchedMovie.setTitle("Previously Watched Movie");
        watchedMovie.setGenres(new ArrayList<>(Arrays.asList("Action")));
        watchedMovie.setYear(2020);
        movieRepository.save(watchedMovie);

        Movie inWatchlist = new Movie();
        inWatchlist.setMovieId(102);
        inWatchlist.setTitle("Movie in Watchlist");
        inWatchlist.setGenres(new ArrayList<>(Arrays.asList("Comedy")));
        inWatchlist.setYear(2021);
        movieRepository.save(inWatchlist);

        // Fetch user from DB to ensure we have the managed entity
        User managedUser = userRepository.findById(testUser.getUserId()).orElseThrow();

        // Add movies to the managed collections
        managedUser.getWatchedMovies().add(watchedMovie);
        managedUser.getWatchlist().add(inWatchlist);

        // Save the updates
        userRepository.save(managedUser);

        // Mock TMDbService
        List<Movie> mockSuggestedMovies = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Movie movie = new Movie();
            movie.setMovieId(200 + i);
            movie.setTitle("Suggested Movie " + i);
            movie.setGenres(new ArrayList<>(Arrays.asList("Action", "Comedy")));
            movie.setYear(2022);
            mockSuggestedMovies.add(movie);
        }

        when(tmdbService.searchMovies(any(Movie.class))).thenReturn(mockSuggestedMovies);

        // Call the method under test
        List<Movie> suggestions = movieService.getMovieSuggestions(managedUser.getUserId(), 3);

        // Assertions
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertEquals(3, suggestions.size());

        for (Movie suggestion : suggestions) {
            assertNotEquals(watchedMovie.getMovieId(), suggestion.getMovieId());
            assertNotEquals(inWatchlist.getMovieId(), suggestion.getMovieId());
        }

        for (Movie suggestion : suggestions) {
            boolean hasMatchingGenre = false;
            for (String genre : suggestion.getGenres()) {
                if (testUser.getFavoriteGenres().contains(genre)) {
                    hasMatchingGenre = true;
                    break;
                }
            }
            assertTrue(hasMatchingGenre, "Suggestion should match at least one favorite genre");
        }
    }

    /**
     * Test 4.2 - Verify TMDbService is called with correct parameters based on user
     * favorites
     */
    @Test
    public void testVerifyMovieSuggestionsWithPreferencesFromDatabase() throws JsonProcessingException {
        // 1. Create User with preferences
        User user = new User();
        user.setUsername("prefUser" + new Random().nextInt(10000));
        user.setPassword("password");
        user.setToken("token-" + UUID.randomUUID().toString());
        user.setEmail("permutation" + new Random().nextInt(10000) + "@example.com");
        user.setStatus(UserStatus.ONLINE);

        // Set user favorites
        List<String> favoriteGenres = Arrays.asList("Action", "Comedy");
        Map<String, String> actorsMap = new HashMap<>();
        actorsMap.put("123", "Tom Hanks");
        actorsMap.put("456", "Meryl Streep");
        Map<String, String> directorsMap = new HashMap<>();
        directorsMap.put("2323", "Steven Spielberg");
        directorsMap.put("421432156", "Christopher Nolan");
        user.setFavoriteGenres(favoriteGenres);
        List<ActorDTO> actorDTOs = actorsMap.entrySet().stream()
            .map(entry -> {
                ActorDTO dto = new ActorDTO();
                try { dto.setId(Integer.parseInt(entry.getKey())); } catch (NumberFormatException e) { System.err.println("Warning: Could not parse actor ID " + entry.getKey());}
                dto.setName(entry.getValue());
                return dto;
            }).collect(Collectors.toList());
        user.setFavoriteActorsJson(objectMapper.writeValueAsString(actorDTOs));
        List<DirectorDTO> directorDTOs = directorsMap.entrySet().stream()
            .map(entry -> {
                DirectorDTO dto = new DirectorDTO();
                try { dto.setId(Integer.parseInt(entry.getKey())); } catch (NumberFormatException e) { System.err.println("Warning: Could not parse director ID " + entry.getKey()); }
                dto.setName(entry.getValue());
                return dto;
            }).collect(Collectors.toList());
        user.setFavoriteDirectorsJson(objectMapper.writeValueAsString(directorDTOs));

        // Save user to database
        userRepository.save(user);
        userRepository.flush();

        // 2. Create mock TMDb response based on user favorites
        List<Movie> mockTMDbMovies = new ArrayList<>();
        Movie movie1 = new Movie();
        movie1.setMovieId(101);
        movie1.setTitle("Test Movie 1");
        movie1.setGenres(Arrays.asList("Action", "Adventure"));
        mockTMDbMovies.add(movie1);

        Movie movie2 = new Movie();
        movie2.setMovieId(102);
        movie2.setTitle("Test Movie 2");
        movie2.setGenres(Arrays.asList("Comedy"));
        mockTMDbMovies.add(movie2);

        // 3. Set up TMDbService mock to verify parameter passing
        Mockito.when(tmdbService.searchMovies(any(Movie.class))).thenReturn(mockTMDbMovies);

        // 4. Call the service method
        List<Movie> suggestions = movieService.getMovieSuggestions(user.getUserId(), 5);

        // 5. Verify TMDbService was called with correct parameters
        Mockito.verify(tmdbService).searchMovies(Mockito.argThat(searchParams -> {
            return searchParams.getGenres() != null &&
                    searchParams.getGenres().containsAll(favoriteGenres) &&
                    searchParams.getActors() != null &&
                    searchParams.getActors().containsAll(actorDTOs.stream().map(ActorDTO::getId).map(String::valueOf).collect(Collectors.toList())) &&
                    searchParams.getDirectors() != null &&
                    searchParams.getDirectors().containsAll(directorDTOs.stream().map(DirectorDTO::getId).map(String::valueOf).collect(Collectors.toList()));
        }));

        // 6. Verify suggestions were returned correctly
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.stream().anyMatch(m -> m.getMovieId() == 101));
        assertTrue(suggestions.stream().anyMatch(m -> m.getMovieId() == 102));
    }

    /**
     * Test 4.3 -Test for edge case: user with no favorites
     */
    @Test
    public void testVerifyMovieSuggestionsNoUserPreferences() throws JsonProcessingException {
        // 1. Create a user with no preferences
        User user = new User();
        user.setUsername("noPrefUser" + new Random().nextInt(10000));
        user.setPassword("password");
        user.setToken("token-" + UUID.randomUUID().toString());
        user.setEmail("permutation" + new Random().nextInt(10000) + "@example.com");
        user.setStatus(UserStatus.ONLINE);

        // Save user to database
        userRepository.save(user);
        userRepository.flush();

        // 2. Set up TMDbService to return popular movies when no specific params
        List<Movie> popularMovies = Arrays.asList(
                createTestMovie(301, "Popular Movie 1"),
                createTestMovie(302, "Popular Movie 2"),
                createTestMovie(303, "Popular Movie 3"));

        Mockito.when(tmdbService
                .searchMovies(Mockito.argThat(params -> (params.getGenres() == null || params.getGenres().isEmpty()) &&
                        (params.getActors() == null || params.getActors().isEmpty()) &&
                        (params.getDirectors() == null || params.getDirectors().isEmpty()))))
                .thenReturn(popularMovies);

        // 3. Call the service method
        List<Movie> suggestions = movieService.getMovieSuggestions(user.getUserId(), 3);

        // 4. Verify popular movies were returned
        assertNotNull(suggestions);
        assertEquals(3, suggestions.size());
        assertTrue(suggestions.containsAll(popularMovies));
    }

    @Test
    public void testVerifyFavoriteActorsAndDirectorsAreSavedAndRetrievedCorrectly() throws JsonProcessingException {
        // 1. Create a User (or use testUser if setup in @BeforeEach)
        // For this example, we assume testUser is configured in setupTestData() and is available.
        // If not, a new user specific to this test should be created, saved, and used.
        // User userForFavTest = new User(); ... userRepository.save(userForFavTest);

        // 2. Create lists of ActorDTO and DirectorDTO (these are the 'expected' values)
        ActorDTO actor1 = new ActorDTO();
        actor1.setId(1);
        actor1.setName("Actor One");
        ActorDTO actor2 = new ActorDTO();
        actor2.setId(2);
        actor2.setName("Actor Two");
        List<ActorDTO> expectedActors = Arrays.asList(actor1, actor2);

        DirectorDTO director1 = new DirectorDTO();
        director1.setId(10);
        director1.setName("Director One");
        DirectorDTO director2 = new DirectorDTO();
        director2.setId(20);
        director2.setName("Director Two");
        List<DirectorDTO> expectedDirectors = Arrays.asList(director1, director2);

        // 3. Serialize and set on User (testUser)
        testUser.setFavoriteActorsJson(objectMapper.writeValueAsString(expectedActors));
        testUser.setFavoriteDirectorsJson(objectMapper.writeValueAsString(expectedDirectors));

        // 4. Save User (testUser)
        userRepository.save(testUser);
        userRepository.flush(); // Ensure changes are persisted before retrieval

        // 5. Retrieve the User and parse favorite actors/directors JSON
        User retrievedUser = userRepository.findById(testUser.getUserId())
                .orElseThrow(() -> new AssertionError("User not found with ID: " + testUser.getUserId()));

        List<ActorDTO> actualFavoriteActors;
        String actorsJson = retrievedUser.getFavoriteActorsJson();
        if (actorsJson != null && !actorsJson.isEmpty()) {
            actualFavoriteActors = objectMapper.readValue(actorsJson, new TypeReference<List<ActorDTO>>() {});
        } else {
            actualFavoriteActors = Collections.emptyList();
        }

        List<DirectorDTO> actualFavoriteDirectors;
        String directorsJson = retrievedUser.getFavoriteDirectorsJson();
        if (directorsJson != null && !directorsJson.isEmpty()) {
            actualFavoriteDirectors = objectMapper.readValue(directorsJson, new TypeReference<List<DirectorDTO>>() {});
        } else {
            actualFavoriteDirectors = Collections.emptyList();
        }

        // 6. Assertions
        assertNotNull(actualFavoriteActors, "Retrieved favorite actors list should not be null");
        assertEquals(expectedActors.size(), actualFavoriteActors.size(), "Number of favorite actors should match");
        for (int i = 0; i < expectedActors.size(); i++) {
            assertEquals(expectedActors.get(i).getId(), actualFavoriteActors.get(i).getId(), "Actor ID mismatch at index " + i);
            assertEquals(expectedActors.get(i).getName(), actualFavoriteActors.get(i).getName(), "Actor name mismatch at index " + i);
        }

        assertNotNull(actualFavoriteDirectors, "Retrieved favorite directors list should not be null");
        assertEquals(expectedDirectors.size(), actualFavoriteDirectors.size(), "Number of favorite directors should match");
        for (int i = 0; i < expectedDirectors.size(); i++) {
            assertEquals(expectedDirectors.get(i).getId(), actualFavoriteDirectors.get(i).getId(), "Director ID mismatch at index " + i);
            assertEquals(expectedDirectors.get(i).getName(), actualFavoriteDirectors.get(i).getName(), "Director name mismatch at index " + i);
        }
    }

    @Test
    public void getFavoriteActors_whenUserHasFavorites_returnsFavoriteActors() throws JsonProcessingException {
        // Arrange: Test user with favorite actors is already set up in @BeforeEach
        // Ensure testUser and its favorites are loaded/refreshed if necessary
        User currentUser = userRepository.findById(testUser.getUserId())
                .orElseThrow(() -> new AssertionError("Test user not found: " + testUser.getUserId()));

        // Act
        String favActorsJson = currentUser.getFavoriteActorsJson();
        List<ActorDTO> favoriteActors;
        if (favActorsJson != null && !favActorsJson.isEmpty()) {
            favoriteActors = objectMapper.readValue(favActorsJson, new TypeReference<List<ActorDTO>>() {});
        } else {
            favoriteActors = Collections.emptyList();
        }

        // Assert
        assertNotNull(favoriteActors);
        assertFalse(favoriteActors.isEmpty());
        // Assuming one favorite actor "Tom Hanks" with ID 123 was set up
        assertEquals(2, favoriteActors.size());
        assertTrue(favoriteActors.stream().anyMatch(a -> a.getName().equals("Tom Hanks") && a.getId().equals(123)));
        assertTrue(favoriteActors.stream().anyMatch(a -> a.getName().equals("Meryl Streep") && a.getId().equals(456)));

        // Verify that the JSON string in User entity was correctly parsed
        String json = testUser.getFavoriteActorsJson();
        List<ActorDTO> expectedActors = objectMapper.readValue(json, new TypeReference<List<ActorDTO>>() {});
        assertEquals(expectedActors.size(), favoriteActors.size());
        assertTrue(favoriteActors.stream().anyMatch(a -> a.getName().equals("Tom Hanks") && a.getId().equals(123)));
        assertTrue(favoriteActors.stream().anyMatch(a -> a.getName().equals("Meryl Streep") && a.getId().equals(456)));
    }

    @Test
    public void getFavoriteDirectors_whenUserHasFavorites_returnsFavoriteDirectors() throws JsonProcessingException {
        // Arrange: Test user with favorite directors is already set up in @BeforeEach
        User currentUser = userRepository.findById(testUser.getUserId())
                .orElseThrow(() -> new AssertionError("Test user not found: " + testUser.getUserId()));

        // Act
        String favDirectorsJson = currentUser.getFavoriteDirectorsJson();
        List<DirectorDTO> favoriteDirectors;
        if (favDirectorsJson != null && !favDirectorsJson.isEmpty()) {
            favoriteDirectors = objectMapper.readValue(favDirectorsJson, new TypeReference<List<DirectorDTO>>() {});
        } else {
            favoriteDirectors = Collections.emptyList();
        }

        // Assert
        assertNotNull(favoriteDirectors);
        assertFalse(favoriteDirectors.isEmpty());
        // Assuming one favorite director "Steven Spielberg" with ID 2323 was set up
        assertEquals(2, favoriteDirectors.size());
        assertTrue(favoriteDirectors.stream().anyMatch(d -> d.getName().equals("Steven Spielberg") && d.getId().equals(2323)));
        assertTrue(favoriteDirectors.stream().anyMatch(d -> d.getName().equals("Christopher Nolan") && d.getId().equals(421432156)));

        // Verify that the JSON string in User entity was correctly parsed
        String json = testUser.getFavoriteDirectorsJson();
        List<DirectorDTO> expectedDirectors = objectMapper.readValue(json, new TypeReference<List<DirectorDTO>>() {});
        assertEquals(expectedDirectors.size(), favoriteDirectors.size());
        assertTrue(favoriteDirectors.stream().anyMatch(d -> d.getName().equals("Steven Spielberg") && d.getId().equals(2323)));
        assertTrue(favoriteDirectors.stream().anyMatch(d -> d.getName().equals("Christopher Nolan") && d.getId().equals(421432156)));
    }

    // Helper method to create test movies
    private Movie createTestMovie(long id, String title) {
        Movie movie = new Movie();
        movie.setMovieId(id);
        movie.setTitle(title);
        return movie;
    }
}
