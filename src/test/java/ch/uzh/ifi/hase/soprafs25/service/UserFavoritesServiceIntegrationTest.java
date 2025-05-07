package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.stream.Collectors;

@WebAppConfiguration
@SpringBootTest
@Transactional
public class UserFavoritesServiceIntegrationTest {

    @Autowired
    private UserFavoritesService userFavoritesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @MockBean
    private TMDbService tmdbService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private Movie testMovie;

    @BeforeEach
    void setup() throws Exception {
        userRepository.deleteAll();
        movieRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("token123");

        // Setup Favorite Actors for testUser
        Map<String, String> favoriteActorsMap = new HashMap<>();
        favoriteActorsMap.put("1", "Actor A"); 
        favoriteActorsMap.put("2", "Actor B");
        List<ActorDTO> actorDTOs = new ArrayList<>();
        for (Map.Entry<String, String> entry : favoriteActorsMap.entrySet()) {
            ActorDTO actor = new ActorDTO();
            actor.setId(Integer.parseInt(entry.getKey()));
            actor.setName(entry.getValue());
            actorDTOs.add(actor);
        }
        testUser.setFavoriteActorsJson(objectMapper.writeValueAsString(actorDTOs));

        // Setup Favorite Directors for testUser
        Map<String, String> favoriteDirectorsMap = new HashMap<>();
        favoriteDirectorsMap.put("10", "Director X");
        List<DirectorDTO> directorDTOs = new ArrayList<>();
        for (Map.Entry<String, String> entry : favoriteDirectorsMap.entrySet()) {
            DirectorDTO director = new DirectorDTO();
            director.setId(Integer.parseInt(entry.getKey()));
            director.setName(entry.getValue());
            directorDTOs.add(director);
        }
        testUser.setFavoriteDirectorsJson(objectMapper.writeValueAsString(directorDTOs));

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
    void saveGenreFavorites_WithValidToken_SavesFavorites() {
        // Arrange
        // Use exact genre names defined in the mock setup
        List<String> genreNames = new ArrayList<>(List.of("Action", "Drama"));

        // Act
        List<String> result = userFavoritesService.saveGenreFavorites(
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
    void saveGenreFavorites_WithInvalidToken_ThrowsException() {
        // Arrange
        List<String> genreNames = List.of("Action", "Drama");

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            userFavoritesService.saveGenreFavorites(
                    testUser.getUserId(), genreNames, "invalidToken");
        });
    }

    @Test
    void getGenreFavorites_ReturnsCorrectFavorites() {
        // Arrange
        List<String> genreNames = new ArrayList<>(List.of("Action", "Comedy"));
        testUser.setFavoriteGenres(genreNames);
        userRepository.save(testUser);

        // Act
        List<String> result = userFavoritesService.getGenreFavorites(testUser.getUserId());

        // Assert
        assertEquals(genreNames, result);
    }

    @Test
    void saveFavoriteMovie_WithValidToken_SavesMovie() {
        // Act
        Movie result = userFavoritesService.saveFavoriteMovie(
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
            userFavoritesService.saveFavoriteMovie(
                    testUser.getUserId(), testMovie.getMovieId(), "invalidToken");
        });
    }

    @Test
    void getFavoriteMovie_ReturnsCorrectMovie() {
        // Arrange
        testUser.setFavoriteMovie(testMovie);
        userRepository.save(testUser);

        // Act
        Movie result = userFavoritesService.getFavoriteMovie(testUser.getUserId());

        // Assert
        assertNotNull(result);
        assertEquals(testMovie.getMovieId(), result.getMovieId());
    }

    @Test
    void saveFavoriteActors_WithValidToken_SavesActors() {
        // Arrange
        List<String> actorNamesToSave = new ArrayList<>(List.of("New Actor 1", "New Actor 2"));
        List<ActorDTO> actorDTOsToSave = actorNamesToSave.stream().map(name -> {
            ActorDTO dto = new ActorDTO();
            dto.setName(name);
            return dto;
        }).collect(Collectors.toList());

        // Act
        List<ActorDTO> resultActorDTOs = userFavoritesService.saveFavoriteActors(
                testUser.getUserId(), actorDTOsToSave, testUser.getToken());

        // Assert: service returns the DTOs of saved actors
        assertNotNull(resultActorDTOs);
        assertEquals(actorNamesToSave.size(), resultActorDTOs.size());
        List<String> resultActorNames = resultActorDTOs.stream().map(ActorDTO::getName).collect(Collectors.toList());
        assertTrue(resultActorNames.containsAll(actorNamesToSave) && actorNamesToSave.containsAll(resultActorNames));

        User updatedUser = userRepository.findById(testUser.getUserId()).get();
        assertNotNull(updatedUser.getFavoriteActorsJson());
        try {
            List<ActorDTO> parsedDTOs = objectMapper.readValue(updatedUser.getFavoriteActorsJson(), 
                objectMapper.getTypeFactory().constructCollectionType(List.class, ActorDTO.class));
            List<String> parsedNames = parsedDTOs.stream().map(ActorDTO::getName).collect(Collectors.toList());
            assertEquals(actorNamesToSave.size(), parsedNames.size());
            assertTrue(parsedNames.containsAll(actorNamesToSave) && actorNamesToSave.containsAll(parsedNames));
        } catch (JsonProcessingException e) {
            fail("Failed to parse favorite actors JSON: " + e.getMessage());
        }
    }

