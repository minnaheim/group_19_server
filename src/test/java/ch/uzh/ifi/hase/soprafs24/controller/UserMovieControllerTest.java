package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Movie;
import ch.uzh.ifi.hase.soprafs24.service.UserMovieService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserMovieController.class)
public class UserMovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMovieService userMovieService;

    @Test
    public void getWatchlist_returnsWatchlist() throws Exception {
        // Given
        Long userId = 1L;
        List<Movie> watchlist = new ArrayList<>();
        
        Movie movie = new Movie();
        movie.setMovieId(1L);
        movie.setTitle("Test Movie");
        watchlist.add(movie);
        
        given(userMovieService.getWatchlist(userId)).willReturn(watchlist);

        // When/Then
        MockHttpServletRequestBuilder getRequest = get("/users/{userId}/watchlist", userId)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));
    }

    @Test
    public void addToWatchlist_success_returnsUpdatedWatchlist() throws Exception {
        // Given
        Long userId = 1L;
        Long movieId = 1L;
        String token = "valid-token";
        List<Movie> updatedWatchlist = new ArrayList<>();
        
        Movie movie = new Movie();
        movie.setMovieId(movieId);
        movie.setTitle("Test Movie");
        updatedWatchlist.add(movie);
        
        given(userMovieService.addToWatchlist(userId, movieId, token)).willReturn(updatedWatchlist);

        // When/Then
        MockHttpServletRequestBuilder postRequest = post("/users/{userId}/watchlist/{movieId}", userId, movieId)
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));
                
        verify(userMovieService).addToWatchlist(userId, movieId, token);
    }

    @Test
    public void removeFromWatchlist_success_returnsUpdatedWatchlist() throws Exception {
        // Given
        Long userId = 1L;
        Long movieId = 1L;
        String token = "valid-token";
        List<Movie> emptyWatchlist = new ArrayList<>();
        
        given(userMovieService.removeFromWatchlist(userId, movieId, token)).willReturn(emptyWatchlist);

        // When/Then
        MockHttpServletRequestBuilder deleteRequest = delete("/users/{userId}/watchlist/{movieId}", userId, movieId)
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(deleteRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
                
        verify(userMovieService).removeFromWatchlist(userId, movieId, token);
    }

    @Test
    public void getWatchedMovies_returnsWatchedMovies() throws Exception {
        // Given
        Long userId = 1L;
        List<Movie> watchedMovies = new ArrayList<>();
        
        Movie movie = new Movie();
        movie.setMovieId(1L);
        movie.setTitle("Test Movie");
        watchedMovies.add(movie);
        
        given(userMovieService.getWatchedMovies(userId)).willReturn(watchedMovies);

        // When/Then
        MockHttpServletRequestBuilder getRequest = get("/users/{userId}/watched", userId)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));
    }

    @Test
    public void addToWatchedMovies_success_returnsUpdatedWatchedMovies() throws Exception {
        // Given
        Long userId = 1L;
        Long movieId = 1L;
        String token = "valid-token";
        List<Movie> updatedWatchedMovies = new ArrayList<>();
        
        Movie movie = new Movie();
        movie.setMovieId(movieId);
        movie.setTitle("Test Movie");
        updatedWatchedMovies.add(movie);
        
        given(userMovieService.addToWatchedMovies(userId, movieId, token)).willReturn(updatedWatchedMovies);

        // When/Then
        MockHttpServletRequestBuilder postRequest = post("/users/{userId}/watched/{movieId}", userId, movieId)
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));
                
        verify(userMovieService).addToWatchedMovies(userId, movieId, token);
    }

    @Test
    public void removeFromWatchedMovies_success_returnsUpdatedWatchedMovies() throws Exception {
        // Given
        Long userId = 1L;
        Long movieId = 1L;
        String token = "valid-token";
        List<Movie> emptyWatchedMovies = new ArrayList<>();
        
        given(userMovieService.removeFromWatchedMovies(userId, movieId, token)).willReturn(emptyWatchedMovies);

        // When/Then
        MockHttpServletRequestBuilder deleteRequest = delete("/users/{userId}/watched/{movieId}", userId, movieId)
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(deleteRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
                
        verify(userMovieService).removeFromWatchedMovies(userId, movieId, token);
    }
}