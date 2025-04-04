package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class UserMovieServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieService movieService;

    @InjectMocks
    private UserMovieService userMovieService;

    private User testUser;
    private Movie testMovie;
    private String validToken = "valid-token";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Create test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken(validToken);
        
        // Create test movie
        testMovie = new Movie();
        testMovie.setMovieId(1L);
        testMovie.setTitle("Test Movie");
        
        // Mock repository behaviors
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByToken(validToken)).thenReturn(testUser);
        when(movieService.getMovieById(testMovie.getMovieId())).thenReturn(testMovie);
        when(movieService.saveMovie(any(Movie.class))).thenReturn(testMovie);
    }

    @Test
    public void getWatchlist_userExists_returnsWatchlist() {
        // Given
        List<Movie> mockWatchlist = new ArrayList<>();
        mockWatchlist.add(testMovie);
        testUser.setWatchlist(mockWatchlist);

        // When
        List<Movie> watchlist = userMovieService.getWatchlist(testUser.getUserId());

        // Then
        assertEquals(1, watchlist.size());
        assertEquals(testMovie.getMovieId(), watchlist.get(0).getMovieId());
    }

    @Test
    public void getWatchlist_emptyWatchlist_returnsEmptyList() {
        // Given
        testUser.setWatchlist(null);

        // When
        List<Movie> watchlist = userMovieService.getWatchlist(testUser.getUserId());

        // Then
        assertNotNull(watchlist);
        assertTrue(watchlist.isEmpty());
    }

    @Test
    public void addToWatchlist_validRequest_addsMovie() {
        // Given
        testUser.setWatchlist(new ArrayList<>());

        // When
        List<Movie> updatedWatchlist = userMovieService.addToWatchlist(
                testUser.getUserId(), testMovie.getMovieId(), validToken);

        // Then
        assertEquals(1, updatedWatchlist.size());
        assertEquals(testMovie.getMovieId(), updatedWatchlist.get(0).getMovieId());
        Mockito.verify(userRepository).save(testUser);
    }

    @Test
    public void addToWatchlist_duplicateMovie_throwsException() {
        // Given
        List<Movie> existingWatchlist = new ArrayList<>();
        existingWatchlist.add(testMovie);
        testUser.setWatchlist(existingWatchlist);

        // When/Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userMovieService.addToWatchlist(testUser.getUserId(), testMovie.getMovieId(), validToken)
        );
        
        assertTrue(exception.getMessage().contains("already in your watchlist"));
    }

    @Test
    public void addToWatchlist_unauthorizedUser_throwsException() {
        // Given
        String invalidToken = "invalid-token";
        when(userRepository.findByToken(invalidToken)).thenReturn(null);

        // When/Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userMovieService.addToWatchlist(testUser.getUserId(), testMovie.getMovieId(), invalidToken)
        );
        
        assertTrue(exception.getMessage().contains("Invalid token"));
    }

    @Test
    public void removeFromWatchlist_existingMovie_removesMovie() {
        // Given
        List<Movie> existingWatchlist = new ArrayList<>();
        existingWatchlist.add(testMovie);
        testUser.setWatchlist(existingWatchlist);

        // When
        List<Movie> updatedWatchlist = userMovieService.removeFromWatchlist(
                testUser.getUserId(), testMovie.getMovieId(), validToken);

        // Then
        assertTrue(updatedWatchlist.isEmpty());
        Mockito.verify(userRepository).save(testUser);
    }

    @Test
    public void removeFromWatchlist_nonExistingMovie_throwsException() {
        // Given
        testUser.setWatchlist(new ArrayList<>());

        // When/Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userMovieService.removeFromWatchlist(testUser.getUserId(), testMovie.getMovieId(), validToken)
        );
        
        assertTrue(exception.getMessage().contains("Watchlist is empty"));
    }

    @Test
    public void getWatchedMovies_userExists_returnsWatchedMovies() {
        // Given
        List<Movie> mockWatchedMovies = new ArrayList<>();
        mockWatchedMovies.add(testMovie);
        testUser.setWatchedMovies(mockWatchedMovies);

        // When
        List<Movie> watchedMovies = userMovieService.getWatchedMovies(testUser.getUserId());

        // Then
        assertEquals(1, watchedMovies.size());
        assertEquals(testMovie.getMovieId(), watchedMovies.get(0).getMovieId());
    }

    @Test
    public void addToWatchedMovies_validRequest_addsMovie() {
        // Given
        testUser.setWatchedMovies(new ArrayList<>());
        testUser.setWatchlist(new ArrayList<>());

        // When
        List<Movie> updatedWatchedMovies = userMovieService.addToWatchedMovies(
                testUser.getUserId(), testMovie.getMovieId(), validToken);

        // Then
        assertEquals(1, updatedWatchedMovies.size());
        assertEquals(testMovie.getMovieId(), updatedWatchedMovies.get(0).getMovieId());
        Mockito.verify(userRepository).save(testUser);
    }

    @Test
    public void addToWatchedMovies_duplicateMovie_throwsException() {
        // Given
        List<Movie> existingWatchedMovies = new ArrayList<>();
        existingWatchedMovies.add(testMovie);
        testUser.setWatchedMovies(existingWatchedMovies);

        // When/Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userMovieService.addToWatchedMovies(testUser.getUserId(), testMovie.getMovieId(), validToken)
        );
        
        assertTrue(exception.getMessage().contains("already in your watched movies"));
    }

    @Test
    public void removeFromWatchedMovies_existingMovie_removesMovie() {
        // Given
        List<Movie> existingWatchedMovies = new ArrayList<>();
        existingWatchedMovies.add(testMovie);
        testUser.setWatchedMovies(existingWatchedMovies);

        // When
        List<Movie> updatedWatchedMovies = userMovieService.removeFromWatchedMovies(
                testUser.getUserId(), testMovie.getMovieId(), validToken);

        // Then
        assertTrue(updatedWatchedMovies.isEmpty());
        Mockito.verify(userRepository).save(testUser);
    }

    @Test
    public void removeFromWatchedMovies_nonExistingMovie_throwsException() {
        // Given
        testUser.setWatchedMovies(new ArrayList<>());

        // When/Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userMovieService.removeFromWatchedMovies(testUser.getUserId(), testMovie.getMovieId(), validToken)
        );
        
        assertTrue(exception.getMessage().contains("Watched movies list is empty"));
    }
}