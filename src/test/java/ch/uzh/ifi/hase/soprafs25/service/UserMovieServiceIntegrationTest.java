package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the UserMovieService
 * This is an integration test that makes sure the backend logic works correctly
 */
@WebAppConfiguration
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserMovieServiceIntegrationTest {

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Qualifier("movieRepository")
    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserMovieService userMovieService;

    @Autowired
    private MovieService movieService;

    private User testUser;
    private User otherUser;
    private Movie testMovie;

    @BeforeEach
    public void setup() {
        // Delete all existing data
        userRepository.deleteAll();
        movieRepository.deleteAll();

        // Create test movie
        testMovie = new Movie();
        testMovie.setMovieId(12345L);
        testMovie.setTitle("Test Movie");
        testMovie.setYear(2023);
        testMovie.setGenres(java.util.Collections.singletonList("Action"));
        testMovie.setDescription("A test movie description");
        testMovie.setTrailerURL("www.youtube.com/watch?v=dQw4w9WgXcQ");
        movieRepository.save(testMovie);

        // Create test users
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("test-token");
        userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("otherUser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setStatus(UserStatus.ONLINE);
        otherUser.setToken("other-token");
        userRepository.save(otherUser);
    }

    @Test
    public void getWatchlist_emptyWatchlist_returnsEmptyList() {
        // Empty watchlist should return empty list, not null
        List<Movie> watchlist = userMovieService.getWatchlist(testUser.getUserId());
        assertNotNull(watchlist);
        assertTrue(watchlist.isEmpty());
    }

    @Test
    public void addToWatchlist_validRequest_addsMovie() {
        // Add movie to watchlist
        List<Movie> updatedWatchlist = userMovieService.addToWatchlist(
                testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Verify movie was added
        assertEquals(1, updatedWatchlist.size());
        assertEquals(testMovie.getMovieId(), updatedWatchlist.get(0).getMovieId());

        // Verify changes were persisted
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertEquals(1, updatedUser.getWatchlist().size());
        assertEquals(testMovie.getMovieId(), updatedUser.getWatchlist().get(0).getMovieId());
    }

    @Test
    public void addToWatchlist_duplicateMovie_throwsException() {
        // First add movie to watchlist
        userMovieService.addToWatchlist(testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Try to add the same movie again
        assertThrows(ResponseStatusException.class, () -> userMovieService.addToWatchlist(testUser.getUserId(),
                testMovie.getMovieId(), testUser.getToken()));
    }

    @Test
    public void addToWatchlist_unauthorizedUser_throwsException() {
        // Try to add movie to another user's watchlist
        assertThrows(ResponseStatusException.class, () -> userMovieService.addToWatchlist(testUser.getUserId(),
                testMovie.getMovieId(), otherUser.getToken()));
    }

    @Test
    public void removeFromWatchlist_existingMovie_removesMovie() {
        // First add movie to watchlist
        userMovieService.addToWatchlist(testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Then remove it
        List<Movie> updatedWatchlist = userMovieService.removeFromWatchlist(
                testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Verify movie was removed
        assertTrue(updatedWatchlist.isEmpty());

        // Verify changes were persisted
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertTrue(updatedUser.getWatchlist().isEmpty());
    }

    @Test
    public void removeFromWatchlist_nonExistingMovie_throwsException() {
        // Try to remove a movie that's not in the watchlist
        assertThrows(ResponseStatusException.class, () -> userMovieService.removeFromWatchlist(testUser.getUserId(),
                testMovie.getMovieId(), testUser.getToken()));
    }

    @Test
    public void addToWatchedMovies_validRequest_addsMovie() {
        // Add movie to watched movies
        List<Movie> updatedWatchedMovies = userMovieService.addToWatchedMovies(
                testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Verify movie was added
        assertEquals(1, updatedWatchedMovies.size());
        assertEquals(testMovie.getMovieId(), updatedWatchedMovies.get(0).getMovieId());

        // Verify changes were persisted
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertEquals(1, updatedUser.getWatchedMovies().size());
        assertEquals(testMovie.getMovieId(), updatedUser.getWatchedMovies().get(0).getMovieId());
    }

    @Test
    public void addToWatchedMovies_movieInWatchlist_removesFromWatchlist() {
        // First add movie to watchlist
        userMovieService.addToWatchlist(testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Then add to watched movies
        userMovieService.addToWatchedMovies(testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Verify movie was removed from watchlist
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertTrue(updatedUser.getWatchlist().isEmpty());
        assertEquals(1, updatedUser.getWatchedMovies().size());
    }

    @Test
    public void removeFromWatchedMovies_existingMovie_removesMovie() {
        // First add movie to watched movies
        userMovieService.addToWatchedMovies(testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Then remove it
        List<Movie> updatedWatchedMovies = userMovieService.removeFromWatchedMovies(
                testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Verify movie was removed
        assertTrue(updatedWatchedMovies.isEmpty());

        // Verify changes were persisted
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertTrue(updatedUser.getWatchedMovies().isEmpty());
    }
}