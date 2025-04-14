package ch.uzh.ifi.hase.soprafs25.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.RankingResultRepository;
import ch.uzh.ifi.hase.soprafs25.repository.RankingSubmissionLogRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserMovieRankingRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // For JSON conversion

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserMovieRankingRepository userMovieRankingRepository;

    @Autowired
    private RankingResultRepository rankingResultRepository;

    @Autowired
    private RankingSubmissionLogRepository rankingSubmissionLogRepository; // For verification

    // We can autowire the real service or mock it. For integration, let's use the real one.
    // @Autowired
    // private RankingService rankingService;

    private User testUser;
    private Movie movie1, movie2, movie3, movie4, movie5;
    private List<Movie> availableMovies;

    @BeforeEach
    void setupDatabase() {
        // Clean up before test - @Transactional handles rollback, but explicit delete can be safer
        userMovieRankingRepository.deleteAll();
        rankingResultRepository.deleteAll();
        rankingSubmissionLogRepository.deleteAll();
        userRepository.deleteAll(); // Clear users if needed
        movieRepository.deleteAll(); // Clear movies if needed

        // Create test user
        testUser = new User();
        testUser.setUsername("testUserRankController");
        testUser.setPassword("password"); // Assuming password is required
        testUser.setEmail("rankcontroller@example.com");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("rankToken");   // Assuming token is required
        testUser = userRepository.saveAndFlush(testUser);

        // Create test movies (assuming Movie entity has necessary fields)
        movie1 = createAndSaveMovie(101L, "Movie A");
        movie2 = createAndSaveMovie(102L, "Movie B");
        movie3 = createAndSaveMovie(103L, "Movie C");
        movie4 = createAndSaveMovie(104L, "Movie D");
        movie5 = createAndSaveMovie(105L, "Movie E");
        availableMovies = List.of(movie1, movie2, movie3, movie4, movie5);
    }

    // --- Tests for POST /api/users/{userId}/rankings ---

    @Test
    void submitRankings_validInput_returnsNoContentAndSavesData() throws Exception {
        List<RankingSubmitDTO> rankings = List.of(
                createSubmitDTO(movie1.getMovieId(), 1),
                createSubmitDTO(movie2.getMovieId(), 2),
                createSubmitDTO(movie3.getMovieId(), 3),
                createSubmitDTO(movie4.getMovieId(), 4),
                createSubmitDTO(movie5.getMovieId(), 5)
        );

        // Perform POST request
        mockMvc.perform(post("/api/users/{userId}/rankings", testUser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rankings)))
                .andExpect(status().isNoContent()); // 204 No Content on success

        // Verify data saved in DB
        List<UserMovieRanking> savedRankings = userMovieRankingRepository.findByUser(testUser);
        assertEquals(5, savedRankings.size());
        assertFalse(rankingSubmissionLogRepository.findByUser(testUser).isEmpty(), "Submission log should exist for the user");
    }

    @Test
    void submitRankings_userNotFound_returnsNotFound() throws Exception {
        long nonExistentUserId = 999L;
        List<RankingSubmitDTO> rankings = List.of(createSubmitDTO(movie1.getMovieId(), 1)); // Dummy payload

        mockMvc.perform(post("/api/users/{userId}/rankings", nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rankings)))
                .andExpect(status().isNotFound()) // Expect 404 Not Found
                .andExpect(jsonPath("$.message", containsString("User with ID " + nonExistentUserId + " not found")));
    }

    @Test
    void submitRankings_invalidRankingData_returnsBadRequest() throws Exception {
         List<RankingSubmitDTO> rankings = List.of( // Missing rank 2
                 createSubmitDTO(movie1.getMovieId(), 1),
                 createSubmitDTO(movie3.getMovieId(), 3),
                 createSubmitDTO(movie4.getMovieId(), 4),
                 createSubmitDTO(movie5.getMovieId(), 5)
         );

        mockMvc.perform(post("/api/users/{userId}/rankings", testUser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rankings)))
                .andExpect(status().isBadRequest()) // Expect 400 Bad Request
                .andExpect(jsonPath("$.message", containsString("Invalid number of rankings submitted")));
    }

    // --- Tests for GET /api/rankings/results/latest ---

    @Test
    void getLatestRankingResult_resultExists_returnsOk() throws Exception {
        // Arrange: Create and save a dummy RankingResult
        RankingResult dummyResult = new RankingResult();
        dummyResult.setWinningMovie(movie1); // Correct setter
        dummyResult.setAverageRank(1.0);     // Set required non-null field
        dummyResult.setCalculationTimestamp(LocalDateTime.now()); // Correct setter
        rankingResultRepository.saveAndFlush(dummyResult);

        // Act & Assert
        mockMvc.perform(get("/api/rankings/results/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Expect 200 OK
                .andExpect(jsonPath("$.winningMovie.movieId").value(movie1.getMovieId())) 
                .andExpect(jsonPath("$.winningMovie.title").value(movie1.getTitle()));  
                // Add more assertions as needed (e.g., calculationTime)
    }

    @Test
    void getLatestRankingResult_noResult_returnsNotFound() throws Exception {
        // Arrange: Ensure no results exist (done by @Transactional and setup)

        // Act & Assert
        mockMvc.perform(get("/api/rankings/results/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Expect 404 Not Found
    }

    // --- Helper Methods ---
    private Movie createAndSaveMovie(long movieId, String title) {
        Movie movie = new Movie();
        movie.setMovieId(movieId);
        movie.setTitle(title);
        movie.setYear(2024); // Default year
        movie.setGenres(new ArrayList<>()); // Default empty list
        // movie.setActorsList(new ArrayList<>()); // Default empty list
        // movie.setDirectorsList(new ArrayList<>()); // Default empty list
        movie.setOriginallanguage("en"); // Default language
        movie.setTrailerURL("http://example.com/trailer"); // Default URL
        movie.setPosterURL("http://example.com/poster"); // Default URL
        movie.setDescription("Default description"); // Default description
        // Add other necessary fields if Movie entity requires them
        return movieRepository.saveAndFlush(movie);
    }

    private RankingSubmitDTO createSubmitDTO(long movieId, int rank) {
        RankingSubmitDTO dto = new RankingSubmitDTO();
        dto.setMovieId(movieId);
        dto.setRank(rank);
        return dto;
    }
}
