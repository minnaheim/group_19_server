package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;

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

    private User testUser;
    private List<Movie> watchlistMovies;
    private List<Movie> watchedMovies;
    private List<Movie> suggestedMovies;

    /**
     * Test 4.1 Test that getMovieSuggestions fetches real user data from database
     * and correctly processes user favorites
     */
    @Test
    public void testGetMovieSuggestions_WithRealUserFromDatabase() {
        // Create a user with well-defined favorites
        User user = new User();
        user.setUsername("testUser" + new Random().nextInt(10000));
        user.setPassword("password123");
        user.setEmail("test" + new Random().nextInt(10000) + "@example.com");

        // Set favorite genres
        List<String> favoriteGenres = new ArrayList<>(Arrays.asList("Action", "Comedy"));
        user.setFavoriteGenres(favoriteGenres);

        // Set preferred actors and directors
        Map<String, String> actorsMap = new HashMap<>();
        actorsMap.put("123", "Tom Hanks");
        actorsMap.put("456", "Meryl Streep");
        user.setFavoriteActors(actorsMap);

        Map<String, String> directorsMap = new HashMap<>();
        directorsMap.put("2323", "Steven Spielberg");
        directorsMap.put("421432156", "Christopher Nolan");
        user.setFavoriteDirectors(directorsMap);

        // Initialize empty collections
        user.setWatchedMovies(new ArrayList<>());
        user.setWatchlist(new ArrayList<>());

        // Save the user to ensure it exists in the database
        User savedUser = userRepository.save(user);

        // Create movies
        Movie watched = new Movie();
        watched.setMovieId(101);
        watched.setTitle("Previously Watched Movie");
        watched.setGenres(new ArrayList<>(Arrays.asList("Action")));
        watched.setYear(2020);
        movieRepository.save(watched);

        Movie inWatchlist = new Movie();
        inWatchlist.setMovieId(102);
        inWatchlist.setTitle("Movie in Watchlist");
        inWatchlist.setGenres(new ArrayList<>(Arrays.asList("Comedy")));
        inWatchlist.setYear(2021);
        movieRepository.save(inWatchlist);

        // Fetch user from DB to ensure we have the managed entity
        User managedUser = userRepository.findById(savedUser.getUserId()).orElseThrow();

        // Add movies to the managed collections
        managedUser.getWatchedMovies().add(watched);
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
            assertNotEquals(watched.getMovieId(), suggestion.getMovieId());
            assertNotEquals(inWatchlist.getMovieId(), suggestion.getMovieId());
        }

        for (Movie suggestion : suggestions) {
            boolean hasMatchingGenre = false;
            for (String genre : suggestion.getGenres()) {
                if (favoriteGenres.contains(genre)) {
                    hasMatchingGenre = true;
                    break;
                }
            }
            assertTrue(hasMatchingGenre, "Suggestion should match at least one favorite genre");
        }
    }

    /**
    * Test 4.2 - Verify TMDbService is called with correct parameters based on user favorites
    */
    @Test
    public void testGetMovieSuggestions_TMDbServiceIntegration() {
        // 1. Create and save a user with favorites
        User user = new User();
        user.setUsername("testuser" + new Random().nextInt(10000));
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
        user.setFavoriteActors(actorsMap);
        user.setFavoriteDirectors(directorsMap);

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
                    searchParams.getActors().containsAll(actorsMap.keySet()) &&
                    searchParams.getDirectors() != null &&
                    searchParams.getDirectors().containsAll(directorsMap.keySet());
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
    public void testGetMovieSuggestions_UserWithNoFavorites() {
        // 1. Create and save a user without favorites
        User user = new User();
        user.setUsername("noprefuser" + new Random().nextInt(10000));
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
        user.setFavoriteActors(actorsMap);
        user.setFavoriteDirectors(directorsMap);

        // Save user to database
        userRepository.save(user);
        userRepository.flush();

        // 2. Set up TMDbService to return popular movies when no specific params
        List<Movie> popularMovies = Arrays.asList(
                createTestMovie(301, "Popular Movie 1"),
                createTestMovie(302, "Popular Movie 2"),
                createTestMovie(303, "Popular Movie 3")
        );

        Mockito.when(tmdbService.searchMovies(Mockito.argThat(params ->
                        (params.getGenres() == null || params.getGenres().isEmpty()) &&
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

    // Helper method to create test movies
    private Movie createTestMovie(long id, String title) {
        Movie movie = new Movie();
        movie.setMovieId(id);
        movie.setTitle(title);
        return movie;
    }

}
