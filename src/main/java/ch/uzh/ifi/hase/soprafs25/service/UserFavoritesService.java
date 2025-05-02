package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserFavoritesService
 * This class is responsible for handling operations related to user favorites
 * such as genre favorites and favorite movie.
 */
@Service
@Transactional
public class UserFavoritesService {

    private final Logger log = LoggerFactory.getLogger(UserFavoritesService.class);
    private final UserRepository userRepository;
    private final MovieService movieService;
    private final TMDbService tmdbService;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserFavoritesService(UserRepository userRepository, 
                                 MovieService movieService,
                                 TMDbService tmdbService) {
        this.userRepository = userRepository;
        this.movieService = movieService;
        this.tmdbService = tmdbService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get all available genres from TMDb
     * 
     * @return list of genres as maps with id and name
     */
    public List<Map<String, Object>> getAllGenres() {
        JsonNode genresNode = tmdbService.getGenres();
        List<Map<String, Object>> genres = new ArrayList<>();
        
        if (genresNode != null && genresNode.isArray()) {
            for (JsonNode genreNode : genresNode) {
                Map<String, Object> genre = new HashMap<>();
                genre.put("id", genreNode.get("id").asInt());
                genre.put("name", genreNode.get("name").asText());
                genres.add(genre);
            }
        }
        
        return genres;
    }

    /**
     * Save genre favorites for a user
     * 
     * @param userId the ID of the user
     * @param genreNames list of genre names
     * @param requesterToken the token of the user making the request (for authorization)
     * @return the updated list of genre favorites
     */
    public List<String> saveGenreFavorites(Long userId, List<String> genreNames, String requesterToken) {
        log.info("saveGenreFavorites called with userId={}, genres={}", userId, genreNames);
        User user = getUserById(userId);
        log.info("User found: {}", user != null ? user.getUsername() : "null");

        // Authorization check
        authorizeUserAction(user, requesterToken);
        log.info("Authorization passed");

        // Validate genres against TMDb list
        validateGenres(genreNames);
        log.info("Genres validated: {}", genreNames);

        // Save genre favorites
        user.setFavoriteGenres(genreNames);
        userRepository.save(user);
        log.info("Favorite genres saved for user {}: {}", user.getUsername(), genreNames);

        return user.getFavoriteGenres();
    }

    /**
     * Get genre favorites for a user
     * 
     * @param userId the ID of the user
     * @return list of genre favorites
     */
    public List<String> getGenreFavorites(Long userId) {
        User user = getUserById(userId);
        if (user.getFavoriteGenres() == null) {
            user.setFavoriteGenres(new ArrayList<>());
            userRepository.save(user);
        }
        return user.getFavoriteGenres();
    }

    /**
     * Save favorite movie for a user
     * 
     * @param userId the ID of the user
     * @param movieId the ID of the favorite movie
     * @param requesterToken the token of the user making the request (for authorization)
     * @return the saved Movie object
     */
    public Movie saveFavoriteMovie(Long userId, Long movieId, String requesterToken) {
        User user = getUserById(userId);
        authorizeUserAction(user, requesterToken);
        // Fetch full movie details and save to DB if not already present
        Movie movie = movieService.getMovieById(movieId);
        movie = movieService.saveMovie(movie);
        user.setFavoriteMovie(movie);
        userRepository.save(user);
        return movie;
    }

    /**
     * Get favorite movie for a user
     * 
     * @param userId the ID of the user
     * @return the favorite Movie object, or null if not set
     */
    public Movie getFavoriteMovie(Long userId) {
        User user = getUserById(userId);
        return user.getFavoriteMovie();
    }

    /**
     * Save favorite actors for a user (list clears all if empty)
     */
    public List<String> saveFavoriteActors(Long userId, List<String> actorList, String requesterToken) {
        log.info("saveFavoriteActors called with userId={}, actors={}", userId, actorList);
        User user = getUserById(userId);
        authorizeUserAction(user, requesterToken);
        if (actorList == null) {
            user.setFavoriteActors(new ArrayList<>());
        } else {
            user.setFavoriteActors(actorList);
        }
        userRepository.save(user);
        return user.getFavoriteActors();
    }

    /**
     * Get favorite actors for a user
     *
     * @param userId the ID of the user
     * @return list of favorite actors, empty if none set
     */
    public List<String> getFavoriteActors(Long userId) {
        User user = getUserById(userId);
        if (user.getFavoriteActors() == null) {
            user.setFavoriteActors(new ArrayList<>());
            userRepository.save(user);
        }
        return user.getFavoriteActors();
    }

    /**
     * Save favorite directors for a user (list clears all if empty)
     */
    public List<String> saveFavoriteDirectors(Long userId, List<String> directorList, String requesterToken) {
        log.info("saveFavoriteDirectors called with userId={}, directors={}", userId, directorList);
        User user = getUserById(userId);
        authorizeUserAction(user, requesterToken);
        if (directorList == null) {
            user.setFavoriteDirectors(new ArrayList<>());
        } else {
            user.setFavoriteDirectors(directorList);
        }
        userRepository.save(user);
        return user.getFavoriteDirectors();
    }

    /**
     * Get favorite directors for a user
     *
     * @param userId the ID of the user
     * @return list of favorite directors, empty if none set
     */
    public List<String> getFavoriteDirectors(Long userId) {
        User user = getUserById(userId);
        if (user.getFavoriteDirectors() == null) {
            user.setFavoriteDirectors(new ArrayList<>());
            userRepository.save(user);
        }
        return user.getFavoriteDirectors();
    }

    /**
     * Helper method to validate genres against TMDb list
     */
    private void validateGenres(List<String> genreNames) {
        if (genreNames == null || genreNames.isEmpty()) {
            return; // Empty list is valid (opt-out)
        }
        
        // Get valid genres from TMDb
        List<Map<String, Object>> validGenres = getAllGenres();
        List<String> validGenreNames = validGenres.stream()
            .map(genre -> (String) genre.get("name"))
            .toList();
        
        // Check if all provided genres are valid
        for (String genre : genreNames) {
            if (!validGenreNames.contains(genre)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid genre: " + genre);
            }
        }
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
     * Helper method to authorize a user action
     * Ensures that only the owner of the account can modify their favorites
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
                "You are not authorized to modify this user's favorites");
        }
    }
}