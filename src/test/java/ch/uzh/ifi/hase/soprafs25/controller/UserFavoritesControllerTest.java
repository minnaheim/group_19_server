package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesActorsDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesDirectorsDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesGenresDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesMovieDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;

import ch.uzh.ifi.hase.soprafs25.service.UserFavoritesService;
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

@WebMvcTest(UserFavoritesController.class)
class UserFavoritesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserFavoritesService UserFavoritesService;

    @Test
    void saveGenreFavorites_WithValidData_ReturnsSuccess() throws Exception {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        given(UserFavoritesService.saveGenreFavorites(anyLong(), anyList(), anyString()))
                .willReturn(genreNames);

        UserFavoritesGenresDTO requestBody = new UserFavoritesGenresDTO();
        requestBody.setGenreIds(genreNames);

        // Act & Assert
        MockHttpServletRequestBuilder postRequest = post("/users/1/favorites/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer validToken")
                .content(asJsonString(requestBody));

        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genreIds[0]", is("Action")))
                .andExpect(jsonPath("$.genreIds[1]", is("Adventure")));
    }

    @Test
    void getGenreFavorites_ReturnsGenres() throws Exception {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        given(UserFavoritesService.getGenreFavorites(anyLong())).willReturn(genreNames);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/users/1/favorites/genres")
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
        given(UserFavoritesService.saveFavoriteMovie(anyLong(), anyLong(), anyString()))
                .willReturn(movie);

        UserFavoritesMovieDTO requestBody = new UserFavoritesMovieDTO();
        requestBody.setMovieId(123L);

        // Act & Assert
        MockHttpServletRequestBuilder postRequest = post("/users/1/favorites/movie")
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
        given(UserFavoritesService.getFavoriteMovie(anyLong())).willReturn(movie);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/users/1/favorites/movie")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie.movieId", is(123)));
    }

    @Test
    void getFavoriteMovie_WithNoMovie_ReturnsNull() throws Exception {
        // Arrange
        given(UserFavoritesService.getFavoriteMovie(anyLong())).willReturn(null);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/users/1/favorites/movie")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movie").doesNotExist());
    }

    @Test
    void getAllFavorites_ReturnsGenresAndMovie() throws Exception {
        // Arrange
        List<String> genreNames = List.of("Action", "Adventure");
        Movie movie = new Movie();
        movie.setMovieId(123L);
        movie.setTitle("Test Movie");

        ActorDTO actor1 = new ActorDTO();
        actor1.setId(1);
        actor1.setName("Favorite Actor 1");
        List<ActorDTO> favoriteActors = List.of(actor1);

        DirectorDTO director1 = new DirectorDTO();
        director1.setId(10);
        director1.setName("Favorite Director 1");
        List<DirectorDTO> favoriteDirectors = List.of(director1);

        given(UserFavoritesService.getGenreFavorites(anyLong())).willReturn(genreNames);
        given(UserFavoritesService.getFavoriteMovie(anyLong())).willReturn(movie);
        given(UserFavoritesService.getFavoriteActors(anyLong())).willReturn(favoriteActors);
        given(UserFavoritesService.getFavoriteDirectors(anyLong())).willReturn(favoriteDirectors);

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/users/1/favorites")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteGenres[0]", is("Action")))
                .andExpect(jsonPath("$.favoriteGenres[1]", is("Adventure")))
                .andExpect(jsonPath("$.favoriteMovie.movieId", is(123)))
                .andExpect(jsonPath("$.favoriteMovie.title", is("Test Movie")))
                .andExpect(jsonPath("$.favoriteActors[0]", is("Favorite Actor 1")))
                .andExpect(jsonPath("$.favoriteDirectors[0]", is("Favorite Director 1")));
    }

    @Test
    void saveFavoriteActors_WithValidData_ReturnsActors() throws Exception {
        ActorDTO actor1 = new ActorDTO();
        actor1.setId(1);
        actor1.setName("Actor One");
        ActorDTO actor2 = new ActorDTO();
        actor2.setId(2);
        actor2.setName("Actor Two");
        List<ActorDTO> actors = List.of(actor1, actor2);

        given(UserFavoritesService.saveFavoriteActors(anyLong(), anyList(), anyString()))
                .willReturn(actors);
        UserFavoritesActorsDTO request = new UserFavoritesActorsDTO();
        request.setFavoriteActors(actors);
        mockMvc.perform(post("/users/1/favorites/actors")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer token")
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteActors[0].id", is(1)))
                .andExpect(jsonPath("$.favoriteActors[0].name", is("Actor One")))
                .andExpect(jsonPath("$.favoriteActors[1].id", is(2)))
                .andExpect(jsonPath("$.favoriteActors[1].name", is("Actor Two")));
    }

    @Test
    void getFavoriteActors_ReturnsActors() throws Exception {
        ActorDTO actor1 = new ActorDTO();
        actor1.setId(1);
        actor1.setName("Actor One");
        ActorDTO actor2 = new ActorDTO();
        actor2.setId(2);
        actor2.setName("Actor Two");
        List<ActorDTO> actors = List.of(actor1, actor2);

        given(UserFavoritesService.getFavoriteActors(anyLong())).willReturn(actors);
        mockMvc.perform(get("/users/1/favorites/actors").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteActors[0].id", is(1)))
                .andExpect(jsonPath("$.favoriteActors[0].name", is("Actor One")))
                .andExpect(jsonPath("$.favoriteActors[1].id", is(2)))
                .andExpect(jsonPath("$.favoriteActors[1].name", is("Actor Two")));
    }

    @Test
    void saveFavoriteDirectors_WithValidData_ReturnsDirectors() throws Exception {
        DirectorDTO director1 = new DirectorDTO();
        director1.setId(1);
        director1.setName("Director One");
        DirectorDTO director2 = new DirectorDTO();
        director2.setId(2);
        director2.setName("Director Two");
        List<DirectorDTO> directors = List.of(director1, director2);

        given(UserFavoritesService.saveFavoriteDirectors(anyLong(), anyList(), anyString()))
                .willReturn(directors);
        UserFavoritesDirectorsDTO request = new UserFavoritesDirectorsDTO();
        request.setFavoriteDirectors(directors);
        mockMvc.perform(post("/users/1/favorites/directors")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer token")
                .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteDirectors[0].id", is(1)))
                .andExpect(jsonPath("$.favoriteDirectors[0].name", is("Director One")))
                .andExpect(jsonPath("$.favoriteDirectors[1].id", is(2)))
                .andExpect(jsonPath("$.favoriteDirectors[1].name", is("Director Two")));
    }

    @Test
    void getFavoriteDirectors_ReturnsDirectors() throws Exception {
        DirectorDTO director1 = new DirectorDTO();
        director1.setId(1);
        director1.setName("Director One");
        DirectorDTO director2 = new DirectorDTO();
        director2.setId(2);
        director2.setName("Director Two");
        List<DirectorDTO> directors = List.of(director1, director2);

        given(UserFavoritesService.getFavoriteDirectors(anyLong())).willReturn(directors);
        mockMvc.perform(get("/users/1/favorites/directors").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteDirectors[0].id", is(1)))
                .andExpect(jsonPath("$.favoriteDirectors[0].name", is("Director One")))
                .andExpect(jsonPath("$.favoriteDirectors[1].id", is(2)))
                .andExpect(jsonPath("$.favoriteDirectors[1].name", is("Director Two")));
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