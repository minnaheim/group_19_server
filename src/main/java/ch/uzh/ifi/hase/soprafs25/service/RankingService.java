package ch.uzh.ifi.hase.soprafs25.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.entity.RankingSubmissionLog;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.exceptions.GroupNotFoundException;
import ch.uzh.ifi.hase.soprafs25.exceptions.InvalidRankingException;
import ch.uzh.ifi.hase.soprafs25.exceptions.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.RankingResultRepository;
import ch.uzh.ifi.hase.soprafs25.repository.RankingSubmissionLogRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserMovieRankingRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieAverageRankDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingResultsDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;

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
    Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GroupNotFoundException("Group with ID " + groupId + " not found."));
    if (group.getPhase() != Group.GroupPhase.VOTING) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Rankings can only be submitted during the VOTING phase");
    }
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

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

    private Map<Movie, Double> calculateAdjustedAverageRanks(Group group, List<UserMovieRanking> groupRankings) {
        int totalUsers = group.getMembers().size();
        int maxRank = 5;
        int penaltyRank = maxRank + 1;
    
        // group rankings by movie
        Map<Movie, List<UserMovieRanking>> rankingsByMovie = groupRankings.stream()
                .collect(Collectors.groupingBy(UserMovieRanking::getMovie));
    
        // calculate adjusted average rank 
        Map<Movie, Double> adjustedAverageRanks = new HashMap<>();
        for (Movie movie : group.getMoviePool().getMovies()) {
            List<UserMovieRanking> ranks = rankingsByMovie.getOrDefault(movie, new ArrayList<>());

            int actualVotes = ranks.size();
            int missingVotes = totalUsers - actualVotes;

            // adjusted total score
            double totalScore = ranks.stream().mapToInt(UserMovieRanking::getRank).sum()
                                 + missingVotes * penaltyRank;
    
            // adjusted average rank
            double average = totalScore / totalUsers;
            adjustedAverageRanks.put(movie, average);
        }
    
        return adjustedAverageRanks;
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

        Map<Movie, Double> adjustedAverageRanks = calculateAdjustedAverageRanks(group, groupRankings);


        // Find the movie with the minimum average rank and highest TMDB ranking
        Optional<Movie> winnerMovie = adjustedAverageRanks.entrySet().stream()
        .min(Comparator
            .comparingDouble(Map.Entry<Movie, Double>::getValue)
            .thenComparing(
                entry -> entry.getKey().getTmdbRating() != null 
                    ? -entry.getKey().getTmdbRating()  // Higher rating wins
                    : Double.MAX_VALUE
            )
        )
        .map(Map.Entry::getKey);

        if (winnerMovie.isPresent()) {
            Movie winningMovie = winnerMovie.get();
            Double winningAverageRank = adjustedAverageRanks.get(winningMovie);

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
     * Retrieves a user's submitted rankings for a given group.
     * Returns empty list if none exist.
     */
    public List<RankingSubmitDTO> getUserRankings(Long userId, Long groupId) {
        // Ensure user and group exist
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GroupNotFoundException("Group not found"));
        // Fetch and map
        return userMovieRankingRepository.findByUserAndGroup(user, group).stream()
            .map(umr -> {
                RankingSubmitDTO dto = new RankingSubmitDTO();
                dto.setMovieId(umr.getMovie().getMovieId());
                dto.setRank(umr.getRank());
                return dto;
            })
            .collect(Collectors.toList());
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
    if (group.getPhase() != Group.GroupPhase.RESULTS) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Ranking details can only be viewed during the RESULTS phase");
    }

        // Get all movies available for ranking in this group
        List<Movie> moviesInPool = getRankableMoviesForGroup(groupId); // Reuses existing method
        if (moviesInPool.isEmpty()) {
            log.warn("No movies found in the pool for group {}. Returning empty ranking details.", groupId);
            return Collections.emptyList();
        }

        // Fetch all rankings submitted for this group
        List<UserMovieRanking> groupRankings = userMovieRankingRepository.findByGroup(group);
        if (groupRankings.isEmpty()) {
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

        Map<Movie, Double> adjustedAverageRanks = calculateAdjustedAverageRanks(group, groupRankings);

        // create DTOs and sort by adjusted average rank and TMDB rating
        // same logic as for winner, but now return list
        return moviesInPool.stream().map(movie -> {
            MovieAverageRankDTO dto = new MovieAverageRankDTO();
            dto.setMovie(DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie));
            dto.setAverageRank(adjustedAverageRanks.get(movie));
            return dto;
        })
        .sorted(Comparator
            .comparing(MovieAverageRankDTO::getAverageRank, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(dto -> dto.getMovie().getTmdbRating() != null 
                ? -dto.getMovie().getTmdbRating()
                : Double.MAX_VALUE
            )
        )
        .collect(Collectors.toList());
    }

    public RankingResultsDTO getRankingResults(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID " + groupId + " not found."));
        if (group.getPhase() != Group.GroupPhase.RESULTS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Results can only be viewed after the RESULTS phase.");
        }
        // Ensure a saved winner exists
        Optional<RankingResult> saved = rankingResultRepository.findTopByGroupOrderByCalculationTimestampDesc(group);
        if (saved.isEmpty()) {
            calculateAndSaveWinner(groupId);
            saved = rankingResultRepository.findTopByGroupOrderByCalculationTimestampDesc(group);
        }
        RankingResult result = saved.get();
        RankingResultsDTO dto = new RankingResultsDTO();
        dto.setResultId(result.getId());
        dto.setGroupId(groupId);
        dto.setCalculatedAt(result.getCalculationTimestamp().toString());
        dto.setWinningMovie(DTOMapper.INSTANCE.convertEntityToMovieRankGetDTO(result.getWinningMovie()));
        long voters = userMovieRankingRepository.findByGroup(group).stream()
                .map(umr -> umr.getUser().getUserId()).distinct().count();
        dto.setNumberOfVoters((int) voters);
        dto.setDetailedResults(getCompleteRankingResult(groupId));
        return dto;
    }

    /**
     * Retrieves the most recently calculated ranking result for a specific group.
     * @param groupId the group ID
     * @return optional RankingResult
     */
    public Optional<RankingResult> getLatestRankingResult(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID " + groupId + " not found."));
        if (group.getPhase() != Group.GroupPhase.RESULTS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ranking results can only be viewed during the RESULTS phase");
        }
        return rankingResultRepository.findTopByGroupOrderByCalculationTimestampDesc(group);
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
