package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.UserPreferencesService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UserPreferencesController
 * This class is responsible for handling all REST requests related to user preferences
 * such as genre preferences and favorite movie.
 */
@RestController
public class UserPreferencesController {
    
    private final UserPreferencesService userPreferencesService;
    
    UserPreferencesController(UserPreferencesService userPreferencesService) {
        this.userPreferencesService = userPreferencesService;
    }
    
    /**
     * GET /api/genres - Get all available genres from TMDb
     */
    @GetMapping("/api/genres")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Map<String, Object>> getAllGenres() {
        return userPreferencesService.getAllGenres();
    }
    
    /**
     * POST /api/users/{userId}/preferences/genres - Save genre preferences for a user
     */
    @PostMapping("/api/users/{userId}/preferences/genres")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Map<String, Object> saveGenrePreferences(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, List<String>> genrePreferences,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract token from Authorization header
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        List<String> updatedPreferences = userPreferencesService.saveGenrePreferences(
                userId, genrePreferences.get("genreIds"), token);
        
        return Map.of(
            "success", true,
            "genres", updatedPreferences
        );
    }
    
    /**
     * GET /api/users/{userId}/preferences/genres - Get genre preferences for a user
     */
    @GetMapping("/api/users/{userId}/preferences/genres")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Map<String, Object> getGenrePreferences(@PathVariable("userId") Long userId) {
        List<String> genres = userPreferencesService.getGenrePreferences(userId);
        return Map.of("genres", genres);
    }
    
    /**
     * POST /api/users/{userId}/preferences/favorite-movie - Save favorite movie for a user
     */
    @PostMapping("/api/users/{userId}/preferences/favorite-movie")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Map<String, Object> saveFavoriteMovie(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, Long> favoriteMovie,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract token from Authorization header
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        Movie movie = userPreferencesService.saveFavoriteMovie(userId, favoriteMovie.get("movieId"), token);
        MovieGetDTO movieDTO = DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie);
        
        return Map.of(
            "success", true,
            "movie", movieDTO
        );
    }
    
    /**
     * GET /api/users/{userId}/preferences/favorite-movie - Get favorite movie for a user
     */
    @GetMapping("/api/users/{userId}/preferences/favorite-movie")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Map<String, Object> getFavoriteMovie(@PathVariable("userId") Long userId) {
        Movie movie = userPreferencesService.getFavoriteMovie(userId);
        
        if (movie != null) {
            MovieGetDTO movieDTO = DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie);
            return Map.of("movie", movieDTO);
        } else {
            // Use HashMap instead of Map.of to allow null values
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("movie", null);
            return response;
        }
    }
}