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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MoviePoolServiceTest {
    @Mock
    private MoviePoolRepository moviePoolRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private MovieRepository movieRepository;
    @InjectMocks
    private MoviePoolService moviePoolService;

    private Group group;
    private Movie movie;
    private MoviePool moviePool;
    private User user;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        group = new Group();
        group.setGroupId(1L);
        group.setMembers(new ArrayList<>());
        user = new User();
        user.setUserId(10L);
        group.getMembers().add(user);
        movie = new Movie();
        movie.setMovieId(100L);
        moviePool = new MoviePool();
        moviePool.setGroup(group);
        moviePool.setMovies(new ArrayList<>());
        // simulate user-movie mapping
        Map<Movie, Long> userAddedMovies = new HashMap<>();
        moviePool.setUserAddedMovies(userAddedMovies);
    }

    @Test
    void addMovie_success_inPoolPhase() {
        group.setPhase(Group.GroupPhase.POOL);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(movieRepository.findById(100L)).thenReturn(Optional.of(movie));
        when(moviePoolRepository.findByGroup_GroupId(1L)).thenReturn(moviePool);
        when(moviePoolRepository.save(any(MoviePool.class))).thenReturn(moviePool);

        MoviePool result = moviePoolService.addMovie(1L, 100L, 10L);
        assertNotNull(result);
        verify(moviePoolRepository).save(moviePool);
    }

    @Test
    void addMovie_conflict_wrongPhase() {
        group.setPhase(Group.GroupPhase.VOTING);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                moviePoolService.addMovie(1L, 100L, 10L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void removeMovie_success_inPoolPhase() {
        group.setPhase(Group.GroupPhase.POOL);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(movieRepository.findById(100L)).thenReturn(Optional.of(movie));
        when(moviePoolRepository.findByGroup_GroupId(1L)).thenReturn(moviePool);
        // Simulate user added the movie
        moviePool.getMovies().add(movie);
        moviePool.getUserAddedMovies().put(movie, 10L);
        when(moviePoolRepository.save(any(MoviePool.class))).thenReturn(moviePool);

        MoviePool result = moviePoolService.removeMovie(1L, 100L, 10L);
        assertNotNull(result);
        verify(moviePoolRepository).save(moviePool);
    }

    @Test
    void removeMovie_conflict_wrongPhase() {
        group.setPhase(Group.GroupPhase.RESULTS);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                moviePoolService.removeMovie(1L, 100L, 10L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void addMovie_forbidden_notMember() {
        group.setPhase(Group.GroupPhase.POOL);
        group.getMembers().clear(); // user not in group
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                moviePoolService.addMovie(1L, 100L, 10L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void addMovie_conflict_movieAlreadyInPool() {
        group.setPhase(Group.GroupPhase.POOL);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(movieRepository.findById(100L)).thenReturn(Optional.of(movie));
        when(moviePoolRepository.findByGroup_GroupId(1L)).thenReturn(moviePool);
        moviePool.getMovies().add(movie);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                moviePoolService.addMovie(1L, 100L, 10L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void addMovie_forbidden_userExceedsLimit() {
        group.setPhase(Group.GroupPhase.POOL);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(movieRepository.findById(100L)).thenReturn(Optional.of(movie));
        when(moviePoolRepository.findByGroup_GroupId(1L)).thenReturn(moviePool);
        // Simulate user has already added 2 movies
        Movie m1 = new Movie(); m1.setMovieId(101L);
        Movie m2 = new Movie(); m2.setMovieId(102L);
        moviePool.getMovies().add(m1);
        moviePool.getMovies().add(m2);
        moviePool.getUserAddedMovies().put(m1, 10L);
        moviePool.getUserAddedMovies().put(m2, 10L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                moviePoolService.addMovie(1L, 100L, 10L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void removeMovie_forbidden_notAddedByUser() {
        group.setPhase(Group.GroupPhase.POOL);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(movieRepository.findById(100L)).thenReturn(Optional.of(movie));
        when(moviePoolRepository.findByGroup_GroupId(1L)).thenReturn(moviePool);
        moviePool.getMovies().add(movie);
        moviePool.getUserAddedMovies().put(movie, 99L); // different user
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                moviePoolService.removeMovie(1L, 100L, 10L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
