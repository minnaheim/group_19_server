package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPreferencesGenresDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPreferencesFavoriteMovieDTO;

import ch.uzh.ifi.hase.soprafs25.service.UserPreferencesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;



import java.util.List;


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
    void saveGenrePreferences_WithValidData_ReturnsSuccess() throws Exception {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        given(userPreferencesService.saveGenrePreferences(anyLong(), anyList(), anyString()))
                .willReturn(genreNames);

        UserPreferencesGenresDTO requestBody = new UserPreferencesGenresDTO();
        requestBody.setGenreIds(genreNames);

        // Act & Assert
        MockHttpServletRequestBuilder postRequest = post("/users/1/preferences/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer validToken")
                .content(asJsonString(requestBody));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genreIds[0]", is("Action")))
                .andExpect(jsonPath("$.genreIds[1]", is("Adventure")));
    }

    @Test
    void getGenrePreferences_ReturnsGenres() throws Exception {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        given(userPreferencesService.getGenrePreferences(anyLong())).willReturn(genreNames);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/users/1/preferences/genres")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genreIds[0]", is("Action")))
                .andExpect(jsonPath("$.genreIds[1]", is("Adventure")));
    }

    @Test
    void saveFavoriteMovie_WithValidData_ReturnsSuccess() throws Exception {
        // Arrange
        Movie movie = new Movie();
        movie.setMovieId(123L);
        movie.setTitle("Test Movie");
        given(userPreferencesService.saveFavoriteMovie(anyLong(), anyLong(), anyString()))
                .willReturn(movie);

        UserPreferencesFavoriteMovieDTO requestBody = new UserPreferencesFavoriteMovieDTO();
        requestBody.setMovieId(123L);

        // Act & Assert
        MockHttpServletRequestBuilder postRequest = post("/users/1/preferences/favorite-movie")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer validToken")
                .content(asJsonString(requestBody));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId", is(123)));
    }

    @Test
    void getFavoriteMovie_ReturnsMovie() throws Exception {
        // Arrange
        Movie movie = new Movie();
        movie.setMovieId(123L);
        movie.setTitle("Test Movie");
        given(userPreferencesService.getFavoriteMovie(anyLong())).willReturn(movie);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/users/1/preferences/favorite-movie")
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
        MockHttpServletRequestBuilder getRequest = get("/users/1/preferences/favorite-movie")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie").doesNotExist());
    }

    @Test
    void getAllPreferences_ReturnsGenresAndMovie() throws Exception {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        Movie movie = new Movie();
        movie.setMovieId(123L);
        movie.setTitle("Test Movie");
        given(userPreferencesService.getGenrePreferences(anyLong())).willReturn(genreNames);
        given(userPreferencesService.getFavoriteMovie(anyLong())).willReturn(movie);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/users/1/preferences")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteGenres[0]", is("Action")))
                .andExpect(jsonPath("$.favoriteGenres[1]", is("Adventure")))
                .andExpect(jsonPath("$.favoriteMovie.movieId", is(123)))
                .andExpect(jsonPath("$.favoriteMovie.title", is("Test Movie")));
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