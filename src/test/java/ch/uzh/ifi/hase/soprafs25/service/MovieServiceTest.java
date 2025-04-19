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
        // CHANGE: Instead of returning the same movies for any search parameter,
        // create different movie sets based on the search parameters

        // Create a list of unique movies to return for each search
        List<Movie> uniqueMovies = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Movie movie = new Movie();
            movie.setMovieId(300L + i);
            movie.setTitle("Suggested Movie " + i);
            uniqueMovies.add(movie);
        }

        // CHANGE: Setup a more advanced mock behavior for TMDbService
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
}


