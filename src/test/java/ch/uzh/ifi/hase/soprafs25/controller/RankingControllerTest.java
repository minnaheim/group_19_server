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
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingResultsDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieRankGetDTO;
import ch.uzh.ifi.hase.soprafs25.service.RankingService;

import org.springframework.boot.test.mock.mockito.SpyBean;
import static org.mockito.Mockito.doReturn;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
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

    @SpyBean
    private RankingService rankingService;

    private User testUser;
    private User testUser2;
    private Movie movie1, movie2, movie3, movie4, movie5;
    private List<Movie> availableMovies;
    private Group testGroup;

    @BeforeEach
    void setupDatabase() {
        // Clean up before test - @Transactional handles rollback, but explicit delete
        // can be safer
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
        testUser.setToken("rankToken"); // Assuming token is required
        testUser = userRepository.saveAndFlush(testUser);

        testUser2 = new User();
        testUser2.setUsername("testUser2RankController");
        testUser2.setPassword("password"); // Assuming password is required
        testUser2.setEmail("rankcontroller2@example.com");
        testUser2.setStatus(UserStatus.ONLINE);
        testUser2.setToken("rankToken2"); // Assuming token is required
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
        // Set group phase to VOTING
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.saveAndFlush(testGroup);
        List<RankingSubmitDTO> rankings = List.of(
                createSubmitDTO(movie1.getMovieId(), 1),
                createSubmitDTO(movie2.getMovieId(), 2),
                createSubmitDTO(movie3.getMovieId(), 3),
                createSubmitDTO(movie4.getMovieId(), 4),
                createSubmitDTO(movie5.getMovieId(), 5));

        // Perform POST request
        mockMvc.perform(post("/groups/{groupId}/users/{userId}/rankings", testGroup.getGroupId(), testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rankings)))
                .andExpect(status().isNoContent()); // 204 No Content on success

        // Verify data was saved (check UserMovieRanking and RankingSubmissionLog)
        List<UserMovieRanking> savedRankings = userMovieRankingRepository.findByUserAndGroup(testUser, testGroup);
        assertEquals(5, savedRankings.size(), "Expected 5 rankings to be saved for the user in the group");
        // Check submission log for the user only, as it doesn't store group directly
        assertFalse(rankingSubmissionLogRepository.findByUser(testUser).isEmpty(),
                "Submission log should exist for the user");
    }

    @Test
    void submitRankings_userNotFound_returnsNotFound() throws Exception {
        // Set group phase to VOTING
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.saveAndFlush(testGroup);
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
        // Set group phase to VOTING
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.saveAndFlush(testGroup);
        // Invalid: Correct size (5), but duplicate rank 3
        List<RankingSubmitDTO> rankings = List.of(
                createSubmitDTO(movie1.getMovieId(), 1),
                createSubmitDTO(movie2.getMovieId(), 2),
                createSubmitDTO(movie3.getMovieId(), 3),
                createSubmitDTO(movie4.getMovieId(), 3), // Duplicate rank
                createSubmitDTO(movie5.getMovieId(), 4));

        mockMvc.perform(post("/groups/{groupId}/users/{userId}/rankings", testGroup.getGroupId(), testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rankings))) // Use correct path
                .andExpect(status().isBadRequest()) // Expect 400 Bad Request
                .andExpect(jsonPath("$.message", containsString("Invalid ranking: Duplicate rank"))); // Check actual
                                                                                                      // error message
                                                                                                      // if known
    }

    // --- Tests for GET /groups/{groupId}/rankings/results ---

    @Test
    void getRankingResults_valid_returnsOkAndCorrectPayload() throws Exception {
        // Stub service to return dummy DTO
        RankingResultsDTO stubDto = new RankingResultsDTO();
        stubDto.setResultId(123L);
        stubDto.setNumberOfVoters(2);
        stubDto.setCalculatedAt("2025-01-01T00:00:00Z");
        MovieRankGetDTO win = new MovieRankGetDTO();
        win.setMovieId(movie1.getMovieId()); win.setTitle(movie1.getTitle());
        stubDto.setWinningMovie(win);
        stubDto.setDetailedResults(List.of());
        doReturn(stubDto).when(rankingService).getRankingResults(testGroup.getGroupId());

        mockMvc.perform(get("/groups/{groupId}/rankings/results", testGroup.getGroupId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultId").value(123))
                .andExpect(jsonPath("$.numberOfVoters").value(2))
                .andExpect(jsonPath("$.winningMovie.movieId").value((int) movie1.getMovieId()))
                .andExpect(jsonPath("$.detailedResults", hasSize(0)));
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
