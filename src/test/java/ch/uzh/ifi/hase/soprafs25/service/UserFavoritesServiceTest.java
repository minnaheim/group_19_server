package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class UserFavoritesServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieService movieService;

    @Mock
    private TMDbService tmdbService;

    @InjectMocks
    private UserFavoritesService UserFavoritesService;

    private User testUser;
    private Movie testMovie;
    private JsonNode genresNode;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Set up test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("validToken");

        // Set up test movie
        testMovie = new Movie();
        testMovie.setMovieId(123L);
        testMovie.setTitle("Test Movie");
        
        // Set up genres data
        ObjectMapper mapper = new ObjectMapper();
        String genresJson = "[{\"id\":28,\"name\":\"Action\"},{\"id\":12,\"name\":\"Adventure\"},{\"id\":16,\"name\":\"Animation\"}]";
        genresNode = mapper.readTree(genresJson);

        // Configure mocks
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByToken("validToken")).thenReturn(testUser);
        when(movieService.getMovieById(123L)).thenReturn(testMovie);
        when(movieService.saveMovie(any(Movie.class))).thenReturn(testMovie);
        when(tmdbService.getGenres()).thenReturn(genresNode);
    }

    @Test
    void getAllGenres_ReturnsListOfGenres() {
        // Act
        List<Map<String, Object>> genres = UserFavoritesService.getAllGenres();

        // Assert
        assertEquals(3, genres.size());
        assertEquals(28, genres.get(0).get("id"));
        assertEquals("Action", genres.get(0).get("name"));
    }

    @Test
    void saveGenreFavorites_SavesGenres() {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");

        // Act
        List<String> result = UserFavoritesService.saveGenreFavorites(1L, genreNames, "validToken");

        // Assert
        assertEquals(genreNames, result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void saveGenreFavorites_ThrowsForInvalidToken() {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            UserFavoritesService.saveGenreFavorites(1L, genreNames, "invalidToken");
        });
    }

    @Test
    void getGenreFavorites_ReturnsEmptyListIfNotSet() {
        // Arrange
        testUser.setFavoriteGenres(null);

        // Act
        List<String> result = UserFavoritesService.getGenreFavorites(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getGenreFavorites_ReturnsSavedGenres() {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        testUser.setFavoriteGenres(genreNames);

        // Act
        List<String> result = UserFavoritesService.getGenreFavorites(1L);

        // Assert
        assertEquals(genreNames, result);
    }

    @Test
    void saveFavoriteMovie_SavesMovie() {
        // Act
        Movie result = UserFavoritesService.saveFavoriteMovie(1L, 123L, "validToken");

        // Assert
        assertEquals(testMovie.getMovieId(), result.getMovieId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void saveFavoriteMovie_ThrowsForInvalidToken() {
        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            UserFavoritesService.saveFavoriteMovie(1L, 123L, "invalidToken");
        });
    }

    @Test
    void getFavoriteMovie_ReturnsNullIfNotSet() {
        // Arrange
        testUser.setFavoriteMovie(null);

        // Act
        Movie result = UserFavoritesService.getFavoriteMovie(1L);

        // Assert
        assertNull(result);
    }

    @Test
    void getFavoriteMovie_ReturnsSavedMovie() {
        // Arrange
        testUser.setFavoriteMovie(testMovie);

        // Act
        Movie result = UserFavoritesService.getFavoriteMovie(1L);

        // Assert
        assertEquals(testMovie, result);
    }

    @Test
    void saveFavoriteActors_SavesActors() {
        List<String> actors = List.of("A1", "B2");
        List<String> result = UserFavoritesService.saveFavoriteActors(1L, actors, "validToken");
        assertEquals(actors, result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void saveFavoriteActors_ThrowsForInvalidToken() {
        List<String> actors = List.of("A1", "B2");
        assertThrows(ResponseStatusException.class, () -> {
            UserFavoritesService.saveFavoriteActors(1L, actors, "invalidToken");
        });
    }

    @Test
    void getFavoriteActors_ReturnsEmptyListIfNotSet() {
        testUser.setFavoriteActors((List<String>) null);
        List<String> result = UserFavoritesService.getFavoriteActors(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFavoriteActors_ReturnsSavedActors() {
        List<String> actors = List.of("A1", "B2");
        testUser.setFavoriteActors(actors);
        List<String> result = UserFavoritesService.getFavoriteActors(1L);
        assertEquals(actors, result);
    }

    @Test
    void saveFavoriteDirectors_SavesDirectors() {
        List<String> dirs = List.of("D1", "D2");
        List<String> result = UserFavoritesService.saveFavoriteDirectors(1L, dirs, "validToken");
        assertEquals(dirs, result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void saveFavoriteDirectors_ThrowsForInvalidToken() {
        List<String> dirs = List.of("D1", "D2");
        assertThrows(ResponseStatusException.class, () -> {
            UserFavoritesService.saveFavoriteDirectors(1L, dirs, "invalidToken");
        });
    }

    @Test
    void getFavoriteDirectors_ReturnsEmptyListIfNotSet() {
        testUser.setFavoriteDirectors((List<String>) null);
        List<String> result = UserFavoritesService.getFavoriteDirectors(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFavoriteDirectors_ReturnsSavedDirectors() {
        List<String> dirs = List.of("D1", "D2");
        testUser.setFavoriteDirectors(dirs);
        List<String> result = UserFavoritesService.getFavoriteDirectors(1L);
        assertEquals(dirs, result);
    }
}