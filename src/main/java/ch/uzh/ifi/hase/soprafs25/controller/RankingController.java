package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingResultsDTO;
import ch.uzh.ifi.hase.soprafs25.service.RankingService;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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

    @GetMapping("/groups/{groupId}/rankings/results")
    public ResponseEntity<RankingResultsDTO> getRankingResults(@PathVariable Long groupId) {
        RankingResultsDTO dto = rankingService.getRankingResults(groupId);
        return ResponseEntity.ok(dto);
    }

    // TODO: Consider adding an endpoint to trigger calculation: POST /groups/{groupId}/rankings/calculate ?
}
