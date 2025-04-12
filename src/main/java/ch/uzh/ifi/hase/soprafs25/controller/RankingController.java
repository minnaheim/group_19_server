package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import ch.uzh.ifi.hase.soprafs25.service.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingResultGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;

@RestController
@RequestMapping("/api")
public class RankingController {

    private final RankingService rankingService;

    @Autowired
    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * POST /api/users/{userId}/rankings
     * Endpoint for a user to submit their movie rankings.
     * The request body should be a JSON array of RankingSubmitDTO objects.
     *
     * @param userId   The ID of the user submitting the rankings.
     * @param rankingSubmitDTOs List of rankings. Input is validated.
     * @return ResponseEntity with status 204 (No Content) on success.
     *         Handles UserNotFoundException (404) and InvalidRankingException (400) via @ResponseStatus.
     */
    @PostMapping("/users/{userId}/rankings")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Return 204 No Content on successful submission
    public ResponseEntity<Void> submitUserRankings(
            @PathVariable Long userId,
            @Valid @RequestBody List<RankingSubmitDTO> rankingSubmitDTOs) {
        
        rankingService.submitRankings(userId, rankingSubmitDTOs);
        return ResponseEntity.noContent().build(); // Explicitly return 204
    }

    /**
     * GET /api/rankings/results/latest
     * Retrieves the most recently calculated ranking result.
     *
     * @return ResponseEntity containing the RankingResultGetDTO (200 OK) or 404 Not Found if no result exists.
     */
    @GetMapping("/rankings/results/latest")
    public ResponseEntity<RankingResultGetDTO> getLatestResult() {
        Optional<RankingResult> latestResultOpt = rankingService.getLatestRankingResult();

        if (latestResultOpt.isPresent()) {
            RankingResult latestResult = latestResultOpt.get();
            RankingResultGetDTO resultDTO = DTOMapper.INSTANCE.convertEntityToRankingResultGetDTO(latestResult);
            return ResponseEntity.ok(resultDTO);
        } else {
            return ResponseEntity.notFound().build(); // Return 404 if no result found
        }
    }

    // TODO: Consider adding an endpoint to trigger calculation: POST /api/rankings/results/calculate ?
    // TODO: Add endpoint to GET movies available for ranking? (e.g., GET /api/rankings/movies)
}
