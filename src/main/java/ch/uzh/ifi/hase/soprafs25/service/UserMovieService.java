package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * UserMovieService
 * This class is responsible for handling operations related to a user's movie lists
 * (watchlist and watched movies)
 */
@Service
@Transactional
public class UserMovieService {

    private final Logger log = LoggerFactory.getLogger(UserMovieService.class);
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final MovieService movieService;

    @Autowired
    public UserMovieService(@Qualifier("userRepository") UserRepository userRepository,
                           @Qualifier("movieRepository") MovieRepository movieRepository,
                           MovieService movieService) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.movieService = movieService;
    }

    /**
     * Get the watchlist for a user
     * 
     * @param userId the ID of the user
     * @return list of movies in the user's watchlist
     */
    public List<Movie> getWatchlist(Long userId) {
        User user = getUserById(userId);
        if (user.getWatchlist() == null) {
            user.setWatchlist(new ArrayList<>());
            userRepository.save(user);
        }
        return user.getWatchlist();
    }

    /**
     * Add a movie to the user's watchlist
     * 
     * @param userId the ID of the user
     * @param movieId the ID of the movie to add
     * @param requesterToken the token of the user making the request (for authorization)
     * @return the updated watchlist
     */
    public List<Movie> addToWatchlist(Long userId, Long movieId, String requesterToken) {
        User user = getUserById(userId);
        // Authorization check
        authorizeUserAction(user, requesterToken);
        // Fetch full movie details and save to DB if not already present
        Movie movie = getAndSaveMovieById(movieId);
        // Add to user's watchlist as usual
        List<Movie> watchlist = getWatchlist(userId);
        if (isMovieInList(watchlist, movieId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Movie is already in your watchlist");
        }
        watchlist.add(movie);
        user.setWatchlist(watchlist);
        userRepository.save(user);
        return user.getWatchlist();
    }

    /**
     * Remove a movie from the user's watchlist
     * 
     * @param userId the ID of the user
     * @param movieId the ID of the movie to remove
     * @param requesterToken the token of the user making the request (for authorization)
     * @return the updated watchlist
     */
    public List<Movie> removeFromWatchlist(Long userId, Long movieId, String requesterToken) {
        User user = getUserById(userId);
        
        // Authorization check
        authorizeUserAction(user, requesterToken);
        
        // Get the watchlist (ensures initialization)
        List<Movie> watchlist = getWatchlist(userId);
        
        // Check if watchlist is empty
        if (watchlist.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Watchlist is empty");
        }
        
        // Remove movie from watchlist
        if (!removeMovieFromList(watchlist, movieId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Movie not found in watchlist");
        }
        
        user.setWatchlist(watchlist);
        userRepository.save(user);
        return user.getWatchlist();
    }

    /**
     * Get the watched movies list for a user
     * 
     * @param userId the ID of the user
     * @return list of movies the user has watched
     */
    public List<Movie> getWatchedMovies(Long userId) {
        User user = getUserById(userId);
        if (user.getWatchedMovies() == null) {
            user.setWatchedMovies(new ArrayList<>());
            userRepository.save(user);
        }
        return user.getWatchedMovies();
    }

    /**
     * Add a movie to the user's watched movies list
     * 
     * @param userId the ID of the user
     * @param movieId the ID of the movie to add
     * @param requesterToken the token of the user making the request (for authorization)
     * @return the updated watched movies list
     */
    public List<Movie> addToWatchedMovies(Long userId, Long movieId, String requesterToken) {
        User user = getUserById(userId);
        // Authorization check
        authorizeUserAction(user, requesterToken);
        // Fetch full movie details and save to DB if not already present
        Movie movie = getAndSaveMovieById(movieId);
        // Add to user's watched movies as usual
        List<Movie> watchedMovies = getWatchedMovies(userId);
        if (isMovieInList(watchedMovies, movieId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Movie is already in your watched movies list");
        }
        watchedMovies.add(movie);
        user.setWatchedMovies(watchedMovies);
        // Remove from watchlist if present
        List<Movie> watchlist = getWatchlist(userId);
        removeMovieFromList(watchlist, movieId);
        user.setWatchlist(watchlist);
        userRepository.save(user);
        return user.getWatchedMovies();
    }

    /**
     * Remove a movie from the user's watched movies list
     * 
     * @param userId the ID of the user
     * @param movieId the ID of the movie to remove
     * @param requesterToken the token of the user making the request (for authorization)
     * @return the updated watched movies list
     */
    public List<Movie> removeFromWatchedMovies(Long userId, Long movieId, String requesterToken) {
        User user = getUserById(userId);
        
        // Authorization check
        authorizeUserAction(user, requesterToken);
        
        // Get the watched movies list (ensures initialization)
        List<Movie> watchedMovies = getWatchedMovies(userId);
        
        // Check if watched movies list is empty
        if (watchedMovies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Watched movies list is empty");
        }
        
        // Remove movie from watched movies
        if (!removeMovieFromList(watchedMovies, movieId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Movie not found in watched movies list");
        }
        
        user.setWatchedMovies(watchedMovies);
        userRepository.save(user);
        return user.getWatchedMovies();
    }

    /**
     * Helper method to get a user by ID
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "User not found with ID: " + userId));
    }

    /**
     * Helper method to get and save a movie by ID
     */
    private Movie getAndSaveMovieById(Long movieId) {
        Movie movie = movieService.getMovieById(movieId);
        return movieService.saveMovie(movie);
    }

    /**
     * Helper method to check if a movie is already in a list
     */
    private boolean isMovieInList(List<Movie> movieList, Long movieId) {
        if (movieList == null) {
            return false;
        }
        
        return movieList.stream()
            .anyMatch(movie -> movie.getMovieId() == movieId);
    }

    /**
     * Helper method to remove a movie from a list
     * 
     * @return true if the movie was found and removed, false otherwise
     */
    private boolean removeMovieFromList(List<Movie> movieList, Long movieId) {
        if (movieList == null) {
            return false;
        }
        
        // Find the movie in the list
        Movie movieToRemove = null;
        for (Movie movie : movieList) {
            if (movie.getMovieId() == movieId) {
                movieToRemove = movie;
                break;
            }
        }
        
        // Remove the movie if found
        if (movieToRemove != null) {
            movieList.remove(movieToRemove);
            return true;
        }
        
        return false;
    }

    /**
     * Helper method to authorize a user action
     * Ensures that only the owner of the account can modify their lists
     */
    private void authorizeUserAction(User user, String requesterToken) {
        // Get the user making the request
        User requester = userRepository.findByToken(requesterToken);
        
        // Check if requester is valid
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                "Invalid token - Authentication required");
        }
        
        // Check if requester is the owner of the account
        if (!requester.getUserId().equals(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You are not authorized to modify this user's movie lists");
        }
    }
}