package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@WebAppConfiguration
@SpringBootTest
@Transactional
class UserPreferencesServiceIntegrationTest {

    @Autowired
    private UserPreferencesService userPreferencesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieService movieService;
    
    @MockBean
    private TMDbService tmdbService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private Movie testMovie;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        movieRepository.deleteAll();
        
        // Create test user
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("token123");
        
        userRepository.save(testUser);
        
        // Create test movie
        testMovie = new Movie();
        testMovie.setMovieId(555L);
        testMovie.setTitle("Test Movie");
        testMovie.setDescription("Test description");
        testMovie.setTrailerURL("youtube.com/watch?v=testTrailer");
        
        movieRepository.save(testMovie);
        
        // Configure Mock TMDbService
        ArrayNode genresArray = objectMapper.createArrayNode();

        ObjectNode actionNode = objectMapper.createObjectNode();
        actionNode.put("id", 28);
        actionNode.put("name", "Action");
        genresArray.add(actionNode);

        ObjectNode dramaNode = objectMapper.createObjectNode();
        dramaNode.put("id", 18);
        dramaNode.put("name", "Drama");
        genresArray.add(dramaNode);
        
        ObjectNode comedyNode = objectMapper.createObjectNode();
        comedyNode.put("id", 35); 
        comedyNode.put("name", "Comedy");
        genresArray.add(comedyNode);

        // Mock the TMDbService.getGenres() method to return our controlled genresArray
        when(tmdbService.getGenres()).thenReturn(genresArray);
    }

    @Test
    void saveGenrePreferences_WithValidToken_SavesPreferences() {
        // Arrange
        // Use exact genre names defined in the mock setup
        List<String> genreNames = new ArrayList<>(List.of("Action", "Drama"));

        // Act
        List<String> result = userPreferencesService.saveGenrePreferences(
                testUser.getUserId(), genreNames, testUser.getToken());

        // Assert
        assertEquals(genreNames, result);
        
        // Verify through repository
        User updatedUser = userRepository.findById(testUser.getUserId()).get();
        assertNotNull(updatedUser.getFavoriteGenres());
        assertEquals(genreNames.size(), updatedUser.getFavoriteGenres().size());
        assertTrue(updatedUser.getFavoriteGenres().containsAll(genreNames));
        assertTrue(genreNames.containsAll(updatedUser.getFavoriteGenres()));
    }

    @Test
    void saveGenrePreferences_WithInvalidToken_ThrowsException() {
        // Arrange
        List<String> genreNames = List.of("Action", "Drama");

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            userPreferencesService.saveGenrePreferences(
                    testUser.getUserId(), genreNames, "invalidToken");
        });
    }

    @Test
    void getGenrePreferences_ReturnsCorrectPreferences() {
        // Arrange
        List<String> genreNames = new ArrayList<>(List.of("Action", "Comedy"));
        testUser.setFavoriteGenres(genreNames);
        userRepository.save(testUser);

        // Act
        List<String> result = userPreferencesService.getGenrePreferences(testUser.getUserId());

        // Assert
        assertEquals(genreNames, result);
    }

    @Test
    void saveFavoriteMovie_WithValidToken_SavesMovie() {
        // Act
        Movie result = userPreferencesService.saveFavoriteMovie(
                testUser.getUserId(), testMovie.getMovieId(), testUser.getToken());

        // Assert
        assertEquals(testMovie.getMovieId(), result.getMovieId());
        
        // Verify through repository
        User updatedUser = userRepository.findById(testUser.getUserId()).get();
        assertNotNull(updatedUser.getFavoriteMovie());
        assertEquals(testMovie.getMovieId(), updatedUser.getFavoriteMovie().getMovieId());
    }

    @Test
    void saveFavoriteMovie_WithInvalidToken_ThrowsException() {
        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            userPreferencesService.saveFavoriteMovie(
                    testUser.getUserId(), testMovie.getMovieId(), "invalidToken");
        });
    }

    @Test
    void getFavoriteMovie_ReturnsCorrectMovie() {
        // Arrange
        testUser.setFavoriteMovie(testMovie);
        userRepository.save(testUser);

        // Act
        Movie result = userPreferencesService.getFavoriteMovie(testUser.getUserId());

        // Assert
        assertNotNull(result);
        assertEquals(testMovie.getMovieId(), result.getMovieId());
    }
}