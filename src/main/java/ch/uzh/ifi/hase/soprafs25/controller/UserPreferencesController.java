package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPreferencesGenresDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPreferencesFavoriteMovieDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPreferencesDTO;
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
     * POST /users/{userId}/preferences/genres - Save genre preferences for a user
     */
    @PostMapping("/users/{userId}/preferences/genres")
    @ResponseStatus(HttpStatus.OK)
    public UserPreferencesGenresDTO saveGenrePreferences(
            @PathVariable("userId") Long userId,
            @RequestBody UserPreferencesGenresDTO genresDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract token from Authorization header
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        List<String> updatedPreferences = userPreferencesService.saveGenrePreferences(
                userId, genresDTO.getGenreIds(), token);
        
        UserPreferencesGenresDTO responseDTO = new UserPreferencesGenresDTO();
        responseDTO.setGenreIds(updatedPreferences);
        return responseDTO;
    }
    
    /**
     * GET /users/{userId}/preferences/genres - Get genre preferences for a user
     */
    @GetMapping("/users/{userId}/preferences/genres")
    @ResponseStatus(HttpStatus.OK)
    public UserPreferencesGenresDTO getGenrePreferences(@PathVariable("userId") Long userId) {
        List<String> genres = userPreferencesService.getGenrePreferences(userId);
        UserPreferencesGenresDTO responseDTO = new UserPreferencesGenresDTO();
        responseDTO.setGenreIds(genres);
        return responseDTO;
    }
    
    /**
     * POST /users/{userId}/preferences/favorite-movie - Save favorite movie for a user
     */
    @PostMapping("/users/{userId}/preferences/favorite-movie")
    @ResponseStatus(HttpStatus.OK)
    public UserPreferencesFavoriteMovieDTO saveFavoriteMovie(
            @PathVariable("userId") Long userId,
            @RequestBody UserPreferencesFavoriteMovieDTO favoriteMovieDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Extract token from Authorization header
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        Movie movie = userPreferencesService.saveFavoriteMovie(userId, favoriteMovieDTO.getMovieId(), token);
        UserPreferencesFavoriteMovieDTO responseDTO = new UserPreferencesFavoriteMovieDTO();
        responseDTO.setMovieId(movie != null ? movie.getMovieId() : null);
        return responseDTO;
    }
    
    /**
     * GET /users/{userId}/preferences/favorite-movie - Get favorite movie for a user
     */
    @GetMapping("/users/{userId}/preferences/favorite-movie")
    @ResponseStatus(HttpStatus.OK)
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

    /**
     * GET /users/{userId}/preferences - Get all preferences for a user (favorite genres and favorite movie)
     */
    @GetMapping("/users/{userId}/preferences")
    @ResponseStatus(HttpStatus.OK)
    public UserPreferencesDTO getAllPreferences(@PathVariable("userId") Long userId) {
        UserPreferencesDTO dto = new UserPreferencesDTO();
        dto.setFavoriteGenres(userPreferencesService.getGenrePreferences(userId));
        dto.setFavoriteMovie(userPreferencesService.getFavoriteMovie(userId));
        return dto;
    }
}