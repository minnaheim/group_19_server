package ch.uzh.ifi.hase.soprafs25.service;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MoviePoolRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;

@Service
@Transactional
public class MoviePoolService {


    private final MoviePoolRepository moviePoolRepository;
    private final GroupRepository groupRepository;
    private final MovieRepository movieRepository;


    @Autowired
    public MoviePoolService(MoviePoolRepository moviePoolRepository, GroupRepository groupRepository, MovieRepository movieRepository) {
        
        this.moviePoolRepository = moviePoolRepository;
        this.groupRepository = groupRepository;
        this.movieRepository = movieRepository;
        // this.groupService = groupService;
    }

    public MoviePool createMoviePool(Group group) {

        MoviePool moviePool = new MoviePool();
        moviePool.setGroup(group);
        moviePool.setMovies(new ArrayList<>());
        moviePool.setLastUpdated(LocalDateTime.now());

        return moviePoolRepository.save(moviePool);
    }

    public MoviePool getMoviePool(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // check if user is a member of the group
        boolean isMember = isMemberOfGroup(group, userId);
        
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this group");
        }

        MoviePool moviePool = moviePoolRepository.findByGroup_GroupId(groupId);
        if (moviePool == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie pool not found for this group");
        }

        return moviePool;
    }


    public MoviePool addMovie(Long groupId, Long movieId, Long userId) {
    Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    if (group.getPhase() != Group.GroupPhase.POOL) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only add movies in POOL phase");
    }

        boolean isMember = isMemberOfGroup(group, userId);

        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this group");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        MoviePool moviePool = moviePoolRepository.findByGroup_GroupId(groupId);
        if (moviePool == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie pool not found for this group");
        }

        // check if user has already added 2 movies
        if (moviePool.getMoviesAddedByUser(userId) >= 2) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User has already added maximum number of movies (2) - delete at least one before adding one more");
        }

        // check if movie is already in the pool
        // I'm not sure what has to be done if a movie is already in the pool - exception or just a message
        if (moviePool.getMovies().contains(movie)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Movie is already in the pool");
        }

        moviePool.addMovie(movie, userId);
        moviePool.setLastUpdated(LocalDateTime.now());
        return moviePoolRepository.save(moviePool);
    }

    // i decided that anyhow user will click "add" button separately for each movie
    
    // public MoviePool addMovies(Long groupId, List<Long> movieIds, Long userId) {
    //     Group group = groupRepository.findById(groupId)
    //             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

    //     boolean isMember = groupService.isUserMemberOfGroup(group, userId);

        
    //     if (!isMember) {
    //         throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this group");
    //     }

    //     MoviePool moviePool = moviePoolRepository.findByGroup_GroupId(groupId);
    //     if (moviePool == null) {
    //         throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie pool not found for this group");
    //     }

    //     // check if adding these movies would violate the limit
    //     int currentMovies = moviePool.getMoviesAddedByUser(userId);
    //     if (currentMovies + movieIds.size() > 2) {
    //         throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
    //             String.format("Adding these movies would exceed the limit of 2 movies. You have already added %d movies.", currentMovies));
    //     }

    //     List<Movie> movies = movieRepository.findAllById(movieIds);
    //     // if some movies are not found
    //     if (movies.size() != movieIds.size()) {
    //         throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more movies not found");
    //     }
    //     // adding all movies that aren't already in the pool
    //     boolean changed = false;
    //     for (Movie movie : movies) {
    //         if (!moviePool.getMovies().contains(movie)) {
    //             moviePool.addMovie(movie, userId);
    //             changed = true;
    //         }
    //     }

    //     if (changed) {
    //         moviePool.setLastUpdated(LocalDateTime.now());
    //     }
    //     return moviePoolRepository.save(moviePool);
    // }

    public MoviePool removeMovie(Long groupId, Long movieId, Long userId) {
    Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    if (group.getPhase() != Group.GroupPhase.POOL) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only remove movies in POOL phase");
    }

        boolean isMember = isMemberOfGroup(group, userId);
        
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this group");
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        MoviePool moviePool = moviePoolRepository.findByGroup_GroupId(groupId);
        if (moviePool == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie pool not found for this group");
        }

        // check if the movie was added by this user
        if (!moviePool.getUserAddedMovies().get(movie).equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only remove movies that you added");
        }
        moviePool.removeMovie(movie);
        moviePool.setLastUpdated(LocalDateTime.now());
        return moviePoolRepository.save(moviePool);
    }

    private Boolean isMemberOfGroup(Group group, Long userId){
        return group.getMembers().stream().anyMatch(member -> member.getUserId().equals(userId));
    }
} 