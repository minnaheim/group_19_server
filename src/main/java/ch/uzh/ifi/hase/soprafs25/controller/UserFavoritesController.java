package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesGenresDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesMovieDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesActorsDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesDirectorsDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserFavoritesDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.UserFavoritesService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UserFavoritesController
 * This class is responsible for handling all REST requests related to user favorites
 * such as genre favorites and favorite movie.
 */
@RestController
public class UserFavoritesController {
    
    private final UserFavoritesService userFavoritesService;
    
    UserFavoritesController(UserFavoritesService userFavoritesService) {
        this.userFavoritesService = userFavoritesService;
    }
    
    /**
     * POST /users/{userId}/favorites/genres - Save genre favorites for a user
     */
    @PostMapping("/users/{userId}/favorites/genres")
    @ResponseStatus(HttpStatus.OK)
    public UserFavoritesGenresDTO saveGenreFavorites(
            @PathVariable("userId") Long userId,
            @RequestBody UserFavoritesGenresDTO genresDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract token from Authorization header
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        List<String> updatedFavorites = userFavoritesService.saveGenreFavorites(
                userId, genresDTO.getGenreIds(), token);
        
        UserFavoritesGenresDTO responseDTO = new UserFavoritesGenresDTO();
        responseDTO.setGenreIds(updatedFavorites);
        return responseDTO;
    }
    
    /**
     * GET /users/{userId}/favorites/genres - Get genre favorites for a user
     */
    @GetMapping("/users/{userId}/favorites/genres")
    @ResponseStatus(HttpStatus.OK)
    public UserFavoritesGenresDTO getGenreFavorites(@PathVariable("userId") Long userId) {
        List<String> genres = userFavoritesService.getGenreFavorites(userId);
        UserFavoritesGenresDTO responseDTO = new UserFavoritesGenresDTO();
        responseDTO.setGenreIds(genres);
        return responseDTO;
    }
    
    /**
     * POST /users/{userId}/favorites/movie - Save favorite movie for a user
     */
    @PostMapping("/users/{userId}/favorites/movie")
    @ResponseStatus(HttpStatus.OK)
    public UserFavoritesMovieDTO saveFavoriteMovie(
            @PathVariable("userId") Long userId,
            @RequestBody UserFavoritesMovieDTO favoriteMovieDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Extract token from Authorization header
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        Movie movie = userFavoritesService.saveFavoriteMovie(userId, favoriteMovieDTO.getMovieId(), token);
        UserFavoritesMovieDTO responseDTO = new UserFavoritesMovieDTO();
        responseDTO.setMovieId(movie != null ? movie.getMovieId() : null);
        return responseDTO;
    }
    
    /**
     * GET /users/{userId}/favorites/movie - Get favorite movie for a user
     */
    @GetMapping("/users/{userId}/favorites/movie")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getFavoriteMovie(@PathVariable("userId") Long userId) {
        Movie movie = userFavoritesService.getFavoriteMovie(userId);
        
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
     * POST /users/{userId}/favorites/actors - Save favorite actors for a user
     */
    @PostMapping("/users/{userId}/favorites/actors")
    @ResponseStatus(HttpStatus.OK)
    public UserFavoritesActorsDTO saveFavoriteActors(
            @PathVariable("userId") Long userId,
            @RequestBody UserFavoritesActorsDTO actorsDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        List<String> updatedActors = userFavoritesService.saveFavoriteActors(
                userId, actorsDTO.getFavoriteActors(), token);
        UserFavoritesActorsDTO responseDTO = new UserFavoritesActorsDTO();
        responseDTO.setFavoriteActors(updatedActors);
        return responseDTO;
    }

    /**
     * GET /users/{userId}/favorites/actors - Get favorite actors for a user
     */
    @GetMapping("/users/{userId}/favorites/actors")
    @ResponseStatus(HttpStatus.OK)
    public UserFavoritesActorsDTO getFavoriteActors(
            @PathVariable("userId") Long userId) {
        List<String> actors = userFavoritesService.getFavoriteActors(userId);
        UserFavoritesActorsDTO responseDTO = new UserFavoritesActorsDTO();
        responseDTO.setFavoriteActors(actors);
        return responseDTO;
    }

    /**
     * POST /users/{userId}/favorites/directors - Save favorite directors for a user
     */
    @PostMapping("/users/{userId}/favorites/directors")
    @ResponseStatus(HttpStatus.OK)
    public UserFavoritesDirectorsDTO saveFavoriteDirectors(
            @PathVariable("userId") Long userId,
            @RequestBody UserFavoritesDirectorsDTO directorsDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        List<String> updatedDirectors = userFavoritesService.saveFavoriteDirectors(
                userId, directorsDTO.getFavoriteDirectors(), token);
        UserFavoritesDirectorsDTO responseDTO = new UserFavoritesDirectorsDTO();
        responseDTO.setFavoriteDirectors(updatedDirectors);
        return responseDTO;
    }

    /**
     * GET /users/{userId}/favorites/directors - Get favorite directors for a user
     */
    @GetMapping("/users/{userId}/favorites/directors")
    @ResponseStatus(HttpStatus.OK)
    public UserFavoritesDirectorsDTO getFavoriteDirectors(
            @PathVariable("userId") Long userId) {
        List<String> directors = userFavoritesService.getFavoriteDirectors(userId);
        UserFavoritesDirectorsDTO responseDTO = new UserFavoritesDirectorsDTO();
        responseDTO.setFavoriteDirectors(directors);
        return responseDTO;
    }

    /**
     * GET /users/{userId}/favorites - Get all favorites for a user (favorite genres and favorite movie)
     */
    @GetMapping("/users/{userId}/favorites")
    @ResponseStatus(HttpStatus.OK)
    public UserFavoritesDTO getAllFavorites(@PathVariable("userId") Long userId) {
        UserFavoritesDTO dto = new UserFavoritesDTO();
        dto.setFavoriteGenres(userFavoritesService.getGenreFavorites(userId));
        dto.setFavoriteMovie(userFavoritesService.getFavoriteMovie(userId));
        dto.setFavoriteActors(userFavoritesService.getFavoriteActors(userId));
        dto.setFavoriteDirectors(userFavoritesService.getFavoriteDirectors(userId));
        return dto;
    }
}