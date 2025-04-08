package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.UserMovieService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * UserMovieController
 * This class is responsible for handling all REST requests related to a user's movie lists
 * (watchlist and watched movies)
 */
@RestController
public class UserMovieController {
    
    private final UserMovieService userMovieService;
    
    UserMovieController(UserMovieService userMovieService) {
        this.userMovieService = userMovieService;
    }
    
    /**
     * GET /users/{userId}/watchlist - Get all movies in a user's watchlist
     */
    @GetMapping("/users/{userId}/watchlist")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<MovieGetDTO> getWatchlist(@PathVariable("userId") Long userId) {
        List<Movie> watchlist = userMovieService.getWatchlist(userId);
        return convertMovieListToDTOList(watchlist);
    }
    
    /**
     * POST /users/{userId}/watchlist/{movieId} - Add a movie to a user's watchlist
     */
    @PostMapping("/users/{userId}/watchlist/{movieId}")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public List<MovieGetDTO> addToWatchlist(
            @PathVariable("userId") Long userId,
            @PathVariable("movieId") Long movieId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract token from Authorization header if present
        String effectiveToken = token;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            effectiveToken = authHeader.substring(7);
        }
        
        List<Movie> updatedWatchlist = userMovieService.addToWatchlist(userId, movieId, effectiveToken);
        return convertMovieListToDTOList(updatedWatchlist);
    }
    
    /**
     * DELETE /users/{userId}/watchlist/{movieId} - Remove a movie from a user's watchlist
     */
    @DeleteMapping("/users/{userId}/watchlist/{movieId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<MovieGetDTO> removeFromWatchlist(
            @PathVariable("userId") Long userId,
            @PathVariable("movieId") Long movieId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract token from Authorization header if present
        String effectiveToken = token;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            effectiveToken = authHeader.substring(7);
        }
        
        List<Movie> updatedWatchlist = userMovieService.removeFromWatchlist(userId, movieId, effectiveToken);
        return convertMovieListToDTOList(updatedWatchlist);
    }
    
    /**
     * GET /users/{userId}/watched - Get all movies in a user's watched list
     */
    @GetMapping("/users/{userId}/watched")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<MovieGetDTO> getWatchedMovies(@PathVariable("userId") Long userId) {
        List<Movie> watchedMovies = userMovieService.getWatchedMovies(userId);
        return convertMovieListToDTOList(watchedMovies);
    }
    
    /**
     * POST /users/{userId}/watched/{movieId} - Add a movie to a user's watched list
     */
    @PostMapping("/users/{userId}/watched/{movieId}")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public List<MovieGetDTO> addToWatchedMovies(
            @PathVariable("userId") Long userId,
            @PathVariable("movieId") Long movieId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract token from Authorization header if present
        String effectiveToken = token;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            effectiveToken = authHeader.substring(7);
        }
        
        List<Movie> updatedWatchedMovies = userMovieService.addToWatchedMovies(userId, movieId, effectiveToken);
        return convertMovieListToDTOList(updatedWatchedMovies);
    }
    
    /**
     * DELETE /users/{userId}/watched/{movieId} - Remove a movie from a user's watched list
     */
    @DeleteMapping("/users/{userId}/watched/{movieId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<MovieGetDTO> removeFromWatchedMovies(
            @PathVariable("userId") Long userId,
            @PathVariable("movieId") Long movieId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract token from Authorization header if present
        String effectiveToken = token;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            effectiveToken = authHeader.substring(7);
        }
        
        List<Movie> updatedWatchedMovies = userMovieService.removeFromWatchedMovies(userId, movieId, effectiveToken);
        return convertMovieListToDTOList(updatedWatchedMovies);
    }
    
    /**
     * Helper method to convert a list of Movie entities to a list of MovieGetDTOs
     */
    private List<MovieGetDTO> convertMovieListToDTOList(List<Movie> movies) {
        List<MovieGetDTO> movieGetDTOs = new ArrayList<>();
        
        for (Movie movie : movies) {
            movieGetDTOs.add(DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie));
        }
        
        return movieGetDTOs;
    }
}