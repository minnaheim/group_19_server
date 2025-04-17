package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingResultGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieAverageRankDTO;
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
     * GET /groups/{groupId}/rankings/result
     * Retrieves the most recently calculated ranking result for a specific group.
     *
     * @param groupId The ID of the group.
     * @return ResponseEntity containing the RankingResultGetDTO (200 OK) or 404 Not Found if no result exists for the group.
     *         Handles GroupNotFoundException (404) via GlobalExceptionHandler.
     */
    @GetMapping("/groups/{groupId}/rankings/result")
    public ResponseEntity<RankingResultGetDTO> getGroupLatestResult(@PathVariable Long groupId) {
        Optional<RankingResult> latestResultOpt = rankingService.getLatestRankingResult(groupId); // Use new service method

        return latestResultOpt
                .map(result -> ResponseEntity.ok(DTOMapper.INSTANCE.convertEntityToRankingResultGetDTO(result)))
                .orElseGet(() -> ResponseEntity.notFound().build());
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

    /**
     * GET /groups/{groupId}/rankings/details : Retrieves the detailed ranking results for a group.
     *
     * This endpoint returns a list of all movies considered in the group's ranking,
     * each with its calculated average rank based on all submissions.
     * The list is sorted by average rank (ascending, best first).
     * Movies with no rankings yet will have a null averageRank and appear last (sorted alphabetically).
     *
     * @param groupId The ID of the group.
     * @return ResponseEntity with a List of MovieAverageRankDTO (200 OK) or 404 Not Found if the group doesn't exist.
     */
    @GetMapping("/groups/{groupId}/rankings/details")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<List<MovieAverageRankDTO>> getGroupCompleteRankingResult(@PathVariable Long groupId) {
        List<MovieAverageRankDTO> completeResult = rankingService.getCompleteRankingResult(groupId);
        // The service method handles the case where the group is not found by throwing GroupNotFoundException,
        // which should be handled by GlobalExceptionHandler to return 404.
        // If the list is empty (no movies or no rankings), we still return 200 OK with an empty list.
        return ResponseEntity.ok(completeResult);
    }

    // TODO: Consider adding an endpoint to trigger calculation: POST /groups/{groupId}/rankings/calculate ?
}
