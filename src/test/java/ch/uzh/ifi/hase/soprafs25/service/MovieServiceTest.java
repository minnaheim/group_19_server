package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovieServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TMDbService tmdbService;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private User testUser;
    private Movie testMovie1;
    private Movie testMovie2;
    private Movie testMovie3;
    private Movie watchedMovie;
    private Movie watchlistMovie;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup test movies for watchlist and watched movies
        testMovie1 = new Movie();
        testMovie1.setMovieId(1001L);
        testMovie1.setTitle("Test Movie 1");
        testMovie1.setPosterURL("example.com/poster1.jpg");
        testMovie1.setGenres(Arrays.asList("Action", "Adventure"));


        testMovie2 = new Movie();
        testMovie2.setMovieId(1002L);
        testMovie2.setTitle("Test Movie 2");
        testMovie2.setPosterURL("example.com/poster2.jpg");
        testMovie2.setGenres(Arrays.asList("Science Fiction"));

        testMovie3 = new Movie();
        testMovie3.setMovieId(1003L);
        testMovie3.setTitle("Test Movie 3");
        testMovie3.setPosterURL("example.com/poster3.jpg");
        testMovie3.setGenres(Arrays.asList("Adventure"));

        watchedMovie = new Movie();
        watchedMovie.setMovieId(1004L);
        watchedMovie.setTitle("Watched Movie\n");
        watchedMovie.setPosterURL("example.com/poster4.jpg");
        watchedMovie.setGenres(Arrays.asList("Action"));


        watchlistMovie = new Movie();
        watchlistMovie.setMovieId(1005L);
        watchlistMovie.setTitle("Watchlist Movie");
        watchlistMovie.setPosterURL("example.com/poster5.jpg");
        watchlistMovie.setGenres(Arrays.asList("Comedy"));


        // Setup test user with known preferences
        testUser = new User();
        testUser.setUserId(2341L);
        testUser.setUsername("testUser");
        testUser.setEmail("test.user@gmail.com");
        testUser.setPassword("dskjföaldskj^^32142");
        testUser.setBio("what kind of string is bio?");
        testUser.setFavoriteGenres(Arrays.asList("Action", "Science Fiction", "Adventure"));
        testUser.setFavoriteMovie(testMovie1);

        // Set favorite genres
        List<String> favoriteGenres = new ArrayList<>();
        favoriteGenres.add("Action");
        favoriteGenres.add("Science Fiction");
        favoriteGenres.add("Adventure");
        testUser.setFavoriteGenres(favoriteGenres);

        // Setup favorite actors
        Map<String, String> favoriteActors = new HashMap<>();
        favoriteActors.put("6193", "Leonardo DiCaprio");
        favoriteActors.put("24045", "Joseph Gordon-Levitt");
        favoriteActors.put("1357546", "Ken Watanabe");
        favoriteActors.put("2524", "Tom Hardy");
        favoriteActors.put("27578", "Elliot Page");
        testUser.setFavoriteActors(favoriteActors);

        // Setup favorite directors
        Map<String, String> favoriteDirectors = new HashMap<>();
        favoriteDirectors.put("525", "Christopher Nolan");
        favoriteDirectors.put("1408530", "Emma Thomas");
        testUser.setFavoriteDirectors(favoriteDirectors);

        // Setup watchlist and watched movies
        testUser.setWatchlist(Collections.singletonList(testMovie2));
        testUser.setWatchedMovies(Collections.singletonList(testMovie1));

        List<Movie> watchlist = new ArrayList<>();
        watchlist.add(watchlistMovie);
        testUser.setWatchlist(watchlist);

        List<Movie> watchedMovies = new ArrayList<>();
        watchedMovies.add(watchedMovie);
        testUser.setWatchedMovies(watchedMovies);

        // Set favorite movie
        Movie favoriteMovie = new Movie();
        favoriteMovie.setMovieId(30);
        favoriteMovie.setTitle("Favorite Movie");
        testUser.setFavoriteMovie(favoriteMovie);

        // Mock UserRepository to return our test user
        when(userRepository.findById(eq(2341L))).thenReturn(Optional.of(testUser));

    }

    /**
     * Test 1.1 - Mock UserRepository to return a predefined user with known preferences
     * This test verifies that the getMovieSuggestions method correctly retrieves a user from the repository
     * and uses their preferences to generate movie suggestions
     */
    @Test
    void testGetMovieSuggestions_withUserPreferences() {
        ;

        // Prepare movie suggestions to be returned by TMDbService
        List<Movie> suggestedMovies = new ArrayList<>();
        suggestedMovies.add(testMovie3); // Suggested movie that is not in watchlist or watched

        // Mock TMDbService to return our predefined movies when called with any search parameters
        // This simulates the TMDbService returning results based on user preferences
        when(tmdbService.searchMovies(any(Movie.class))).thenReturn(suggestedMovies);

        // Call the method under test
        List<Movie> result = movieService.getMovieSuggestions(2341L, 100);

        // Verify UserRepository was called exactly once to retrieve the user
        verify(userRepository, times(1)).findById(eq(2341L));

        // Verify the TMDbService was called at least once to search for movies
        verify(tmdbService, atLeastOnce()).searchMovies(any(Movie.class));

        // Verify the result contains the suggested movie
        assertEquals(1, result.size());
        assertEquals("Test Movie 3", result.get(0).getTitle());

        // Verify watchlist and watched movies are filtered out
        assertFalse(result.contains(testMovie1)); // Watched movie filtered out
        assertFalse(result.contains(testMovie2)); // Watchlist movie filtered out
    }

    /**
     * Test 1.2 - Mock TMDbService to return controlled sets of movies for different search parameters
     * This test verifies that the MovieService queries TMDbService with correct search parameters
     * and processes the returned movies appropriately
     */
    @Test
    void testGetMovieSuggestions_mockTMDbService() {
        // create different movie sets based on the search parameters

        // Create a list of unique movies to return for each search
        List<Movie> uniqueMovies = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Movie movie = new Movie();
            movie.setMovieId(300L + i);
            movie.setTitle("Suggested Movie " + i);
            uniqueMovies.add(movie);
        }

        // Return different movies for each call to simulate realistic behavior
        when(tmdbService.searchMovies(any(Movie.class))).thenAnswer(invocation -> {
            // Create a new list for each call to prevent modification issues
            List<Movie> resultMovies = new ArrayList<>();

            // Add 3 unique movies for each call (adjust as needed)
            for (int i = 0; i < 3; i++) {
                int index = (int) (Math.random() * uniqueMovies.size());
                if (index < uniqueMovies.size()) {
                    resultMovies.add(uniqueMovies.get(index));
                }
            }

            return resultMovies;
        });

        // Execute
        List<Movie> suggestions = movieService.getMovieSuggestions(2341L, 3);

        // Verify
        assertEquals(3, suggestions.size(), "The method should return exactly 3 movies");

        // Make sure watched and watchlist movies are filtered out
        for (Movie movie : suggestions) {
            assertNotEquals(watchedMovie.getMovieId(), movie.getMovieId(),
                    "Watched movies should be filtered out");
            assertNotEquals(watchlistMovie.getMovieId(), movie.getMovieId(),
                    "Watchlist movies should be filtered out");
        }

        // Verify TMDbService was called at least once
        verify(tmdbService, atLeastOnce()).searchMovies(any(Movie.class));
    }

    /**
     * Test 1.3 - Verify the method correctly filters out watchlist/watched movies
     * This test ensures that movies in the user's watchlist and watched list are not included in suggestions
     */
    @Test
    void testGetMovieSuggestions_filterWatchedAndWatchlist() {
        // test to verify filtering of watched and watchlist movies

        // Mock movieService to return movies including watched and watchlist movies
        List<Movie> allMovies = Arrays.asList(testMovie1, testMovie2, testMovie3, watchedMovie, watchlistMovie);
        when(movieService.getMovies(any(Movie.class))).thenReturn(allMovies);

        // Call the method under test
        List<Movie> suggestions = movieService.getMovieSuggestions(testUser.getUserId(), 10);

        // Verify suggestions do not contain watched or watchlist movies
        assertNotNull(suggestions);
        assertFalse(suggestions.contains(watchedMovie), "Watched movie should be filtered out");
        assertFalse(suggestions.contains(watchlistMovie), "Watchlist movie should be filtered out");

        // Verify other movies are included
        assertTrue(suggestions.contains(testMovie1));
        assertTrue(suggestions.contains(testMovie2));
        assertTrue(suggestions.contains(testMovie3));
    }

    /**
     * Test 1.4 - Test the permutation logic works as expected
     * This test verifies that the search permutations are generated correctly and in the right order
     */
    @Test
    void testGenerateSearchPermutations() {
        // test to verify permutation generation logic

        // Set up test data
        List<String> genres = Arrays.asList("Action", "Adventure");
        List<String> actors = Arrays.asList("6193", "24045");
        List<String> directors = Arrays.asList("525");

        // Create spy on movieService to test private method
        MovieService spyMovieService = spy(movieService);

        // Use reflection to make the private method accessible
        List<Movie> permutations = null;
        try {
            java.lang.reflect.Method method = MovieService.class.getDeclaredMethod(
                    "generateSearchPermutations", List.class, List.class, List.class);
            method.setAccessible(true);
            permutations = (List<Movie>) method.invoke(spyMovieService, genres, actors, directors);
        } catch (Exception e) {
            fail("Failed to call private method: " + e.getMessage());
        }

        // Verify the permutations
        assertNotNull(permutations);
        assertFalse(permutations.isEmpty());

        // First permutation should have all parameters (most specific)
        Movie firstPermutation = permutations.get(0);
        assertEquals(genres, firstPermutation.getGenres());
        assertEquals(actors, firstPermutation.getActors());
        assertEquals(directors, firstPermutation.getDirectors());

        // Verify decreasing specificity in permutations
        boolean foundGenreOnlyQuery = false;
        boolean foundActorOnlyQuery = false;
        boolean foundDirectorOnlyQuery = false;

        for (Movie movie : permutations) {
            if (movie.getGenres() != null && !movie.getGenres().isEmpty() &&
                    (movie.getActors() == null || movie.getActors().isEmpty()) &&
                    (movie.getDirectors() == null || movie.getDirectors().isEmpty())) {
                foundGenreOnlyQuery = true;
            }

            if ((movie.getGenres() == null || movie.getGenres().isEmpty()) &&
                    movie.getActors() != null && !movie.getActors().isEmpty() &&
                    (movie.getDirectors() == null || movie.getDirectors().isEmpty())) {
                foundActorOnlyQuery = true;
            }

            if ((movie.getGenres() == null || movie.getGenres().isEmpty()) &&
                    (movie.getActors() == null || movie.getActors().isEmpty()) &&
                    movie.getDirectors() != null && !movie.getDirectors().isEmpty()) {
                foundDirectorOnlyQuery = true;
            }
        }

        assertTrue(foundGenreOnlyQuery, "Should include genre-only query");
        assertTrue(foundActorOnlyQuery, "Should include actor-only query");
        assertTrue(foundDirectorOnlyQuery, "Should include director-only query");
    }

    /**
     * Test 1.5 - Ensure it returns exactly the requested number of movies when possible
     * This test verifies that the method returns the correct number of suggestions
     */
    @Test
    void testGetMovieSuggestions_respectLimit() {
        // test to verify respect for requested limit

        // Create a large number of test movies
        List<Movie> manyMovies = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Movie movie = new Movie();
            movie.setMovieId(100L + i);
            movie.setTitle("Test Movie " + i);
            manyMovies.add(movie);
        }

        // Mock movieService to return many movies
        when(movieService.getMovies(any(Movie.class))).thenReturn(manyMovies);

        // Test with different limits
        int limit1 = 10;
        List<Movie> suggestions1 = movieService.getMovieSuggestions(testUser.getUserId(), limit1);
        assertEquals(limit1, suggestions1.size(), "Should return exactly " + limit1 + " movies");

        int limit2 = 50;
        List<Movie> suggestions2 = movieService.getMovieSuggestions(testUser.getUserId(), limit2);
        assertEquals(limit2, suggestions2.size(), "Should return exactly " + limit2 + " movies");

        int limit3 = 100;
        List<Movie> suggestions3 = movieService.getMovieSuggestions(testUser.getUserId(), limit3);
        assertEquals(limit3, suggestions3.size(), "Should return exactly " + limit3 + " movies");
    }

    /**
     * Test 1.6 - Testing when user has no preferences
     * This test verifies that the method handles users with no preferences correctly
     */
    @Test
    void testGetMovieSuggestions_noUserPreferences() {
        // test for users with no preferences

        // Create user with no preferences
        User userNoPrefs = new User();
        userNoPrefs.setUserId(9999L);
        userNoPrefs.setUsername("userNoPrefs");
        userNoPrefs.setFavoriteGenres(Collections.emptyList());
        userNoPrefs.setFavoriteActors(Collections.emptyMap());
        userNoPrefs.setFavoriteDirectors(Collections.emptyMap());
        userNoPrefs.setWatchedMovies(Collections.emptyList());
        userNoPrefs.setWatchlist(Collections.emptyList());

        // Mock repository to return this user
        when(userRepository.findById(userNoPrefs.getUserId())).thenReturn(Optional.of(userNoPrefs));

        // Mock movieService to return movies for empty search params
        List<Movie> defaultMovies = Arrays.asList(testMovie1, testMovie2, testMovie3);
        when(movieService.getMovies(argThat(movie ->
                (movie.getGenres() == null || movie.getGenres().isEmpty()) &&
                        (movie.getActors() == null || movie.getActors().isEmpty()) &&
                        (movie.getDirectors() == null || movie.getDirectors().isEmpty())
        ))).thenReturn(defaultMovies);

        // Call the method under test
        List<Movie> suggestions = movieService.getMovieSuggestions(userNoPrefs.getUserId(), 10);

        // Verify tmdbService was called with empty parameters
        verify(tmdbService).searchMovies(argThat(movie ->
                (movie.getGenres() == null || movie.getGenres().isEmpty()) &&
                        (movie.getActors() == null || movie.getActors().isEmpty()) &&
                        (movie.getDirectors() == null || movie.getDirectors().isEmpty())
        ));

        // Verify suggestions were returned
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertEquals(defaultMovies.size(), suggestions.size());
        assertTrue(suggestions.containsAll(defaultMovies));
    }
}



