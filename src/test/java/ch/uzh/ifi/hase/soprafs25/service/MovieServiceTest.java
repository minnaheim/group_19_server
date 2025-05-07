package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;
import ch.uzh.ifi.hase.soprafs25.service.TMDbService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovieServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TMDbService tmdbService;

    @InjectMocks
    private MovieService movieService;

    private User testUser;
    private Movie testMovie1;
    private Movie testMovie2;
    private Movie testMovie3;
    private Movie watchedMovie;
    private Movie watchlistMovie;
    private final ObjectMapper objectMapper = new ObjectMapper();

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


        // Setup test user with known favorites
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
        List<ActorDTO> favoriteActorDTOs = new ArrayList<>();
        ActorDTO actor1 = new ActorDTO(); actor1.setId(6193); actor1.setName("Leonardo DiCaprio");
        ActorDTO actor2 = new ActorDTO(); actor2.setId(24045); actor2.setName("Joseph Gordon-Levitt");
        ActorDTO actor3 = new ActorDTO(); actor3.setId(1357546); actor3.setName("Ken Watanabe");
        ActorDTO actor4 = new ActorDTO(); actor4.setId(2524); actor4.setName("Tom Hardy");
        ActorDTO actor5 = new ActorDTO(); actor5.setId(27578); actor5.setName("Elliot Page");
        favoriteActorDTOs.addAll(Arrays.asList(actor1, actor2, actor3, actor4, actor5));
        try {
            String actorsJson = objectMapper.writeValueAsString(favoriteActorDTOs);
            testUser.setFavoriteActorsJson(actorsJson);
        } catch (Exception e) {
            fail("Failed to serialize favorite actors to JSON: " + e.getMessage());
        }

        // Setup favorite directors
        List<DirectorDTO> favoriteDirectorDTOs = new ArrayList<>();
        DirectorDTO director1 = new DirectorDTO(); director1.setId(525); director1.setName("Christopher Nolan");
        DirectorDTO director2 = new DirectorDTO(); director2.setId(1408530); director2.setName("Emma Thomas");
        favoriteDirectorDTOs.addAll(Arrays.asList(director1, director2));
        try {
            String directorsJson = objectMapper.writeValueAsString(favoriteDirectorDTOs);
            testUser.setFavoriteDirectorsJson(directorsJson);
        } catch (Exception e) {
            fail("Failed to serialize favorite directors to JSON: " + e.getMessage());
        }

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
     * Test 1.1 - Mock UserRepository to return a predefined user with known favorites
     * This test verifies that the getMovieSuggestions method correctly retrieves a user from the repository
     * and uses their favorites to generate movie suggestions
     */
    @Test
    void testGetMovieSuggestions_withUserFavorites() {
        // Prepare a user for this specific test
        User userWithPrefs = new User();
        userWithPrefs.setUserId(2341L);
        userWithPrefs.setUsername("user2341");
        userWithPrefs.setFavoriteGenres(Arrays.asList("Sci-Fi")); // Example preference
        userWithPrefs.setFavoriteActorsJson("[]");
        userWithPrefs.setFavoriteDirectorsJson("[]");
        userWithPrefs.setWatchlist(new ArrayList<>());
        userWithPrefs.setWatchedMovies(new ArrayList<>());

        when(userRepository.findById(2341L)).thenReturn(Optional.of(userWithPrefs));

        // Prepare movie suggestions to be returned by TMDbService
        List<Movie> suggestedMovies = new ArrayList<>();
        suggestedMovies.add(testMovie3); // Suggested movie that is not in watchlist or watched

        // Mock TMDbService to return our predefined movies when called with any search parameters
        // This simulates the TMDbService returning results based on user favorites
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
    }

    /**
     * Test 1.2 - Mock TMDbService to return controlled sets of movies for different search parameters
     * This test verifies that the MovieService queries TMDbService with correct search parameters
     * and processes the returned movies appropriately
     */
    @Test
    void testGetMovieSuggestions_mockTMDbService() {
        // Setup a user with some preferences
        Long userId = 2341L;
        testUser = new User();
        testUser.setUserId(userId);
        testUser.setFavoriteGenres(Arrays.asList("Action")); // Add a favorite genre
        // Initialize favorite actors/directors to empty lists if not further specified
        testUser.setFavoriteActorsJson("[]"); 
        testUser.setFavoriteDirectorsJson("[]");
        testUser.setWatchlist(new ArrayList<>()); 
        testUser.setWatchedMovies(new ArrayList<>()); 

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Create a list of 3 unique movies to be returned by the mock
        List<Movie> mockedResponseMovies = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Movie movie = new Movie();
            movie.setMovieId(300L + i);
            movie.setTitle("Suggested Movie " + i);
            mockedResponseMovies.add(movie);
        }

        // Mock tmdbService.searchMovies to return our list of 3 movies for any search query
        when(tmdbService.searchMovies(any(Movie.class))).thenReturn(mockedResponseMovies);

        // Execute
        // Calling with userId and a limit of 3 for suggestions
        List<Movie> suggestions = movieService.getMovieSuggestions(userId, 3);

        // Verify
        assertEquals(3, suggestions.size(), "The method should return exactly 3 movies");

        // Verify TMDbService.searchMovies was called.
        // It should be called for permutations generated from user's preferences.
        // Given one favorite genre, it should generate at least one permutation and thus call searchMovies.
        verify(tmdbService, atLeastOnce()).searchMovies(any(Movie.class));
    }

    /**
     * Test 1.3 - Verify the method correctly filters out watchlist/watched movies
     * This test ensures that movies in the user's watchlist and watched list are not included in suggestions
     */
    @Test
    void testGetMovieSuggestions_filterWatchedAndWatchlist() {
        // Configure testUser specifically for this test's scenario.

        // Setup User's watched and watchlist for this specific test
        // This will use the instances of watchedMovie and watchlistMovie from @BeforeEach
        testUser.setWatchedMovies(new ArrayList<>(Arrays.asList(watchedMovie)));
        testUser.setWatchlist(new ArrayList<>(Arrays.asList(watchlistMovie)));
        // Ensure user has some preferences to trigger permutation generation
        if (testUser.getFavoriteGenres() == null || testUser.getFavoriteGenres().isEmpty()) {
            testUser.setFavoriteGenres(Arrays.asList("Action")); 
        }
        if (testUser.getFavoriteActorsJson() == null || testUser.getFavoriteActorsJson().equals("null")) {
            testUser.setFavoriteActorsJson("[]");
        }
        if (testUser.getFavoriteDirectorsJson() == null || testUser.getFavoriteDirectorsJson().equals("null")) {
            testUser.setFavoriteDirectorsJson("[]");
        }

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));

        // Mock tmdbService to return a list including movies that should be filtered
        // Ensure these are the same movie instances (or at least movies with the same IDs) as those in user's lists
        List<Movie> allMoviesFromTMDb = new ArrayList<>(Arrays.asList(testMovie1, testMovie2, testMovie3, watchedMovie, watchlistMovie));
        when(tmdbService.searchMovies(any(Movie.class))).thenReturn(allMoviesFromTMDb);

        // Call the method under test
        List<Movie> suggestions = movieService.getMovieSuggestions(testUser.getUserId(), 10);

        // Verify suggestions do not contain watched or watchlist movies
        assertNotNull(suggestions);
        assertFalse(suggestions.stream().anyMatch(m -> Objects.equals(m.getMovieId(), watchedMovie.getMovieId())), "Watched movie should be filtered out");
        assertFalse(suggestions.stream().anyMatch(m -> Objects.equals(m.getMovieId(), watchlistMovie.getMovieId())), "Watchlist movie should be filtered out");

        // Verify other movies are included
        assertTrue(suggestions.stream().anyMatch(m -> Objects.equals(m.getMovieId(), testMovie1.getMovieId())), "TestMovie1 should be present if not filtered");
        assertTrue(suggestions.stream().anyMatch(m -> Objects.equals(m.getMovieId(), testMovie2.getMovieId())), "TestMovie2 should be present if not filtered");
        assertTrue(suggestions.stream().anyMatch(m -> Objects.equals(m.getMovieId(), testMovie3.getMovieId())), "TestMovie3 should be present if not filtered");
        assertEquals(3, suggestions.size(), "Should return 3 movies after filtering");
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
        List<Long> actors = Arrays.asList(6193L, 24045L);
        List<Long> directors = Arrays.asList(525L);

        // Create spy on movieService to test private method
        MovieService spyMovieService = spy(movieService);

        // Use reflection making the private method accessible
        List<Movie> permutations = null;
        try {
            java.lang.reflect.Method method = MovieService.class.getDeclaredMethod(
                    "generateSearchPermutations", List.class, List.class, List.class);
            method.setAccessible(true);
            Object result = method.invoke(spyMovieService, genres, actors, directors);

            // Type-safe check before casting
            if (result instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Movie> castedResult = (List<Movie>) result;
                permutations = castedResult;
            } else {
                fail("Method did not return a List");
            }
        } catch (Exception e) {
            fail("Failed to call private method: " + e.getMessage());
        }

        // Verify the permutations
        assertNotNull(permutations);
        assertFalse(permutations.isEmpty());

        // First permutation should have all parameters (most specific)
        Movie firstPermutation = permutations.get(0);
        assertEquals(genres, firstPermutation.getGenres());
        assertEquals(actors.stream().map(String::valueOf).collect(Collectors.toList()), firstPermutation.getActors());
        assertEquals(directors.stream().map(String::valueOf).collect(Collectors.toList()), firstPermutation.getDirectors());

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

        // Configure testUser with some preferences for this test.
        testUser.setFavoriteGenres(Arrays.asList("Comedy")); // Add a favorite genre
        testUser.setFavoriteActorsJson("[]"); 
        testUser.setFavoriteDirectorsJson("[]");
        testUser.setWatchlist(new ArrayList<>());
        testUser.setWatchedMovies(new ArrayList<>());

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));

        // Create a large number of test movies
        List<Movie> manyMovies = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Movie movie = new Movie();
            movie.setMovieId(100L + i);
            movie.setTitle("Test Movie " + i);
            manyMovies.add(movie);
        }

        // Mock tmdbService.searchMovies to return many movies
        when(tmdbService.searchMovies(any(Movie.class))).thenReturn(manyMovies);

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
     * Test 1.6 - Testing when user has no favorites
     * This test verifies that the method handles users with no favorites correctly
     */
    @Test
    void testGetMovieSuggestions_noUserFavorites() {
        // test for users with no favorites

        // Create user with no favorites
        User userNoPrefs = new User();
        userNoPrefs.setUserId(2L);
        userNoPrefs.setFavoriteGenres(Collections.emptyList()); // No favorite genres
        userNoPrefs.setFavoriteActorsJson("[]"); // Empty JSON array for actors
        userNoPrefs.setFavoriteDirectorsJson("[]"); // Empty JSON array for directors
        userNoPrefs.setWatchlist(Collections.emptyList());
        userNoPrefs.setWatchedMovies(Collections.emptyList());

        // Mock repository to return this user
        when(userRepository.findById(userNoPrefs.getUserId())).thenReturn(Optional.of(userNoPrefs));

        // Mock tmdbService to return movies for empty search params
        // (i.e., when Movie object has no genres, actors, or directors specified)
        List<Movie> defaultMovies = Arrays.asList(testMovie1, testMovie2, testMovie3);
        when(tmdbService.searchMovies(argThat(movie ->
                (movie.getGenres() == null || movie.getGenres().isEmpty()) &&
                        (movie.getActors() == null || movie.getActors().isEmpty()) &&
                        (movie.getDirectors() == null || movie.getDirectors().isEmpty())
        ))).thenReturn(defaultMovies);

        // Call the method under test
        List<Movie> suggestions = movieService.getMovieSuggestions(userNoPrefs.getUserId(), 10);

        // Verify tmdbService was called with effectively empty search parameters
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

class MovieServiceSearchPermutationsTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    private MovieService movieService;
    private Method generateSearchPermutationsMethod;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create the MovieService instance with mocked dependencies
        movieService = new MovieService(movieRepository, null, userRepository);

        // Use reflection to access the private method
        generateSearchPermutationsMethod = MovieService.class.getDeclaredMethod(
                "generateSearchPermutations",
                List.class, List.class, List.class
        );
        generateSearchPermutationsMethod.setAccessible(true);
    }

    /**
     * Test 2.1: Test with various combinations of input parameters
     */
    @Test
    public void testGenerateSearchPermutationsWithVariousCombinations() throws Exception {
        // Define test input
        List<String> genres = Arrays.asList("Action", "Adventure");
        List<Long> actorIds = Arrays.asList(123L, 456L);
        List<Long> directorIds = Arrays.asList(789L);

        // Call the private method via reflection
        @SuppressWarnings("unchecked")
        List<Movie> result = (List<Movie>) generateSearchPermutationsMethod.invoke(
                movieService, genres, actorIds, directorIds
        );

        // Assertions for various combinations Verify that all expected combinations are generated
        assertFalse(result.isEmpty(), "Search permutations should not be empty");

        // Check if the most specific search (all parameters) is first
        Movie firstSearch = result.get(0);
        assertEquals(genres, firstSearch.getGenres(), "First search should have all genres");
        assertEquals(actorIds.stream().map(String::valueOf).collect(Collectors.toList()), firstSearch.getActors(), "First search should have all actors");
        assertEquals(directorIds.stream().map(String::valueOf).collect(Collectors.toList()), firstSearch.getDirectors(), "First search should have all directors");

        // Verify total number of permutations
        // 1 (all params) + 3 (two params) + 3 (single category) + 2 (individual genres) + 2 (individual actors) + 1 (individual director)
        assertEquals(12, result.size(), "Should generate correct number of permutations");

        // Verify some specific permutations exist (sampling a few)
        boolean foundGenresOnly = false;
        boolean foundActorsOnly = false;
        boolean foundDirectorsOnly = false;

        for (Movie search : result) {
            if (!search.getGenres().isEmpty() && search.getActors().isEmpty() && search.getDirectors().isEmpty()) {
                foundGenresOnly = true;
            }
            if (search.getGenres().isEmpty() && !search.getActors().isEmpty() && search.getDirectors().isEmpty()) {
                foundActorsOnly = true;
            }
            if (search.getGenres().isEmpty() && search.getActors().isEmpty() && !search.getDirectors().isEmpty()) {
                foundDirectorsOnly = true;
            }
        }

        assertTrue(foundGenresOnly, "Should include genres-only search");
        assertTrue(foundActorsOnly, "Should include actors-only search");
        assertTrue(foundDirectorsOnly, "Should include directors-only search");
    }

    /**
     * Test 2.2: Verify the permutations are generated in the correct order (most specific to least)
     */
    @Test
    public void testSearchPermutationsOrder() throws Exception {
        // Define test input
        List<String> genres = Arrays.asList("Action", "Adventure");
        List<Long> actorIds = Arrays.asList(123L, 456L);
        List<Long> directorIds = Arrays.asList(789L);

        // Call the private method via reflection
        @SuppressWarnings("unchecked")
        List<Movie> result = (List<Movie>) generateSearchPermutationsMethod.invoke(
                movieService, genres, actorIds, directorIds
        );

        // Verify the order is from most specific to least specific First should have all parameters
        Movie firstSearch = result.get(0);
        assertFalse(firstSearch.getGenres().isEmpty());
        assertFalse(firstSearch.getActors().isEmpty());
        assertFalse(firstSearch.getDirectors().isEmpty());

        // Check that single parameter searches come after multi-parameter searches
        // (This assumes the implementation follows the ordering: all params, 2 params, 1 param, individual items)
        boolean foundAllDualParamBeforeSingleParam = true;
        boolean foundSingleParam = false;

        for (int i = 1; i < 4; i++) { // Next 3 should be dual-parameter searches
            Movie search = result.get(i);
            int paramCount = 0;
            if (!search.getGenres().isEmpty()) paramCount++;
            if (!search.getActors().isEmpty()) paramCount++;
            if (!search.getDirectors().isEmpty()) paramCount++;

            if (paramCount != 2) {
                foundAllDualParamBeforeSingleParam = false;
                break;
            }
        }

        for (int i = 4; i < 7; i++) { // Next 3 should be single-category searches
            Movie search = result.get(i);
            int paramCount = 0;
            if (!search.getGenres().isEmpty()) paramCount++;
            if (!search.getActors().isEmpty()) paramCount++;
            if (!search.getDirectors().isEmpty()) paramCount++;

            if (paramCount == 1) {
                foundSingleParam = true;
            }
        }

        assertTrue(foundAllDualParamBeforeSingleParam, "Dual-parameter searches should come before single-parameter searches");
        assertTrue(foundSingleParam, "Should include single-parameter searches after dual-parameter searches");
    }

    /**
     * Test 2.3: Check edge cases like empty input lists
     */
    @Test
    public void testGenerateSearchPermutationsWithEmptyLists() throws Exception {
        // Call with empty lists
        @SuppressWarnings("unchecked")
        List<Movie> result = (List<Movie>) generateSearchPermutationsMethod.invoke(
                movieService,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        // Verify that an empty list is returned when all inputs are empty
        assertTrue(result.isEmpty(), "Should return empty list when all inputs are empty");

        // Test with only one non-empty list
        @SuppressWarnings("unchecked")
        List<Movie> resultWithGenres = (List<Movie>) generateSearchPermutationsMethod.invoke(
                movieService,
                Arrays.asList("Action"),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertEquals("Action", resultWithGenres.get(0).getGenres().get(0), "Should include the provided genre");
    }

    /**
     * Test 2.4:  test to verify individual item permutations
     */
    @Test
    public void testIndividualItemPermutations() throws Exception {
        // Setup multiple items in each category
        List<String> genres = Arrays.asList("Action", "Adventure", "Comedy");
        List<Long> actorIds = Arrays.asList(123L, 456L);
        List<Long> directorIds = Arrays.asList(789L, 12L); // 012L is 10 in octal, 12L is clearer for decimal 12

        // Call the method
        @SuppressWarnings("unchecked")
        List<Movie> result = (List<Movie>) generateSearchPermutationsMethod.invoke(
                movieService, genres, actorIds, directorIds
        );

        // Verify individual item permutations are included Count individual genre permutations
        int individualGenreCount = 0;
        for (Movie search : result) {
            if (search.getGenres() != null && search.getGenres().size() == 1 &&
                    search.getActors().isEmpty() && search.getDirectors().isEmpty()) {
                individualGenreCount++;
            }
        }
        assertEquals(3, individualGenreCount, "Should have a permutation for each individual genre");

        // Count individual actor permutations
        int individualActorCount = 0;
        for (Movie search : result) {
            if (search.getGenres().isEmpty() &&
                    search.getActors() != null && search.getActors().size() == 1 &&
                    search.getDirectors().isEmpty()) {
                individualActorCount++;
            }
        }
        assertEquals(2, individualActorCount, "Should have a permutation for each individual actor");

        // Count individual director permutations
        int individualDirectorCount = 0;
        for (Movie search : result) {
            if (search.getGenres().isEmpty() && search.getActors().isEmpty() &&
                    search.getDirectors() != null && search.getDirectors().size() == 1) {
                individualDirectorCount++;
            }
        }
        assertEquals(2, individualDirectorCount, "Should have a permutation for each individual director");
    }
}
