package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.RankingSubmissionLog;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.exceptions.InvalidRankingException;
import ch.uzh.ifi.hase.soprafs25.exceptions.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.RankingSubmissionLogRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserMovieRankingRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.repository.RankingResultRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class RankingService {

    private final Logger log = LoggerFactory.getLogger(RankingService.class);

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final UserMovieRankingRepository userMovieRankingRepository;
    private final RankingSubmissionLogRepository rankingSubmissionLogRepository;
    private final RankingResultRepository rankingResultRepository;

    @Autowired
    public RankingService(@Qualifier("userRepository") UserRepository userRepository,
                          @Qualifier("movieRepository") MovieRepository movieRepository,
                          @Qualifier("userMovieRankingRepository") UserMovieRankingRepository userMovieRankingRepository,
                          @Qualifier("rankingSubmissionLogRepository") RankingSubmissionLogRepository rankingSubmissionLogRepository,
                          @Qualifier("rankingResultRepository") RankingResultRepository rankingResultRepository) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.userMovieRankingRepository = userMovieRankingRepository;
        this.rankingSubmissionLogRepository = rankingSubmissionLogRepository;
        this.rankingResultRepository = rankingResultRepository;
    }

    /**
     * Submits movie rankings for a given user.
     * Performs validation according to the rules:
     * - Rank top 5 if >= 5 movies available.
     * - Rank all if < 5 movies available.
     * - Movies must exist in the available pool.
     * - Ranks must be unique and sequential from 1.
     * Allows users to re-submit rankings (deletes old ones).
     *
     * @param userId   The ID of the user submitting rankings.
     * @param rankings A list of DTOs containing movieId and rank.
     * @throws UserNotFoundException     If the user does not exist.
     * @throws InvalidRankingException If the rankings are invalid.
     */
    public void submitRankings(Long userId, List<RankingSubmitDTO> rankings) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

        // TODO: Refine how "available movies for ranking" are determined.
        // For now, assume all movies in the DB are rankable.
        // This might need adjustment based on specific ranking periods or movie flags.
        List<Movie> availableMovies = movieRepository.findAll();
        if (availableMovies.isEmpty()) {
            throw new InvalidRankingException("No movies available for ranking.");
        }

        int availableMovieCount = availableMovies.size();
        int requiredRankings = Math.min(5, availableMovieCount);

        // --- Validation ---
        validateRankings(rankings, availableMovies, requiredRankings);

        // --- Process Valid Rankings ---
        // Delete existing rankings for this user (allows re-ranking)
        userMovieRankingRepository.deleteByUser(user);
        log.debug("Deleted existing rankings for user {}", userId);

        Map<Long, Movie> availableMoviesMap = availableMovies.stream()
                .collect(Collectors.toMap(Movie::getMovieId, movie -> movie));

        List<UserMovieRanking> newUserRankings = new ArrayList<>();
        for (RankingSubmitDTO dto : rankings) {
            UserMovieRanking newRanking = new UserMovieRanking();
            newRanking.setUser(user);
            Movie rankedMovie = availableMoviesMap.get(dto.getMovieId()); // Already validated that it exists
            newRanking.setMovie(rankedMovie);
            newRanking.setRank(dto.getRank());
            newUserRankings.add(newRanking);
        }

        userMovieRankingRepository.saveAll(newUserRankings);
        log.info("Successfully saved {} rankings for user {}", newUserRankings.size(), userId);

        // Log the successful submission
        RankingSubmissionLog submissionLog = new RankingSubmissionLog();
        submissionLog.setUser(user);
        submissionLog.setSubmissionTime(LocalDateTime.now());
        rankingSubmissionLogRepository.save(submissionLog);
        log.debug("Logged ranking submission for user {}", userId);
    }

    private void validateRankings(List<RankingSubmitDTO> rankings, List<Movie> availableMovies, int requiredRankings) {
        if (rankings == null || rankings.size() != requiredRankings) {
            throw new InvalidRankingException("Invalid number of rankings submitted. Expected " + requiredRankings + ", but received " + (rankings == null ? 0 : rankings.size()) + ".");
        }

        Set<Long> submittedMovieIds = new HashSet<>();
        Set<Integer> submittedRanks = new HashSet<>();
        Set<Long> availableMovieIds = availableMovies.stream().map(Movie::getMovieId).collect(Collectors.toSet());

        for (RankingSubmitDTO dto : rankings) {
            // Check for nulls (DTO validation should catch this, but double-check)
            if (dto.getMovieId() == null || dto.getRank() == null) {
                throw new InvalidRankingException("Ranking submission contains null movieId or rank.");
            }

            // Check if movie exists in the available pool
            if (!availableMovieIds.contains(dto.getMovieId())) {
                throw new InvalidRankingException("Invalid ranking: Movie with ID " + dto.getMovieId() + " is not available for ranking.");
            }

            // Check for duplicate movie IDs in submission
            if (!submittedMovieIds.add(dto.getMovieId())) {
                throw new InvalidRankingException("Invalid ranking: Duplicate movie ID " + dto.getMovieId() + " submitted.");
            }

            // Check rank range (1 to requiredRankings)
            if (dto.getRank() < 1 || dto.getRank() > requiredRankings) {
                throw new InvalidRankingException("Invalid rank: Rank " + dto.getRank() + " is outside the allowed range [1, " + requiredRankings + "].");
            }

            // Check for duplicate ranks
            if (!submittedRanks.add(dto.getRank())) {
                throw new InvalidRankingException("Invalid ranking: Duplicate rank " + dto.getRank() + " submitted.");
            }
        }

        log.debug("Rankings validated successfully.");
    }

    // --- Task 3 Methods ---

    /**
     * Calculates the movie with the lowest average rank from all submissions
     * and saves the result. If no rankings exist, it logs a warning.
     * Handles ties by picking the first movie encountered with the minimum average rank.
     */
    public void calculateAndSaveWinner() {
        List<UserMovieRanking> allRankings = userMovieRankingRepository.findAll();

        if (allRankings.isEmpty()) {
            log.warn("Cannot calculate winner: No rankings have been submitted yet.");
            return;
        }

        // Group rankings by movie and calculate average rank for each
        Map<Movie, Double> averageRankings = allRankings.stream()
            .collect(Collectors.groupingBy(
                UserMovieRanking::getMovie,
                Collectors.averagingInt(UserMovieRanking::getRank)
            ));

        // Find the movie with the minimum average rank
        Optional<Map.Entry<Movie, Double>> winnerEntry = averageRankings.entrySet().stream()
            .min(Comparator.comparingDouble(Map.Entry::getValue));

        if (winnerEntry.isPresent()) {
            Movie winningMovie = winnerEntry.get().getKey();
            Double winningAverageRank = winnerEntry.get().getValue();

            RankingResult result = new RankingResult();
            result.setWinningMovie(winningMovie);
            result.setAverageRank(winningAverageRank);
            result.setCalculationTimestamp(LocalDateTime.now());

            // Consider clearing previous results or handling multiple results per period if needed
            // For now, just saves the latest calculation
            rankingResultRepository.save(result);
            log.info("Calculated and saved ranking winner: Movie ID {}, Average Rank: {}", winningMovie.getMovieId(), winningAverageRank);

            // TODO: Implement notification system (Task 3) - call notification service here
            // Example: notificationService.notifyWinner(result);

        } else {
            // Should not happen if allRankings is not empty, but good to handle
            log.error("Could not determine a winner even though rankings exist.");
        }
    }

    /**
     * Retrieves the most recently calculated ranking result.
     *
     * @return An Optional containing the latest RankingResult, or empty if none exists.
     */
    public Optional<RankingResult> getLatestRankingResult() {
        return rankingResultRepository.findTopByOrderByCalculationTimestampDesc();
    }

    // TODO: Add method for retrieving movies available for ranking (maybe getMoviesForRanking())
}
