package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MoviePoolRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class GroupControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private MoviePoolRepository moviePoolRepository;

    private User user;
    private Group group;
    private Movie movie;
    private MoviePool pool;
    private String token;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUsername("controllerUser");
        user.setEmail("controller@example.com");
        user.setPassword("pw");
        user.setStatus(ch.uzh.ifi.hase.soprafs25.constant.UserStatus.ONLINE);
        token = java.util.UUID.randomUUID().toString();
        user.setToken(token);
        user = userRepository.saveAndFlush(user);

        group = new Group();
        group.setGroupName("controllerGroup");
        group.setPhase(Group.GroupPhase.POOL);
        group.setMembers(new java.util.ArrayList<>());
        group.getMembers().add(user);
        group.setCreator(user);
        group = groupRepository.saveAndFlush(group);

        movie = new Movie();
        movie.setMovieId(555L);
        movie.setTitle("Controller Test Movie");
        movie = movieRepository.saveAndFlush(movie);

        pool = new MoviePool();
        pool.setGroup(group);
        pool.setMovies(new java.util.ArrayList<>());
        pool = moviePoolRepository.saveAndFlush(pool);
        group.setMoviePool(pool);
        groupRepository.saveAndFlush(group);
        // Ensure userAddedMovies is initialized for test user
        pool.setUserAddedMovies(new java.util.HashMap<>());
        moviePoolRepository.saveAndFlush(pool);
    }

    @Test
    void addMovieToGroupPool_wrongPhase_returnsConflict() throws Exception {
        group.setPhase(Group.GroupPhase.VOTING);
        groupRepository.saveAndFlush(group);
        mockMvc.perform(post("/groups/{groupId}/pool/{movieId}", group.getGroupId(), movie.getMovieId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void addMovieToGroupPool_inPoolPhase_returnsOk() throws Exception {
        group.setPhase(Group.GroupPhase.POOL);
        groupRepository.saveAndFlush(group);
        mockMvc.perform(post("/groups/{groupId}/pool/{movieId}", group.getGroupId(), movie.getMovieId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void removeMovieFromGroupPool_wrongPhase_returnsConflict() throws Exception {
        // First add the movie in POOL phase
        group.setPhase(Group.GroupPhase.POOL);
        groupRepository.saveAndFlush(group);
        mockMvc.perform(post("/groups/{groupId}/pool/{movieId}", group.getGroupId(), movie.getMovieId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        // Change phase
        group.setPhase(Group.GroupPhase.RESULTS);
        groupRepository.saveAndFlush(group);
        mockMvc.perform(delete("/groups/{groupId}/pool/{movieId}", group.getGroupId(), movie.getMovieId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void removeMovieFromGroupPool_inPoolPhase_returnsNoContent() throws Exception {
        // First add the movie in POOL phase
        group.setPhase(Group.GroupPhase.POOL);
        groupRepository.saveAndFlush(group);
        mockMvc.perform(post("/groups/{groupId}/pool/{movieId}", group.getGroupId(), movie.getMovieId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        // Reload pool from DB to ensure userAddedMovies is up-to-date
        pool = moviePoolRepository.findByGroup_GroupId(group.getGroupId());
        // Remove in POOL phase
        mockMvc.perform(delete("/groups/{groupId}/pool/{movieId}", group.getGroupId(), movie.getMovieId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
