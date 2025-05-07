package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

class UserFavoritesServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieService movieService;

    @Mock
    private TMDbService tmdbService;

    private UserFavoritesService UserFavoritesService;

    private User testUser;
    private Movie testMovie;
    private JsonNode genresNode;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        UserFavoritesService = new UserFavoritesService(userRepository, movieService, tmdbService, objectMapper);

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
        String genresJson = "[{\"id\":28,\"name\":\"Action\"},{\"id\":12,\"name\":\"Adventure\"},{\"id\":16,\"name\":\"Animation\"}]";
        genresNode = objectMapper.readTree(genresJson);

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
    void saveFavoriteActors_SavesActors() throws JsonProcessingException {
        // Arrange
        ActorDTO actor1 = new ActorDTO();
        actor1.setId(1);
        actor1.setName("Actor One");
        ActorDTO actor2 = new ActorDTO();
        actor2.setId(2);
        actor2.setName("Actor Two");
        List<ActorDTO> actors = List.of(actor1, actor2);
        String expectedJson = objectMapper.writeValueAsString(actors);

        // Act
        List<ActorDTO> result = UserFavoritesService.saveFavoriteActors(1L, actors, "validToken");

        // Assert
        assertEquals(actors.size(), result.size());
        for (int i = 0; i < actors.size(); i++) {
            assertEquals(actors.get(i).getId(), result.get(i).getId());
            assertEquals(actors.get(i).getName(), result.get(i).getName());
        }
        
        // Verify that the user entity was updated with the JSON string
        verify(userRepository).save(testUser); 
        assertEquals(expectedJson, testUser.getFavoriteActorsJson());
    }

    @Test
    void saveFavoriteActors_ThrowsForInvalidToken() {
        // Arrange
        ActorDTO actor1 = new ActorDTO();
        actor1.setId(1);
        actor1.setName("Actor One");
        List<ActorDTO> actors = List.of(actor1);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            UserFavoritesService.saveFavoriteActors(1L, actors, "invalidToken");
        });
    }

    @Test
    void getFavoriteActors_ReturnsEmptyListIfNotSet() {
        // Arrange
        testUser.setFavoriteActorsJson(null); 

        // Act
        List<ActorDTO> result = UserFavoritesService.getFavoriteActors(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFavoriteActors_ReturnsSavedActors() throws JsonProcessingException {
        // Arrange
        ActorDTO actor1 = new ActorDTO();
        actor1.setId(1);
        actor1.setName("Actor One");
        ActorDTO actor2 = new ActorDTO();
        actor2.setId(2);
        actor2.setName("Actor Two");
        List<ActorDTO> expectedActors = List.of(actor1, actor2);
        String actorsJson = objectMapper.writeValueAsString(expectedActors);
        testUser.setFavoriteActorsJson(actorsJson);

        // Act
        List<ActorDTO> result = UserFavoritesService.getFavoriteActors(1L);

        // Assert
        assertNotNull(result);
        assertEquals(expectedActors.size(), result.size());
        for (int i = 0; i < expectedActors.size(); i++) {
            assertEquals(expectedActors.get(i).getId(), result.get(i).getId());
            assertEquals(expectedActors.get(i).getName(), result.get(i).getName());
        }
    }

    @Test
    void saveFavoriteDirectors_SavesDirectors() throws JsonProcessingException {
        // Arrange
        DirectorDTO director1 = new DirectorDTO();
        director1.setId(1);
        director1.setName("Director One");
        DirectorDTO director2 = new DirectorDTO();
        director2.setId(2);
        director2.setName("Director Two");
        List<DirectorDTO> directors = List.of(director1, director2);
        String expectedJson = objectMapper.writeValueAsString(directors);

        // Act
        List<DirectorDTO> result = UserFavoritesService.saveFavoriteDirectors(1L, directors, "validToken");

        // Assert
        assertEquals(directors.size(), result.size());
        for (int i = 0; i < directors.size(); i++) {
            assertEquals(directors.get(i).getId(), result.get(i).getId());
            assertEquals(directors.get(i).getName(), result.get(i).getName());
        }

        // Verify that the user entity was updated with the JSON string
        verify(userRepository).save(testUser);
        assertEquals(expectedJson, testUser.getFavoriteDirectorsJson());
    }

    @Test
    void saveFavoriteDirectors_ThrowsForInvalidToken() {
        // Arrange
        DirectorDTO director1 = new DirectorDTO();
        director1.setId(1);
        director1.setName("Director One");
        List<DirectorDTO> directors = List.of(director1);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            UserFavoritesService.saveFavoriteDirectors(1L, directors, "invalidToken");
        });
    }

    @Test
    void getFavoriteDirectors_ReturnsEmptyListIfNotSet() {
        // Arrange
        testUser.setFavoriteDirectorsJson(null); 

        // Act
        List<DirectorDTO> result = UserFavoritesService.getFavoriteDirectors(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFavoriteDirectors_ReturnsSavedDirectors() throws JsonProcessingException {
        // Arrange
        DirectorDTO director1 = new DirectorDTO();
        director1.setId(1);
        director1.setName("Director One");
        DirectorDTO director2 = new DirectorDTO();
        director2.setId(2);
        director2.setName("Director Two");
        List<DirectorDTO> expectedDirectors = List.of(director1, director2);
        String directorsJson = objectMapper.writeValueAsString(expectedDirectors);
        testUser.setFavoriteDirectorsJson(directorsJson);

        // Act
        List<DirectorDTO> result = UserFavoritesService.getFavoriteDirectors(1L);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDirectors.size(), result.size());
        for (int i = 0; i < expectedDirectors.size(); i++) {
            assertEquals(expectedDirectors.get(i).getId(), result.get(i).getId());
            assertEquals(expectedDirectors.get(i).getName(), result.get(i).getName());
        }
    }
}