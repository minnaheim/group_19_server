package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.service.UserPreferencesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserPreferencesController.class)
class UserPreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserPreferencesService userPreferencesService;

    @Test
    void getAllGenres_ReturnsGenres() throws Exception {
        // Arrange
        List<Map<String, Object>> genres = new ArrayList<>();
        Map<String, Object> genre1 = new HashMap<>();
        genre1.put("id", 28);
        genre1.put("name", "Action");
        genres.add(genre1);
        
        Map<String, Object> genre2 = new HashMap<>();
        genre2.put("id", 12);
        genre2.put("name", "Adventure");
        genres.add(genre2);
        
        given(userPreferencesService.getAllGenres()).willReturn(genres);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/api/genres")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(28)))
                .andExpect(jsonPath("$[0].name", is("Action")))
                .andExpect(jsonPath("$[1].id", is(12)))
                .andExpect(jsonPath("$[1].name", is("Adventure")));
    }

    @Test
    void saveGenrePreferences_WithValidData_ReturnsSuccess() throws Exception {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        
        given(userPreferencesService.saveGenrePreferences(anyLong(), anyList(), anyString()))
                .willReturn(genreNames);

        Map<String, List<String>> requestBody = new HashMap<>();
        requestBody.put("genreIds", genreNames);

        // Act & Assert
        MockHttpServletRequestBuilder postRequest = post("/api/users/1/preferences/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer validToken")
                .content(asJsonString(requestBody));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.genres[0]", is("Action")))
                .andExpect(jsonPath("$.genres[1]", is("Adventure")));
    }

    @Test
    void getGenrePreferences_ReturnsGenres() throws Exception {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        
        given(userPreferencesService.getGenrePreferences(anyLong())).willReturn(genreNames);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/api/users/1/preferences/genres")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres[0]", is("Action")))
                .andExpect(jsonPath("$.genres[1]", is("Adventure")));
    }

    @Test
    void saveFavoriteMovie_WithValidData_ReturnsSuccess() throws Exception {
        // Arrange
        Movie movie = new Movie();
        movie.setMovieId(123L);
        movie.setTitle("Test Movie");
        
        given(userPreferencesService.saveFavoriteMovie(anyLong(), anyLong(), anyString()))
                .willReturn(movie);

        Map<String, Long> requestBody = new HashMap<>();
        requestBody.put("movieId", 123L);

        // Act & Assert
        MockHttpServletRequestBuilder postRequest = post("/api/users/1/preferences/favorite-movie")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer validToken")
                .content(asJsonString(requestBody));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.movie.movieId", is(123)));
    }

    @Test
    void getFavoriteMovie_ReturnsMovie() throws Exception {
        // Arrange
        Movie movie = new Movie();
        movie.setMovieId(123L);
        movie.setTitle("Test Movie");
        
        given(userPreferencesService.getFavoriteMovie(anyLong())).willReturn(movie);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/api/users/1/preferences/favorite-movie")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie.movieId", is(123)));
    }

    @Test
    void getFavoriteMovie_WithNoMovie_ReturnsNull() throws Exception {
        // Arrange
        given(userPreferencesService.getFavoriteMovie(anyLong())).willReturn(null);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/api/users/1/preferences/favorite-movie")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie").isEmpty());
    }

    /**
     * Helper Method to convert object to JSON string
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}