package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingResultGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieAverageRankDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingResultsDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.RankingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;

@RestController

public class RankingController {

    private final RankingService rankingService;

    @Autowired
    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * POST /groups/{groupId}/users/{userId}/rankings
     * Endpoint for a user to submit their movie rankings for a specific group.
     *
     * @param groupId The ID of the group.
     * @param userId  The ID of the user submitting the rankings.
     * @param rankingSubmitDTOs List of rankings. Input is validated.
     * @return ResponseEntity with status 204 (No Content) on success.
     *         Handles UserNotFoundException, GroupNotFoundException (404) and 
     *         InvalidRankingException (400) via GlobalExceptionHandler.
     */
    @PostMapping("/groups/{groupId}/users/{userId}/rankings")
    @ResponseStatus(HttpStatus.NO_CONTENT) 
    public ResponseEntity<Void> submitGroupUserRankings(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @Valid @RequestBody List<RankingSubmitDTO> rankingSubmitDTOs) {
        
        rankingService.submitRankings(userId, groupId, rankingSubmitDTOs); // Use new service method
        return ResponseEntity.noContent().build(); 
    }

    /**
     * GET /groups/{groupId}/movies/rankable
     * Retrieves the list of movies available for ranking within a specific group.
     *
     * @param groupId The ID of the group.
     * @return ResponseEntity containing a list of MovieGetDTOs (200 OK) or 404 if the group or pool is not found.
     *         Handles GroupNotFoundException (404) via GlobalExceptionHandler.
     */
    @GetMapping("/groups/{groupId}/movies/rankable")
    public ResponseEntity<List<MovieGetDTO>> getRankableMoviesForGroup(@PathVariable Long groupId) {
        List<Movie> rankableMovies = rankingService.getRankableMoviesForGroup(groupId); // Use new service method
        List<MovieGetDTO> movieDTOs = rankableMovies.stream()
                .map(DTOMapper.INSTANCE::convertEntityToMovieGetDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(movieDTOs);
    }

    @GetMapping("/groups/{groupId}/rankings/results")
    public ResponseEntity<RankingResultsDTO> getRankingResults(@PathVariable Long groupId) {
        RankingResultsDTO dto = rankingService.getRankingResults(groupId);
        return ResponseEntity.ok(dto);
    }

    // TODO: Consider adding an endpoint to trigger calculation: POST /groups/{groupId}/rankings/calculate ?
}
