 package ch.uzh.ifi.hase.soprafs25.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MoviePoolRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserMovieRankingRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RankingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private User testUser;
    private String testToken;
    private Group testGroup;
    private Movie movie1, movie2, movie3;
    private MoviePool moviePool;

    @BeforeEach
    void setup() {
        // сlean up repositories
        userMovieRankingRepository.deleteAll();
        moviePoolRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
        movieRepository.deleteAll();

        // сreate test user
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@mail.com");
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ONLINE);
        testToken = "testToken";
        testUser.setToken(testToken);
        testUser = userRepository.saveAndFlush(testUser);

        // сreate test movies
        movie1 = createAndSaveMovie(101L, "Movie 1");
        movie2 = createAndSaveMovie(102L, "Movie 2");
        movie3 = createAndSaveMovie(103L, "Movie 3");

        // сreate test group
        testGroup = new Group();
        testGroup.setGroupName("Test Group");
        testGroup.setCreator(testUser);
        testGroup.setMembers(new ArrayList<>());
        testGroup.getMembers().add(testUser);
        testGroup.setPhase(Group.GroupPhase.VOTING);
        testGroup = groupRepository.saveAndFlush(testGroup);

        // сreate movie pool
        moviePool = new MoviePool();
        moviePool.setGroup(testGroup);
        moviePool.setMovies(new ArrayList<>(List.of(movie1, movie2, movie3)));
        moviePool.setLastUpdated(LocalDateTime.now());
        moviePool = moviePoolRepository.saveAndFlush(moviePool);
        testGroup.setMoviePool(moviePool);
        testGroup = groupRepository.saveAndFlush(testGroup);
    }

    // successful submit of rankings  
    @Test
    void submitGroupUserRankings_validInput_returnsNoContent() throws Exception {
        // create rankings
        List<RankingSubmitDTO> rankings = List.of(
            createSubmitDTO(movie1.getMovieId(), 1),
            createSubmitDTO(movie2.getMovieId(), 2),
            createSubmitDTO(movie3.getMovieId(), 3)
        );

        // send the request
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/users/{userId}/rankings", 
                testGroup.getGroupId(), testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rankings)))
                .andExpect(status().isNoContent());

        // check rankings was saved
        List<UserMovieRanking> savedRankings = userMovieRankingRepository.findByUserAndGroup(testUser, testGroup);
        assertEquals(3, savedRankings.size());
    }

    // submission of rankings fails because of double rank of a movie
    // technically it's impossible in UI, but just to make sure it works as supposed
    @Test
    void submitGroupUserRankings_invalidRankings_returnsBadRequest() throws Exception {
        // invalid input: duplicate ranking
        List<RankingSubmitDTO> rankings = List.of(
            createSubmitDTO(movie1.getMovieId(), 1),
            createSubmitDTO(movie2.getMovieId(), 1),
            createSubmitDTO(movie3.getMovieId(), 2)
        );
        // send the request
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/users/{userId}/rankings", 
                testGroup.getGroupId(), testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rankings)))
                .andExpect(status().isBadRequest());
    }


    // successful results fetch
    @Test
    void getRankingResults_validGroup_returnsResults() throws Exception {
        // create some rankings
        List<RankingSubmitDTO> rankings = List.of(
            createSubmitDTO(movie1.getMovieId(), 1),
            createSubmitDTO(movie2.getMovieId(), 2),
            createSubmitDTO(movie3.getMovieId(), 3)
        );

        // send request to save rankings
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/users/{userId}/rankings", 
                testGroup.getGroupId(), testUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rankings)))
                .andExpect(status().isNoContent());

        // send request to get results
        testGroup.setPhase(Group.GroupPhase.RESULTS);
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/rankings/results", testGroup.getGroupId()))
                .andExpect(status().isOk())
                // only one user has voted
                .andExpect(jsonPath("$.numberOfVoters").value(1))
                // movie1 is the winner
                .andExpect(jsonPath("$.winningMovie.movieId").value((int) movie1.getMovieId()));
    }

    @Test
    void getRankingResults_notResultsPhase_returnsConflict() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/rankings/results", testGroup.getGroupId()))
                .andExpect(status().isConflict());
    }

    // additional methods 
    private Movie createAndSaveMovie(Long movieId, String title) {
        Movie movie = new Movie();
        movie.setMovieId(movieId);
        movie.setTitle(title);
        movie.setYear(2025);
        movie.setGenres(new ArrayList<>());
        movie.setActors(new ArrayList<>());
        movie.setDirectors(new ArrayList<>());
        movie.setOriginallanguage("en");
        movie.setTrailerURL("http://test.com/trailer");
        movie.setPosterURL("http://test.com/poster");
        movie.setDescription("Test description");
        return movieRepository.saveAndFlush(movie);
    }

    private RankingSubmitDTO createSubmitDTO(Long movieId, Integer rank) {
        RankingSubmitDTO dto = new RankingSubmitDTO();
        dto.setMovieId(movieId);
        dto.setRank(rank);
        return dto;
    }
}