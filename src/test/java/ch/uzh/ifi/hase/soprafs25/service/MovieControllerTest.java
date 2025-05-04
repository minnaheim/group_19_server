package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.controller.MovieController;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.exceptions.SearchValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;
import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MovieControllerTest
 * This is a WebMvcTest which allows to test the MovieController i.e. GET/POST/PUT request
 * without actually sending them over the network.
 * This tests if the MovieController works.
 */
@WebMvcTest(MovieController.class)
public class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;

    @MockBean
    private TMDbService tmdbService;

    private List<Movie> testMovies;

    @BeforeEach
    public void setup() {
        // Create test movies
        testMovies = new ArrayList<>();

        Movie movie1 = new Movie();
        movie1.setMovieId(1);
        movie1.setTitle("Inception");
        movie1.setYear(2010);
        movie1.setGenres(Arrays.asList("Action", "Science Fiction", "Adventure"));
        movie1.setDirectors(Collections.singletonList("Christopher Nolan"));
        movie1.setActors(Arrays.asList("Leonardo DiCaprio", "Joseph Gordon-Levitt"));
        movie1.setDescription("A thief who steals corporate secrets through the use of dream-sharing technology.");
        movie1.setPosterURL("image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg");
        movie1.setTrailerURL("www.youtube.com/watch?v=9gk7adHYeDv");
        movie1.setOriginallanguage("English");

        Movie movie2 = new Movie();
        movie2.setMovieId(2);
        movie2.setTitle("The Dark Knight");
        movie2.setYear(2008);
        movie2.setGenres(Arrays.asList("Action", "Crime", "Drama"));
        movie2.setDirectors(Collections.singletonList("Christopher Nolan"));
        movie2.setActors(Arrays.asList("Christian Bale", "Heath Ledger"));
        movie2.setDescription("When the menace known as the Joker wreaks havoc and chaos on the people of Gotham.");
        movie2.setPosterURL("image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg");

        testMovies.add(movie1);
        testMovies.add(movie2);
    }

    /**
     * Test getting movies with valid query parameters
     * This test verifies that the controller correctly calls the service and returns movies
     */
    @Test // Test for GET /movies endpoint with valid parameters
    public void testGetMovies_ValidParameters() throws Exception {
        // Mock service response
        when(movieService.getMovies(any(Movie.class))).thenReturn(testMovies);

        // Perform GET request with parameters
        mockMvc.perform(get("/movies")
                        .param("title", "Inception")
                        .param("genres", "Action", "Science Fiction")
                        .param("year", "2010")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Inception")))
                .andExpect(jsonPath("$[0].year", is(2010)))
                .andExpect(jsonPath("$[0].genres", hasSize(3)))
                .andExpect(jsonPath("$[0].genres", containsInAnyOrder("Action", "Science Fiction", "Adventure")));
    }

    /**
     * Test getting movies with invalid query parameters
     * This test verifies that the controller correctly handles validation errors
     */
    @Test // Test for GET /movies endpoint with invalid parameters
    public void testGetMovies_InvalidParameters() throws Exception {
        // Perform the request with invalid year parameter
        MockHttpServletResponse response = mockMvc.perform(get("/movies")
                        .param("year", "3000")  // Invalid year
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse();

        // Update the assertion to match the actual response format
        mockMvc.perform(get("/movies")
                        .param("year", "3000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Year must be between 1888 and 2035"))
                .andExpect(jsonPath("$.status").value(400));
    }


    /**
     * Test getting a movie by valid ID
     * This test verifies that the controller correctly retrieves a movie by its ID
     */
    @Test // Test for GET /movies/{movieId} endpoint with valid movie ID
    public void testGetMovieById_ValidId() throws Exception {
        // Mock service response
        when(movieService.getMovieById(1L)).thenReturn(testMovies.get(0));

        // Perform GET request
        mockMvc.perform(get("/movies/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId", is(1)))
                .andExpect(jsonPath("$.title", is("Inception")))
                .andExpect(jsonPath("$.genres", hasSize(3)))
                .andExpect(jsonPath("$.genres", containsInAnyOrder("Action", "Science Fiction", "Adventure")))
                .andExpect(jsonPath("$.year", is(2010)))
                .andExpect(jsonPath("$.directors", hasSize(1)))
                .andExpect(jsonPath("$.directors[0]", is("Christopher Nolan")))
                .andExpect(jsonPath("$.actors", hasSize(2)))
                .andExpect(jsonPath("$.actors[0]", is("Leonardo DiCaprio")))
                .andExpect(jsonPath("$.actors[1]", is("Joseph Gordon-Levitt")))
                .andExpect(jsonPath("$.description", is("A thief who steals corporate secrets through the use of dream-sharing technology.")))
                .andExpect(jsonPath("$.posterURL", is("image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg")))
                .andExpect(jsonPath("$.trailerURL", is("www.youtube.com/watch?v=9gk7adHYeDv")))
                .andExpect(jsonPath("$.originallanguage", is("English")));
    }

    /**
     * Test getting a movie by invalid ID
     * This test verifies that the controller correctly handles errors when an invalid movie ID is provided
     */
    @Test // Test for GET /movies/{movieId} endpoint with invalid movie ID
    public void testGetMovieById_InvalidId() throws Exception {
        // Mock service to throw exception for non-existent movie
        when(movieService.getMovieById(999L)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        // Perform GET request with invalid ID
        mockMvc.perform(get("/movies/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


    /**
     * Test 3.1 - Mock the MovieService to return a predefined list of movies
     * This test verifies that the controller correctly calls the service with the right parameters
     */
    @Test
    public void testGetMovieSuggestions_Success() throws Exception {
        // Mock service to return predefined list of movies
        when(movieService.getMovieSuggestions(eq(1L), eq(100))).thenReturn(testMovies);

        // When/Then -> Perform the request and validate the response
        MockHttpServletRequestBuilder getRequest = get("/movies/suggestions/1")
                .contentType(MediaType.APPLICATION_JSON);

        // Verify controller returns correct status and expected data
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Inception")))
                .andExpect(jsonPath("$[0].posterURL", is("image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg")))
                .andExpect(jsonPath("$[1].movieId", is(2)))
                .andExpect(jsonPath("$[1].title", is("The Dark Knight")))
                .andExpect(jsonPath("$[1].posterURL", is("image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg")));
    }

    /**
     * Test 3.2 - Verify the controller correctly converts the service response to DTOs
     * This test ensures that the movie entities are correctly converted to DTOs
     */
    @Test
    public void testGetMovieSuggestions_CorrectDTOConversion() throws Exception {
        // Add more fields to test movies to verify complete DTO conversion
        Movie detailedMovie = new Movie();
        detailedMovie.setMovieId(3L);
        detailedMovie.setTitle("Detailed Movie");
        detailedMovie.setPosterURL("example.com/poster3.jpg");
        detailedMovie.setTrailerURL("example.com/trailer3.mp4");
        detailedMovie.setDescription("This is a detailed description");
        detailedMovie.setYear(2023);
        detailedMovie.setGenres(Arrays.asList("Action", "Adventure"));
        detailedMovie.setActors(Arrays.asList("Actor 1", "Actor 2"));
        detailedMovie.setDirectors(Arrays.asList("Director 1"));
        detailedMovie.setSpokenlanguages(Arrays.asList("English", "Spanish"));
        detailedMovie.setOriginallanguage("English");

        List<Movie> detailedMovies = new ArrayList<>();
        detailedMovies.add(detailedMovie);

        // Mock service to return the detailed movie
        when(movieService.getMovieSuggestions(eq(1L), eq(100))).thenReturn(detailedMovies);

        // When/Then -> Perform the request and validate the response
        MockHttpServletRequestBuilder getRequest = get("/movies/suggestions/1")
                .contentType(MediaType.APPLICATION_JSON);

        // Verify all movie fields are correctly converted to DTO fields
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(3)))
                .andExpect(jsonPath("$[0].title", is("Detailed Movie")))
                .andExpect(jsonPath("$[0].posterURL", is("example.com/poster3.jpg")));
    }

    /**
     * Test 3.3 - Test error handling for invalid user IDs
     * This test verifies that the controller properly handles errors when an invalid user ID is provided
     */
    @Test
    public void testGetMovieSuggestions_InvalidUserId() throws Exception {
        // Mock service to throw exception for invalid user ID
        when(movieService.getMovieSuggestions(eq(99L), any(Integer.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID 99 not found"));

        // When/Then -> Perform the request and validate the error response
        MockHttpServletRequestBuilder getRequest = get("/movies/suggestions/99")
                .contentType(MediaType.APPLICATION_JSON);

        // Verify controller returns NOT_FOUND status for invalid user ID
        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    /**
     * Test getting all genres
     * This test verifies that the controller correctly retrieves all genres
     */
    @Test // Test for GET /movies/genres endpoint
    public void testGetGenres_Success() throws Exception {
        // Create mock JsonNode for genres response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode genresNode = mapper.readTree(
                "{\"genres\":[" +
                        "{\"id\":28,\"name\":\"Action\"}," +
                        "{\"id\":12,\"name\":\"Adventure\"}," +
                        "{\"id\":16,\"name\":\"Animation\"}" +
                        "]}");

        // Mock service response
        when(tmdbService.getGenres()).thenReturn(genresNode);

        // Perform GET request
        mockMvc.perform(get("/movies/genres")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres", hasSize(3)))
                .andExpect(jsonPath("$.genres[0].id", is(28)))
                .andExpect(jsonPath("$.genres[0].name", is("Action")))
                .andExpect(jsonPath("$.genres[1].id", is(12)))
                .andExpect(jsonPath("$.genres[1].name", is("Adventure")))
                .andExpect(jsonPath("$.genres[2].id", is(16)))
                .andExpect(jsonPath("$.genres[2].name", is("Animation")));
    }

    /**
     * Test searching for actors with a valid name
     * This test verifies that the controller correctly retrieves actors by name
     */
    @Test
    public void testSearchActors_ValidName() throws Exception {
        // Prepare test data
        List<ActorDTO> actors = new ArrayList<>();
        ActorDTO actor = new ActorDTO();
        actor.setActorId(1L);
        actor.setActorName("Leonardo DiCaprio");
        actors.add(actor);

        // Mock the service
        when(tmdbService.searchActors(eq("LeonardoDiCaprio"))).thenReturn(actors);

        // Perform the GET request and validate the response
        mockMvc.perform(get("/movies/actors")
                        .param("actorname", "LeonardoDiCaprio")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].actorId", is(1)))
                .andExpect(jsonPath("$[0].actorName", is("Leonardo DiCaprio")));

        // Verify the service was called with the correct parameter
        verify(tmdbService).searchActors("LeonardoDiCaprio");
    }

    /**
     * Test searching for actors with an invalid name (empty)
     * This test verifies that the controller correctly handles validation errors
     */
    @Test
    public void testSearchActors_InvalidName_Empty() throws Exception {
        // Mock the service to throw exception for empty name
        when(tmdbService.searchActors(eq("")))
                .thenThrow(new IllegalArgumentException("Search term must be at least 1 character long"));

        // Perform the GET request and validate the error response
        mockMvc.perform(get("/movies/actors")
                        .param("actorname", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Search term must be at least 1 character long")));
    }

    /**
     * Test searching for actors with a null name (missing parameter)
     * This test verifies that the controller correctly handles missing parameters
     */
    @Test
    public void testSearchActors_InvalidName_Null() throws Exception {
        // Perform the GET request without the name parameter and validate the error response
        mockMvc.perform(get("/movies/actors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Search term must be at least 1 character long")));
    }

    /**
     * Test searching for actors when TMDb service returns an error
     * This test verifies that the controller correctly handles service errors
     */
    @Test
    public void testSearchActors_ServiceError() throws Exception {
        // Mock the service to throw exception for service error
        when(tmdbService.searchActors(eq("LeonardoDiCaprio")))
                .thenThrow(new RuntimeException("Error searching for actors: 401 Unauthorized"));

        // Perform the GET request and validate the error response
        mockMvc.perform(get("/movies/actors")
                        .param("actorname", "LeonardoDiCaprio")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Error searching for actors: 401 Unauthorized")));
    }

    /**
     * Test searching for directors with a valid name
     * This test verifies that the controller correctly retrieves directors by name
     */
    @Test
    public void testSearchDirectors_ValidName() throws Exception {
        // Prepare test data
        List<DirectorDTO> directors = new ArrayList<>();
        DirectorDTO director = new DirectorDTO();
        director.setDirectorId(1L);
        director.setDirectorName("Christopher Nolan");
        directors.add(director);

        // Mock the service
        when(tmdbService.searchDirectors(eq("ChristopherNolan"))).thenReturn(directors);

        // Perform the GET request and validate the response
        mockMvc.perform(get("/movies/directors")
                        .param("directorname", "ChristopherNolan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].directorId", is(1)))
                .andExpect(jsonPath("$[0].directorName", is("Christopher Nolan")));

        // Verify the service was called with the correct parameter
        verify(tmdbService).searchDirectors("ChristopherNolan");
    }

    /**
     * Test searching for directors with an invalid name (empty)
     * This test verifies that the controller correctly handles validation errors
     */
    @Test
    public void testSearchDirectors_InvalidName_Empty() throws Exception {
        // Mock the service to throw exception for empty name
        when(tmdbService.searchDirectors(eq("")))
                .thenThrow(new IllegalArgumentException("Director search query cannot be empty"));

        // Perform the GET request and validate the error response
        mockMvc.perform(get("/movies/directors")
                        .param("directorname", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Search term must be at least 1 character long")));
    }

    /**
     * Test searching for directors with a null name (missing parameter)
     * This test verifies that the controller correctly handles missing parameters
     */
    @Test
    public void testSearchDirectors_InvalidName_Null() throws Exception {
        // Perform the GET request without the name parameter and validate the error response
        mockMvc.perform(get("/movies/directors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Search term must be at least 1 character long")));
    }

    /**
     * Test searching for directors when TMDb service returns an error
     * This test verifies that the controller correctly handles service errors
     */
    @Test
    public void testSearchDirectors_ServiceError() throws Exception {
        // Mock the service to throw exception for service error
        when(tmdbService.searchDirectors(eq("ChristopherNolan")))
                .thenThrow(new RuntimeException("Error searching for directors: 401 Unauthorized"));

        // Perform the GET request and validate the error response
        mockMvc.perform(get("/movies/directors")
                        .param("directorname", "ChristopherNolan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Error searching for directors: 401 Unauthorized")));
    }
}
