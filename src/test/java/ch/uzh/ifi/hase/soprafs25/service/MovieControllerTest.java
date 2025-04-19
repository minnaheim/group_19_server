package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.MovieService;
import ch.uzh.ifi.hase.soprafs25.service.TMDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        movie1.setMovieId(1L);
        movie1.setTitle("Test Movie 1");
        movie1.setPosterURL("http://example.com/poster1.jpg");

        Movie movie2 = new Movie();
        movie2.setMovieId(2L);
        movie2.setTitle("Test Movie 2");
        movie2.setPosterURL("http://example.com/poster2.jpg");

        testMovies.add(movie1);
        testMovies.add(movie2);
    }

    /**
     * Test 3.1 - Mock the MovieService to return a predefined list of movies
     * This test verifies that the controller correctly calls the service with the right parameters
     */
    @Test
    public void testGetMovieSuggestions_Success() throws Exception {
        // CHANGE: Mock service to return predefined list of movies
        when(movieService.getMovieSuggestions(eq(1L), eq(100))).thenReturn(testMovies);

        // When/Then -> Perform the request and validate the response
        MockHttpServletRequestBuilder getRequest = get("/movies/suggestions/1")
                .contentType(MediaType.APPLICATION_JSON);

        // CHANGE: Verify controller returns correct status and expected data
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie 1")))
                .andExpect(jsonPath("$[0].posterURL", is("http://example.com/poster1.jpg")))
                .andExpect(jsonPath("$[1].movieId", is(2)))
                .andExpect(jsonPath("$[1].title", is("Test Movie 2")))
                .andExpect(jsonPath("$[1].posterURL", is("http://example.com/poster2.jpg")));
    }

    /**
     * Test 3.2 - Verify the controller correctly converts the service response to DTOs
     * This test ensures that the movie entities are correctly converted to DTOs
     */
    @Test
    public void testGetMovieSuggestions_CorrectDTOConversion() throws Exception {
        // CHANGE: Add more fields to test movies to verify complete DTO conversion
        Movie detailedMovie = new Movie();
        detailedMovie.setMovieId(3L);
        detailedMovie.setTitle("Detailed Movie");
        detailedMovie.setPosterURL("http://example.com/poster3.jpg");
        detailedMovie.setTrailerURL("http://example.com/trailer3.mp4");
        detailedMovie.setDescription("This is a detailed description");
        detailedMovie.setYear(2023);
        detailedMovie.setGenres(Arrays.asList("Action", "Adventure"));
        detailedMovie.setActors(Arrays.asList("Actor 1", "Actor 2"));
        detailedMovie.setDirectors(Arrays.asList("Director 1"));
        detailedMovie.setSpokenlanguages(Arrays.asList("English", "Spanish"));
        detailedMovie.setOriginallanguage("English");

        List<Movie> detailedMovies = new ArrayList<>();
        detailedMovies.add(detailedMovie);

        // CHANGE: Mock service to return the detailed movie
        when(movieService.getMovieSuggestions(eq(1L), eq(100))).thenReturn(detailedMovies);

        // When/Then -> Perform the request and validate the response
        MockHttpServletRequestBuilder getRequest = get("/movies/suggestions/1")
                .contentType(MediaType.APPLICATION_JSON);

        // CHANGE: Verify all movie fields are correctly converted to DTO fields
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(3)))
                .andExpect(jsonPath("$[0].title", is("Detailed Movie")))
                .andExpect(jsonPath("$[0].posterURL", is("http://example.com/poster3.jpg")));
    }

    /**
     * Test 3.3 - Test error handling for invalid user IDs
     * This test verifies that the controller properly handles errors when an invalid user ID is provided
     */
    @Test
    public void testGetMovieSuggestions_InvalidUserId() throws Exception {
        // CHANGE: Mock service to throw exception for invalid user ID
        when(movieService.getMovieSuggestions(eq(99L), any(Integer.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID 99 not found"));

        // When/Then -> Perform the request and validate the error response
        MockHttpServletRequestBuilder getRequest = get("/movies/suggestions/99")
                .contentType(MediaType.APPLICATION_JSON);

        // CHANGE: Verify controller returns NOT_FOUND status for invalid user ID
        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }
}