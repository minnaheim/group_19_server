package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.RankingSubmissionLog;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.exceptions.InvalidRankingException;
import ch.uzh.ifi.hase.soprafs25.exceptions.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs25.exceptions.GroupNotFoundException;
import ch.uzh.ifi.hase.soprafs25.repository.RankingSubmissionLogRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserMovieRankingRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.repository.RankingResultRepository;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieAverageRankDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
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
    private final UserMovieRankingRepository userMovieRankingRepository;
    private final RankingSubmissionLogRepository rankingSubmissionLogRepository;
    private final RankingResultRepository rankingResultRepository;
    private final GroupRepository groupRepository;

    @Autowired
    public RankingService(@Qualifier("userRepository") UserRepository userRepository,
                          @Qualifier("userMovieRankingRepository") UserMovieRankingRepository userMovieRankingRepository,
                          @Qualifier("rankingSubmissionLogRepository") RankingSubmissionLogRepository rankingSubmissionLogRepository,
                          @Qualifier("rankingResultRepository") RankingResultRepository rankingResultRepository,
                          @Qualifier("groupRepository") GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.userMovieRankingRepository = userMovieRankingRepository;
        this.rankingSubmissionLogRepository = rankingSubmissionLogRepository;
        this.rankingResultRepository = rankingResultRepository;
        this.groupRepository = groupRepository;
    }

    /**
     * Submits movie rankings for a given user within a specific group.
     * Performs validation according to the rules:
     * - Rank top 5 if >= 5 movies available.
     * - Rank all if < 5 movies available.
     * - Movies must exist in the available pool.
     * - Ranks must be unique and sequential from 1.
     * Allows users to re-submit rankings (deletes old ones for the specific group).
     *
     * @param userId   The ID of the user submitting rankings.
     * @param groupId  The ID of the group for which rankings are submitted.
     * @param rankings A list of DTOs containing movieId and rank.
     * @throws UserNotFoundException     If the user does not exist.
     * @throws GroupNotFoundException    If the group does not exist.
     * @throws InvalidRankingException If the rankings are invalid or the movie pool is empty/null.
     */
    public void submitRankings(Long userId, Long groupId, List<RankingSubmitDTO> rankings) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID " + groupId + " not found."));

        // Use the group's specific movie pool
        MoviePool moviePool = group.getMoviePool();
        if (moviePool == null || moviePool.getMovies().isEmpty()) {
            throw new InvalidRankingException("No movies available for ranking in group " + groupId + ".");
        }
        List<Movie> availableMovies = moviePool.getMovies();

        int availableMovieCount = availableMovies.size();
        int requiredRankings = Math.min(5, availableMovieCount);

        // --- Validate Rankings ---
        validateRankings(rankings, availableMovies, requiredRankings);

        // --- Process Valid Rankings ---
        // Delete existing rankings for this user *in this group*
        userMovieRankingRepository.deleteByUserAndGroup(user, group);

        List<UserMovieRanking> newRankings = new ArrayList<>();
        for (RankingSubmitDTO dto : rankings) {
            Movie movie = availableMovies.stream()
                    .filter(m -> m.getMovieId() == dto.getMovieId()) // Use == for long/Long comparison
                    .findFirst()
                    .orElseThrow(() -> new InvalidRankingException("Error retrieving movie details for ID: " + dto.getMovieId())); // Should not happen due to validation

            UserMovieRanking ranking = new UserMovieRanking();
            ranking.setUser(user);
            ranking.setMovie(movie);
            ranking.setRank(dto.getRank());
            ranking.setGroup(group); // Set the group
            newRankings.add(ranking);
        }

        userMovieRankingRepository.saveAll(newRankings);
        log.info("User {} submitted {} rankings for group {}", userId, newRankings.size(), groupId);

        // Log the submission
        RankingSubmissionLog submissionLog = new RankingSubmissionLog();
        submissionLog.setUser(user);
        // submissionLog.setGroup(group); // Consider adding group to log if needed
        submissionLog.setSubmissionTime(LocalDateTime.now());
        submissionLog.setNumberOfMoviesRanked(newRankings.size());
        rankingSubmissionLogRepository.save(submissionLog);
    }

    /**
     * Calculates the movie with the lowest average rank from all submissions within a specific group
     * and saves the result for that group. If no rankings exist for the group,
     * it logs a warning.
     * Handles ties by picking the first movie encountered with the minimum average rank.
     *
     * @param groupId The ID of the group for which to calculate the winner.
     * @throws GroupNotFoundException If the group does not exist.
     */
    public void calculateAndSaveWinner(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID " + groupId + " not found."));

        List<UserMovieRanking> groupRankings = userMovieRankingRepository.findByGroup(group);

        if (groupRankings.isEmpty()) {
            log.warn("Cannot calculate winner for group {}: No rankings have been submitted yet.", groupId);
            return;
        }

        // Group rankings by movie and calculate average rank for each within the group
        Map<Movie, Double> averageRankings = groupRankings.stream()
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
            result.setGroup(group); // Set the group for the result

            // Consider clearing previous results for this group or handling multiple results per period if needed
            // For now, just saves the latest calculation for this group
            rankingResultRepository.save(result);
            log.info("Calculated and saved ranking winner for group {}: Movie ID {}, Average Rank: {}", groupId, winningMovie.getMovieId(), winningAverageRank);

            // TODO: Implement notification system (Task 3) - call notification service here, potentially group-specific
            // Example: notificationService.notifyGroupWinner(group, result);

        } else {
            // Should not happen if groupRankings is not empty, but good to handle
            log.error("Could not determine a winner for group {} even though rankings exist.", groupId);
        }
    }

    /**
     * Retrieves the most recently calculated ranking result for a specific group.
     *
     * @param groupId The ID of the group.
     * @return An Optional containing the latest RankingResult for the group, or empty if none exists.
     * @throws GroupNotFoundException If the group does not exist.
     */
    public Optional<RankingResult> getLatestRankingResult(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID " + groupId + " not found."));
        return rankingResultRepository.findTopByGroupOrderByCalculationTimestampDesc(group);
    }

    /**
     * Retrieves the list of movies available for ranking within a specific group.
     *
     * @param groupId The ID of the group.
     * @return A list of Movie entities in the group's pool.
     * @throws GroupNotFoundException If the group does not exist or has no movie pool.
     */
    public List<Movie> getRankableMoviesForGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID " + groupId + " not found."));

        MoviePool moviePool = group.getMoviePool();
        if (moviePool == null) {
            throw new GroupNotFoundException("Movie pool not found for group " + groupId);
        }
        return moviePool.getMovies();
    }

    /**
     * Retrieves the detailed ranking results for all movies in a specific group's movie pool.
     * Calculates the average rank for each movie based on all submitted user rankings for that group.
     *
     * @param groupId The ID of the group.
     * @return A list of MovieAverageRankDTO objects, sorted by average rank (ascending).
     *         Returns an empty list if the group has no movies or no rankings have been submitted.
     * @throws GroupNotFoundException If the group does not exist.
     */
    public List<MovieAverageRankDTO> getCompleteRankingResult(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID " + groupId + " not found."));

        // Get all movies available for ranking in this group
        List<Movie> moviesInPool = getRankableMoviesForGroup(groupId); // Reuses existing method
        if (moviesInPool.isEmpty()) {
            log.warn("No movies found in the pool for group {}. Returning empty ranking details.", groupId);
            return Collections.emptyList();
        }

        // Fetch all rankings submitted for this group
        List<UserMovieRanking> allGroupRankings = userMovieRankingRepository.findByGroup(group);
        if (allGroupRankings.isEmpty()) {
            log.warn("No rankings submitted yet for group {}. Returning empty ranking details.", groupId);
            // Return DTOs with null ranks for all movies in the pool
            return moviesInPool.stream()
                    .map(movie -> {
                        MovieAverageRankDTO dto = new MovieAverageRankDTO();
                        dto.setMovie(DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie));
                        dto.setAverageRank(null); // Indicate no rankings submitted yet
                        return dto;
                    })
                    .sorted(Comparator.comparing(dto -> dto.getMovie().getTitle())) // Sort alphabetically if no ranks
                    .collect(Collectors.toList());
        }

        // Group rankings by movie
        Map<Movie, List<UserMovieRanking>> rankingsByMovie = allGroupRankings.stream()
                .collect(Collectors.groupingBy(UserMovieRanking::getMovie));

        // Calculate average rank for each movie in the pool
        List<MovieAverageRankDTO> averageRankDTOs = moviesInPool.stream()
                .map(movie -> {
                    MovieAverageRankDTO dto = new MovieAverageRankDTO();
                    dto.setMovie(DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie));

                    // Calculate average rank only if rankings exist for this movie
                    List<UserMovieRanking> movieRankings = rankingsByMovie.get(movie);
                    Double averageRank = null;
                    if (movieRankings != null && !movieRankings.isEmpty()) {
                        averageRank = movieRankings.stream()
                                .mapToInt(UserMovieRanking::getRank)
                                .average()
                                .orElse(Double.NaN); // Should not happen if list is not empty
                    }
                    dto.setAverageRank(averageRank);
                    return dto;
                })
                // Sort by average rank (ascending), handle nulls (movies with no rankings) last
                .sorted(Comparator.comparing(MovieAverageRankDTO::getAverageRank, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        log.info("Retrieved complete ranking details for group {}", groupId);
        return averageRankDTOs;
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
}