    @Test
    void saveFavoriteActors_WithInvalidToken_ThrowsException() {
        // Arrange
        List<String> actorNames = new ArrayList<>(List.of("A1", "B2"));
        List<ActorDTO> actorDTOsToSave = actorNames.stream().map(name -> {
            ActorDTO dto = new ActorDTO();
            dto.setName(name);
            return dto;
        }).collect(Collectors.toList());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            userFavoritesService.saveFavoriteActors(
                    testUser.getUserId(), actorDTOsToSave, "wrongToken");
        });
    }

    @Test
    void getFavoriteActors_ReturnsCorrectActors() throws JsonProcessingException { 
        // Arrange via service - save some actors first
        List<String> actorNamesToSetup = new ArrayList<>(List.of("Setup Actor X", "Setup Actor Y"));
        List<ActorDTO> actorDTOsToSetup = actorNamesToSetup.stream().map(name -> {
            ActorDTO dto = new ActorDTO();
            dto.setName(name);
            return dto;
        }).collect(Collectors.toList());
        userFavoritesService.saveFavoriteActors(testUser.getUserId(), actorDTOsToSetup, testUser.getToken());

        // Act
        List<ActorDTO> resultDTOs = userFavoritesService.getFavoriteActors(testUser.getUserId());
        List<String> resultNames = resultDTOs.stream().map(ActorDTO::getName).collect(Collectors.toList());

        // Assert
        assertEquals(actorNamesToSetup.size(), resultNames.size());
        assertTrue(resultNames.containsAll(actorNamesToSetup) && actorNamesToSetup.containsAll(resultNames));
    }

    @Test
    void saveFavoriteDirectors_WithValidToken_SavesDirectors() {
        // Arrange
        List<String> directorNamesToSave = new ArrayList<>(List.of("New Director 1", "New Director 2"));
        List<DirectorDTO> directorDTOsToSave = directorNamesToSave.stream().map(name -> {
            DirectorDTO dto = new DirectorDTO();
            dto.setName(name);
            return dto;
        }).collect(Collectors.toList());

        // Act
        List<DirectorDTO> resultDirectorDTOs = userFavoritesService.saveFavoriteDirectors(
                testUser.getUserId(), directorDTOsToSave, testUser.getToken());

        // Assert: service returns the DTOs of saved directors
        assertNotNull(resultDirectorDTOs);
        assertEquals(directorNamesToSave.size(), resultDirectorDTOs.size());
        List<String> resultDirectorNames = resultDirectorDTOs.stream().map(DirectorDTO::getName).collect(Collectors.toList());
        assertTrue(resultDirectorNames.containsAll(directorNamesToSave) && directorNamesToSave.containsAll(resultDirectorNames));

        User updatedUser = userRepository.findById(testUser.getUserId()).get();
        assertNotNull(updatedUser.getFavoriteDirectorsJson());
        try {
            List<DirectorDTO> parsedDTOs = objectMapper.readValue(updatedUser.getFavoriteDirectorsJson(), 
                objectMapper.getTypeFactory().constructCollectionType(List.class, DirectorDTO.class));
            List<String> parsedNames = parsedDTOs.stream().map(DirectorDTO::getName).collect(Collectors.toList());
            assertEquals(directorNamesToSave.size(), parsedNames.size());
            assertTrue(parsedNames.containsAll(directorNamesToSave) && directorNamesToSave.containsAll(parsedNames));
        } catch (JsonProcessingException e) {
            fail("Failed to parse favorite directors JSON: " + e.getMessage());
        }
    }

    @Test
    void saveFavoriteDirectors_WithInvalidToken_ThrowsException() {
        // Arrange
        List<String> directorNames = new ArrayList<>(List.of("D1", "D2"));
        List<DirectorDTO> directorDTOsToSave = directorNames.stream().map(name -> {
            DirectorDTO dto = new DirectorDTO();
            dto.setName(name);
            return dto;
        }).collect(Collectors.toList());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            userFavoritesService.saveFavoriteDirectors(
                    testUser.getUserId(), directorDTOsToSave, "wrongToken");
        });
    }

    @Test
    void getFavoriteDirectors_ReturnsCorrectDirectors() throws JsonProcessingException { 
        // Arrange via service - save some directors first
        List<String> directorNamesToSetup = new ArrayList<>(List.of("Setup Director X", "Setup Director Y"));
        List<DirectorDTO> directorDTOsToSetup = directorNamesToSetup.stream().map(name -> {
            DirectorDTO dto = new DirectorDTO();
            dto.setName(name);
            return dto;
        }).collect(Collectors.toList());
        userFavoritesService.saveFavoriteDirectors(testUser.getUserId(), directorDTOsToSetup, testUser.getToken());

        // Act
        List<DirectorDTO> resultDTOs = userFavoritesService.getFavoriteDirectors(testUser.getUserId());
        List<String> resultNames = resultDTOs.stream().map(DirectorDTO::getName).collect(Collectors.toList());

        // Assert
        assertEquals(directorNamesToSetup.size(), resultNames.size());
        assertTrue(resultNames.containsAll(directorNamesToSetup) && directorNamesToSetup.containsAll(resultNames));
    }
}