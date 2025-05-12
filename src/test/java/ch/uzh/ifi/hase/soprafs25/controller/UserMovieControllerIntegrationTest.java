package ch.uzh.ifi.hase.soprafs25.controller;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserMovieControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    private User testUser;
    private String testToken;
    private Movie testMovie;

    @BeforeEach
    void setup() {
        // clean up repositories
        userRepository.deleteAll();
        movieRepository.deleteAll();

        // create test user
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@mail.com");
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ONLINE);
        testToken = "testToken";
        testUser.setToken(testToken);
        testUser = userRepository.saveAndFlush(testUser);

        // create test movie
        testMovie = new Movie();
        testMovie.setMovieId(1L);
        testMovie.setTitle("Test Movie");
        testMovie.setYear(2025);
        testMovie.setGenres(new ArrayList<>());
        testMovie.setActors(new ArrayList<>());
        testMovie.setDirectors(new ArrayList<>());
        testMovie.setOriginallanguage("en");
        testMovie.setTrailerURL("http://example.com/trailer");
        testMovie.setPosterURL("http://example.com/poster");
        testMovie.setDescription("Test description");
        testMovie = movieRepository.saveAndFlush(testMovie);
    }
    // successful update of watchlist
    @Test
    void addToWatchlist_returnsUpdatedWatchlist() throws Exception {
        // send request 
        mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/watchlist/{movieId}", 
                testUser.getUserId(), testMovie.getMovieId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                // make sure that the testuser was added
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));

        // check movie was added to watchlist
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertEquals(1, updatedUser.getWatchlist().size());
    }
    
    // successfully remove movie from watchlist  
    @Test
    void removeFromWatchlist_returnsUpdatedWatchlist() throws Exception {
        // add movie to watchlist
        testUser.setWatchlist(new ArrayList<>(List.of(testMovie)));
        userRepository.saveAndFlush(testUser);

        // send request 
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/{userId}/watchlist/{movieId}", 
                testUser.getUserId(), testMovie.getMovieId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // nothing, because the only movie was deleted
                .andExpect(jsonPath("$", hasSize(0)));

        // verify movie was removed from watchlist
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertEquals(0, updatedUser.getWatchlist().size());
    }
    
    // successful retrieve of watchlist     
    @Test
    void getWatchlist_returnsWatchlist() throws Exception {
        // add movie to user's watchlist
        testUser.setWatchlist(new ArrayList<>(List.of(testMovie)));
        userRepository.saveAndFlush(testUser);
        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/users/{userId}/watchlist", testUser.getUserId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // make sure that test movie appears
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));
    }

    // succesfully retrieve watched movies
    @Test
    void getWatchedMovies_returnsWatchedMovies() throws Exception {
        // add movie to user'swatched movies
        testUser.setWatchedMovies(new ArrayList<>(List.of(testMovie)));
        userRepository.saveAndFlush(testUser);

        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/users/{userId}/watched", testUser.getUserId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // verify that test movie is returned 
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));
    }
    
    // successfully add a movie to watched movies
    @Test
    void addToWatchedMovies_returnsUpdatedWatchedMovies() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/users/{userId}/watched/{movieId}", 
                testUser.getUserId(), testMovie.getMovieId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                // test movie was added
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movieId", is(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));

        // verify movie was added to watched movies
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertEquals(1, updatedUser.getWatchedMovies().size());
    }
    // successfully remove movie from watchlist
    @Test
    void removeFromWatchedMovies_returnsUpdatedWatchedMovies() throws Exception {
        // add movie to watched movies
        testUser.setWatchedMovies(new ArrayList<>(List.of(testMovie)));
        userRepository.saveAndFlush(testUser);

        // send request
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/{userId}/watched/{movieId}", 
                testUser.getUserId(), testMovie.getMovieId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // test movie was removed => nothing
                .andExpect(jsonPath("$", hasSize(0)));

        // check movie was removed from watched movies
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertEquals(0, updatedUser.getWatchedMovies().size());
    }
}