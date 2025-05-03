package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MoviePoolRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MoviePoolServiceIntegrationTest {
    @Autowired
    private MoviePoolService moviePoolService;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private MoviePoolRepository moviePoolRepository;
    @Autowired
    private ch.uzh.ifi.hase.soprafs25.repository.UserRepository userRepository;

    private Group group;
    private User user;
    private Movie movie;

    @BeforeEach
    void setup() {
        // Create and persist a user
        user = new User();
        user.setUsername("integrationUser");
        user.setEmail("integration@example.com");
        user.setPassword("password");
        user.setStatus(ch.uzh.ifi.hase.soprafs25.constant.UserStatus.ONLINE);
        user = userRepository.saveAndFlush(user);
        // Create and persist a group
        group = new Group();
        group.setGroupName("integrationGroup");
        group.setPhase(Group.GroupPhase.POOL);
        group.setMembers(new java.util.ArrayList<>());
        group.getMembers().add(user);
        group.setCreator(user);
        group = groupRepository.saveAndFlush(group);
        // Create and persist a movie
        movie = new Movie();
        movie.setMovieId(200L);
        movie.setTitle("Integration Movie");
        movie = movieRepository.saveAndFlush(movie);
        // Create and persist a movie pool
        MoviePool pool = new MoviePool();
        pool.setGroup(group);
        pool.setMovies(new java.util.ArrayList<>());
        pool = moviePoolRepository.saveAndFlush(pool);
        group.setMoviePool(pool);
        groupRepository.saveAndFlush(group);
    }

    @Test
    void addMovie_inPoolPhase_success() {
        MoviePool result = moviePoolService.addMovie(group.getGroupId(), movie.getMovieId(), user.getUserId());
        assertTrue(result.getMovies().contains(movie));
    }

    @Test
    void addMovie_wrongPhase_conflict() {
        group.setPhase(Group.GroupPhase.VOTING);
        groupRepository.saveAndFlush(group);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> moviePoolService.addMovie(group.getGroupId(), movie.getMovieId(), user.getUserId()));
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void removeMovie_inPoolPhase_success() {
        // First add movie
        moviePoolService.addMovie(group.getGroupId(), movie.getMovieId(), user.getUserId());
        MoviePool result = moviePoolService.removeMovie(group.getGroupId(), movie.getMovieId(), user.getUserId());
        assertFalse(result.getMovies().contains(movie));
    }

    @Test
    void removeMovie_wrongPhase_conflict() {
        // Add movie in POOL phase
        moviePoolService.addMovie(group.getGroupId(), movie.getMovieId(), user.getUserId());
        // Change phase
        group.setPhase(Group.GroupPhase.RESULTS);
        groupRepository.saveAndFlush(group);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> moviePoolService.removeMovie(group.getGroupId(), movie.getMovieId(), user.getUserId()));
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }
}
