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
        testMovie1.setTitle("Already Watched Movie");
        testMovie1.setPosterURL("example.com/poster1.jpg");
        testMovie1.setGenres(Arrays.asList("Action", "Adventure"));


        testMovie2 = new Movie();
        testMovie2.setMovieId(1002L);
        testMovie2.setTitle("Test Sci-Fi Movie");
        testMovie2.setPosterURL("example.com/poster2.jpg");
        testMovie2.setGenres(Arrays.asList("Science Fiction"));

        testMovie3 = new Movie();
        testMovie3.setMovieId(1003L);
        testMovie3.setTitle("Suggested Movie: Test Adventure Movie");
        testMovie3.setPosterURL("example.com/poster3.jpg");
        testMovie3.setGenres(Arrays.asList("Adventure"));

        watchedMovie = new Movie();
        watchedMovie.setMovieId(1004L);
        watchedMovie.setTitle("Already Watched Movie");
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

    }

    /**
     * Test 1.1 - Mock UserRepository to return a predefined user with known preferences
     * This test verifies that the getMovieSuggestions method correctly retrieves a user from the repository
     * and uses their preferences to generate movie suggestions
     */
    @Test
    void testGetMovieSuggestions_withUserPreferences() {
        // Mock UserRepository to return our test user
        when(userRepository.findById(eq(2341L))).thenReturn(Optional.of(testUser));

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
        assertEquals("Suggested Movie: Test Adventure Movie", result.get(0).getTitle());

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
        // Mock UserRepository to return our test user
        when(userRepository.findById(eq(2341L))).thenReturn(Optional.of(testUser));

        // Create lists of movies that TMDbService will return
        List<Movie> tmdbMovies1 = new ArrayList<>();
        tmdbMovies1.add(testMovie1);
        tmdbMovies1.add(watchlistMovie); // This should be filtered out in results
        tmdbMovies1.add(watchedMovie);   // This should be filtered out in results

        List<Movie> tmdbMovies2 = new ArrayList<>();
        tmdbMovies2.add(testMovie2);

        List<Movie> tmdbMovies3 = new ArrayList<>();
        tmdbMovies3.add(testMovie3);

        // First search with all parameters (most specific)
        when(movieService.getMovies(argThat(movie ->
                movie != null &&
                        movie.getGenres() != null && movie.getGenres().containsAll(Arrays.asList("Action", "Science Fiction", "Adventure")) &&
                        movie.getActors() != null && movie.getActors().containsAll(Arrays.asList("6193", "24045", "1357546", "2524", "27578")) &&
                        movie.getDirectors() != null && movie.getDirectors().containsAll(Arrays.asList("525", "1408530"))
        ))).thenReturn(tmdbMovies1);

        // Second search with just genres
        when(movieService.getMovies(argThat(movie ->
                movie != null &&
                        movie.getGenres() != null && movie.getGenres().containsAll(Arrays.asList("Action", "Science Fiction", "Adventure")) &&
                        (movie.getActors() == null || movie.getActors().isEmpty()) &&
                        (movie.getDirectors() == null || movie.getDirectors().isEmpty())
        ))).thenReturn(tmdbMovies2);

        // Third search with no parameters (fallback)
        when(movieService.getMovies(argThat(movie ->
                movie != null &&
                        (movie.getGenres() == null || movie.getGenres().isEmpty()) &&
                        (movie.getActors() == null || movie.getActors().isEmpty()) &&
                        (movie.getDirectors() == null || movie.getDirectors().isEmpty())
        ))).thenReturn(tmdbMovies3);

        // This ensures we don't get NullPointerException during the test
        when(movieService.getMovies(any(Movie.class))).thenReturn(Collections.emptyList());

        // Call the method under test
        List<Movie> result = movieService.getMovieSuggestions(2341L, 5);

        // Verify that TMDbService was called with the right parameters
        verify(movieService, atLeastOnce()).getMovies(any(Movie.class));

        // Check the result contains only non-watched, non-watchlist movies
        assertEquals(3, result.size());
        assertTrue(result.contains(testMovie1));
        assertTrue(result.contains(testMovie2));
        assertTrue(result.contains(testMovie3));
        assertFalse(result.contains(watchlistMovie));
        assertFalse(result.contains(watchedMovie));

        // Verify the search was performed with different parameter combinations
        verify(movieService, atLeastOnce()).getMovies(argThat(movie ->
                movie.getGenres() != null && movie.getGenres().containsAll(Arrays.asList("Action", "Science Fiction", "Adventure")) &&
                        movie.getActors() != null && movie.getActors().size() > 0 &&
                        movie.getDirectors() != null && movie.getDirectors().size() > 0
        ));

        // Also verify fallback searches were performed when needed
        verify(movieService, atLeastOnce()).getMovies(argThat(movie ->
                movie.getGenres() != null && movie.getGenres().size() > 0 &&
                        (movie.getActors() == null || movie.getActors().isEmpty()) &&
                        (movie.getDirectors() == null || movie.getDirectors().isEmpty())
        ));
    }
}

