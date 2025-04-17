package ch.uzh.ifi.hase.soprafs25.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static java.util.Arrays.asList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MoviePoolRepository;
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
    private GroupRepository groupRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MoviePoolRepository moviePoolRepository;

    @Autowired
    private UserMovieRankingRepository userMovieRankingRepository;

    @Autowired
    private RankingResultRepository rankingResultRepository;

    @Autowired
    private RankingSubmissionLogRepository rankingSubmissionLogRepository; // For verification

    private User testUser;
    private User testUser2;
    private Movie movie1, movie2, movie3, movie4, movie5;
    private List<Movie> availableMovies;
    private Group testGroup;

    @BeforeEach
    void setupDatabase() {
        // Clean up before test - @Transactional handles rollback, but explicit delete can be safer
        userMovieRankingRepository.deleteAll();
        rankingResultRepository.deleteAll();
        rankingSubmissionLogRepository.deleteAll();
        userRepository.deleteAll(); // Clear users if needed
        // Need to delete MoviePool before Group due to foreign key constraint
        moviePoolRepository.deleteAll();
        groupRepository.deleteAll(); // Clear groups
        movieRepository.deleteAll(); // Clear movies if needed

        // Create test user
        testUser = new User();
        testUser.setUsername("testUserRankController");
        testUser.setPassword("password"); // Assuming password is required
        testUser.setEmail("rankcontroller@example.com");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("rankToken");   // Assuming token is required
        testUser = userRepository.saveAndFlush(testUser);

        testUser2 = new User();
        testUser2.setUsername("testUser2RankController");
        testUser2.setPassword("password"); // Assuming password is required
        testUser2.setEmail("rankcontroller2@example.com");
        testUser2.setStatus(UserStatus.ONLINE);
        testUser2.setToken("rankToken2");   // Assuming token is required
        testUser2 = userRepository.saveAndFlush(testUser2);

        // Create test group
        testGroup = new Group();
        testGroup.setGroupName("Test Ranking Group"); // Use correct setter
        testGroup.setCreator(testUser); // Set the creator
        testGroup.setMembers(new ArrayList<>()); // Initialize the list
        testGroup.getMembers().add(testUser); // Add user to the member list
        testGroup.getMembers().add(testUser2); // Add second user to the member list
        testGroup = groupRepository.saveAndFlush(testGroup);

        // Create movies for the pool
        movie1 = createAndSaveMovie(101L, "Movie A");
        movie2 = createAndSaveMovie(102L, "Movie B");
        movie3 = createAndSaveMovie(103L, "Movie C");
        movie4 = createAndSaveMovie(104L, "Movie D");
        movie5 = createAndSaveMovie(105L, "Movie E");
        availableMovies = List.of(movie1, movie2, movie3, movie4, movie5);

        // Create and associate MoviePool
        MoviePool moviePool = new MoviePool();
        moviePool.setGroup(testGroup);
        moviePool.setMovies(new ArrayList<>(availableMovies)); // Add movies to pool
        moviePool.setLastUpdated(LocalDateTime.now());
        moviePoolRepository.saveAndFlush(moviePool);

        // Associate pool with group and save group again
        testGroup.setMoviePool(moviePool);
        testGroup = groupRepository.saveAndFlush(testGroup);

    }

    // --- Tests for POST /groups/{groupId}/users/{userId}/rankings ---

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
        mockMvc.perform(post("/groups/{groupId}/users/{userId}/rankings", testGroup.getGroupId(), testUser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rankings)))
                .andExpect(status().isNoContent()); // 204 No Content on success

        // Verify data was saved (check UserMovieRanking and RankingSubmissionLog)
        List<UserMovieRanking> savedRankings = userMovieRankingRepository.findByUserAndGroup(testUser, testGroup);
        assertEquals(5, savedRankings.size(), "Expected 5 rankings to be saved for the user in the group");
        // Check submission log for the user only, as it doesn't store group directly
        assertFalse(rankingSubmissionLogRepository.findByUser(testUser).isEmpty(), "Submission log should exist for the user");
    }

    @Test
    void submitRankings_userNotFound_returnsNotFound() throws Exception {
        long nonExistentUserId = 999L;
        List<RankingSubmitDTO> rankings = List.of(createSubmitDTO(movie1.getMovieId(), 1)); // Dummy payload

        mockMvc.perform(post("/groups/{groupId}/users/{userId}/rankings", testGroup.getGroupId(), nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rankings))) // Use correct path
                .andExpect(status().isNotFound()) // Expect 404 Not Found
                .andExpect(jsonPath("$.message", containsString("User with ID " + nonExistentUserId + " not found")));
    }

    @Test
    void submitRankings_invalidRankingData_returnsBadRequest() throws Exception {
         // Invalid: Correct size (5), but duplicate rank 3
         List<RankingSubmitDTO> rankings = List.of(
                 createSubmitDTO(movie1.getMovieId(), 1),
                 createSubmitDTO(movie2.getMovieId(), 2),
                 createSubmitDTO(movie3.getMovieId(), 3),
                 createSubmitDTO(movie4.getMovieId(), 3), // Duplicate rank
                 createSubmitDTO(movie5.getMovieId(), 4)
         );

        mockMvc.perform(post("/groups/{groupId}/users/{userId}/rankings", testGroup.getGroupId(), testUser.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rankings))) // Use correct path
                .andExpect(status().isBadRequest()) // Expect 400 Bad Request
                .andExpect(jsonPath("$.message", containsString("Invalid ranking: Duplicate rank"))); // Check actual error message if known
    }

    // --- Tests for GET /groups/{groupId}/rankings/result ---

    @Test
    void getLatestRankingResult_resultExists_returnsOk() throws Exception {
        // Arrange: Create and save a dummy RankingResult associated with the group
        RankingResult dummyResult = new RankingResult();
        dummyResult.setGroup(testGroup); // Associate with the test group
        dummyResult.setWinningMovie(movie1); // Correct setter
        dummyResult.setAverageRank(1.0);     // Set required non-null field
        dummyResult.setCalculationTimestamp(LocalDateTime.now()); // Correct setter
        rankingResultRepository.saveAndFlush(dummyResult);

        // Act & Assert
        mockMvc.perform(get("/groups/{groupId}/rankings/result", testGroup.getGroupId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Expect 200 OK
                .andExpect(jsonPath("$.winningMovie.movieId").value(movie1.getMovieId())) 
                .andExpect(jsonPath("$.winningMovie.title").value(movie1.getTitle()));  
                // Add more assertions as needed (e.g., calculationTime)
    }

    @Test
    void getLatestRankingResult_noResult_returnsNotFound() throws Exception {
        // Arrange: Ensure no results exist for this group (done by @Transactional and setup)

        // Act & Assert
        mockMvc.perform(get("/groups/{groupId}/rankings/result", testGroup.getGroupId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Expect 404 Not Found
    }

    // --- NEW TEST FOR COMPLETE RANKING RESULT ---
    @Test
    void getGroupCompleteRankingResult_validInput_returnsSortedListWithAverages() throws Exception {
        // Setup: Submit rankings from two users
        // User 1: A(1), B(2), C(3), D(4), E(5)
        userMovieRankingRepository.saveAll(asList(
            createRanking(testUser, testGroup, movie1, 1),
            createRanking(testUser, testGroup, movie2, 2),
            createRanking(testUser, testGroup, movie3, 3),
            createRanking(testUser, testGroup, movie4, 4),
            createRanking(testUser, testGroup, movie5, 5)
        ));
        userMovieRankingRepository.flush();
        // User 2: A(2), B(1), C(3), D(4), E(5)
        userMovieRankingRepository.saveAll(asList(
            createRanking(testUser2, testGroup, movie1, 2),
            createRanking(testUser2, testGroup, movie2, 1),
            createRanking(testUser2, testGroup, movie3, 3),
            createRanking(testUser2, testGroup, movie4, 4),
            createRanking(testUser2, testGroup, movie5, 5)
        ));
        userMovieRankingRepository.flush();

        // Expected Averages: A=(1+2)/2=1.5, B=(2+1)/2=1.5, C=(3+3)/2=3.0, D=(4+4)/2=4.0, E=(5+5)/2=5.0
        // Expected Order: A (1.5), B (1.5), C (3.0), D (4.0), E (5.0) - Tie-breaking not specified, so order of A/B might vary
        // Let's assume stable sort or check possibilities

        // Action & Assert
        mockMvc.perform(get("/groups/{groupId}/rankings/details", testGroup.getGroupId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5))) // Expecting 5 movies
                // Check first element (either Movie A or B with avg 1.5)
                .andExpect(jsonPath("$[0].movie.movieId", equalTo((int)movie1.getMovieId())))
                .andExpect(jsonPath("$[0].averageRank", equalTo(1.5)))
                .andExpect(jsonPath("$[1].movie.movieId", equalTo((int)movie2.getMovieId())))
                .andExpect(jsonPath("$[1].averageRank", equalTo(1.5)))
                .andExpect(jsonPath("$[2].movie.movieId", equalTo((int)movie3.getMovieId())))
                .andExpect(jsonPath("$[2].averageRank", equalTo(3.0)))
                .andExpect(jsonPath("$[3].movie.movieId", equalTo((int)movie4.getMovieId())))
                .andExpect(jsonPath("$[3].averageRank", equalTo(4.0)))
                .andExpect(jsonPath("$[4].movie.movieId", equalTo((int)movie5.getMovieId())))
                .andExpect(jsonPath("$[4].averageRank", equalTo(5.0)));

        // Ensure A and B are distinct in the first two positions
        // This is harder with jsonPath, might need custom matcher or parsing response
    }

    @Test
    void getGroupCompleteRankingResult_noRankingsSubmitted_returnsOkAndListWithNullAverages() throws Exception {
        // Setup: No rankings submitted, just the group and movie pool exist

        // Action & Assert
        mockMvc.perform(get("/groups/{groupId}/rankings/details", testGroup.getGroupId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5))) // Expecting 5 movies
                // Check that averageRanks are null and movies are present (order might be alphabetical)
                .andExpect(jsonPath("$[0].movie.movieId", equalTo((int)movie1.getMovieId())))
                .andExpect(jsonPath("$[0].averageRank").doesNotExist()) // Average rank should be null or absent
                .andExpect(jsonPath("$[1].movie.movieId", equalTo((int)movie2.getMovieId())))
                .andExpect(jsonPath("$[1].averageRank").doesNotExist()); // Average rank should be null or absent
                // Optional: Check alphabetical sort order if needed
    }

    @Test
    void getGroupCompleteRankingResult_invalidGroupId_returnsNotFound() throws Exception {
        // Action & Assert
        mockMvc.perform(get("/groups/{groupId}/rankings/details", 999L)) // Non-existent ID
                .andExpect(status().isNotFound());
    }

    // --- Helper Methods ---
    private Movie createAndSaveMovie(long movieId, String title) {
        Movie movie = new Movie();
        movie.setMovieId(movieId);
        movie.setTitle(title);
        movie.setYear(2024); // Default year
        movie.setGenres(new ArrayList<>()); // Default empty list
        movie.setActors(new ArrayList<>()); // Default empty list
        movie.setDirectors(new ArrayList<>()); // Default empty list
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

    private UserMovieRanking createRanking(User user, Group group, Movie movie, int rank) {
        UserMovieRanking ranking = new UserMovieRanking();
        ranking.setUser(user);
        ranking.setGroup(group);
        ranking.setMovie(movie);
        ranking.setRank(rank);
        return ranking;
    }
}
