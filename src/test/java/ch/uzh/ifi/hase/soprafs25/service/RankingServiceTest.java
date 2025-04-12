package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.entity.RankingSubmissionLog;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.exceptions.InvalidRankingException;
import ch.uzh.ifi.hase.soprafs25.exceptions.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs25.repository.*;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException; // For potential exception tests

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class RankingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserMovieRankingRepository userMovieRankingRepository;

    @Mock
    private RankingSubmissionLogRepository rankingSubmissionLogRepository;

    @Mock
    private RankingResultRepository rankingResultRepository;

    @InjectMocks
    private RankingService rankingService;

    private User testUser;
    private Movie movie1, movie2, movie3, movie4, movie5, movie6;
    private List<Movie> availableMovies_5;
    private List<Movie> availableMovies_3;
    private List<RankingSubmitDTO> validRankings_5;
    private List<RankingSubmitDTO> validRankings_3;


    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Initialize common test data
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");

        movie1 = new Movie(); movie1.setMovieId(101L); movie1.setTitle("Movie A");
        movie2 = new Movie(); movie2.setMovieId(102L); movie2.setTitle("Movie B");
        movie3 = new Movie(); movie3.setMovieId(103L); movie3.setTitle("Movie C");
        movie4 = new Movie(); movie4.setMovieId(104L); movie4.setTitle("Movie D");
        movie5 = new Movie(); movie5.setMovieId(105L); movie5.setTitle("Movie E");
        movie6 = new Movie(); movie6.setMovieId(106L); movie6.setTitle("Movie F"); // Extra movie

        availableMovies_5 = Arrays.asList(movie1, movie2, movie3, movie4, movie5);
        availableMovies_3 = Arrays.asList(movie1, movie2, movie3);

        validRankings_5 = new ArrayList<>();
        validRankings_5.add(createSubmitDTO(movie1.getMovieId(), 1));
        validRankings_5.add(createSubmitDTO(movie2.getMovieId(), 2));
        validRankings_5.add(createSubmitDTO(movie3.getMovieId(), 3));
        validRankings_5.add(createSubmitDTO(movie4.getMovieId(), 4));
        validRankings_5.add(createSubmitDTO(movie5.getMovieId(), 5));

        validRankings_3 = new ArrayList<>();
        validRankings_3.add(createSubmitDTO(movie1.getMovieId(), 1));
        validRankings_3.add(createSubmitDTO(movie2.getMovieId(), 2));
        validRankings_3.add(createSubmitDTO(movie3.getMovieId(), 3));

        // Mock default behavior
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty()); // Default: user not found
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(movieRepository.findAll()).thenReturn(availableMovies_5); // Default: 5 movies available
    }

    // --- Tests for submitRankings ---

    @Test
    void submitRankings_validInput_5Movies_success() {
        // Arrange
        when(movieRepository.findAll()).thenReturn(availableMovies_5);

        // Act
        rankingService.submitRankings(testUser.getUserId(), validRankings_5);

        // Assert
        verify(userMovieRankingRepository, times(1)).deleteByUser(testUser);
        ArgumentCaptor<List<UserMovieRanking>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userMovieRankingRepository, times(1)).saveAll(listCaptor.capture());
        assertEquals(5, listCaptor.getValue().size());
        verify(rankingSubmissionLogRepository, times(1)).save(any(RankingSubmissionLog.class)); // Check log saved
    }

    @Test
    void submitRankings_validInput_3Movies_success() {
        // Arrange
        when(movieRepository.findAll()).thenReturn(availableMovies_3);

        // Act
        rankingService.submitRankings(testUser.getUserId(), validRankings_3);

        // Assert
        verify(userMovieRankingRepository, times(1)).deleteByUser(testUser);
        ArgumentCaptor<List<UserMovieRanking>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userMovieRankingRepository, times(1)).saveAll(listCaptor.capture());
        assertEquals(3, listCaptor.getValue().size());
        verify(rankingSubmissionLogRepository, times(1)).save(any(RankingSubmissionLog.class));
    }

    @Test
    void submitRankings_userNotFound_throwsUserNotFoundException() {
        // Arrange
        Long nonExistentUserId = 99L;
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            rankingService.submitRankings(nonExistentUserId, validRankings_5);
        });
        verify(userMovieRankingRepository, never()).saveAll(anyList());
        verify(rankingSubmissionLogRepository, never()).save(any());
    }

    @Test
    void submitRankings_noMoviesAvailable_throwsInvalidRankingException() {
        // Arrange
        when(movieRepository.findAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        InvalidRankingException exception = assertThrows(InvalidRankingException.class, () -> {
            rankingService.submitRankings(testUser.getUserId(), validRankings_5);
        });
        assertEquals("No movies available for ranking.", exception.getMessage());
        verify(userMovieRankingRepository, never()).saveAll(anyList());
        verify(rankingSubmissionLogRepository, never()).save(any());
    }

    @Test
    void submitRankings_invalidNumberOfRankings_throwsInvalidRankingException() {
        // Arrange (Expect 5, provide 4)
        List<RankingSubmitDTO> invalidRankings = validRankings_5.subList(0, 4);
        when(movieRepository.findAll()).thenReturn(availableMovies_5);

        // Act & Assert
        InvalidRankingException exception = assertThrows(InvalidRankingException.class, () -> {
            rankingService.submitRankings(testUser.getUserId(), invalidRankings);
        });
        assertTrue(exception.getMessage().contains("Invalid number of rankings submitted. Expected 5"));
    }

    @Test
    void submitRankings_duplicateMovieId_throwsInvalidRankingException() {
        // Arrange
        List<RankingSubmitDTO> invalidRankings = new ArrayList<>(validRankings_5);
        invalidRankings.set(4, createSubmitDTO(movie1.getMovieId(), 5)); // Duplicate movie1
        when(movieRepository.findAll()).thenReturn(availableMovies_5);

        // Act & Assert
        InvalidRankingException exception = assertThrows(InvalidRankingException.class, () -> {
            rankingService.submitRankings(testUser.getUserId(), invalidRankings);
        });
        assertTrue(exception.getMessage().contains("Duplicate movie ID"));
    }

    @Test
    void submitRankings_movieNotAvailable_throwsInvalidRankingException() {
        // Arrange
        List<RankingSubmitDTO> invalidRankings = new ArrayList<>(validRankings_5);
        invalidRankings.set(0, createSubmitDTO(movie6.getMovieId(), 1)); // movie6 is not in availableMovies_5
        when(movieRepository.findAll()).thenReturn(availableMovies_5);

        // Act & Assert
        InvalidRankingException exception = assertThrows(InvalidRankingException.class, () -> {
            rankingService.submitRankings(testUser.getUserId(), invalidRankings);
        });
        assertTrue(exception.getMessage().contains("is not available for ranking"));
    }

    @Test
    void submitRankings_duplicateRank_throwsInvalidRankingException() {
        // Arrange
        List<RankingSubmitDTO> invalidRankings = new ArrayList<>(validRankings_5);
        invalidRankings.set(4, createSubmitDTO(movie5.getMovieId(), 1)); // Duplicate rank 1
        when(movieRepository.findAll()).thenReturn(availableMovies_5);

        // Act & Assert
        InvalidRankingException exception = assertThrows(InvalidRankingException.class, () -> {
            rankingService.submitRankings(testUser.getUserId(), invalidRankings);
        });
        assertTrue(exception.getMessage().contains("Duplicate rank"));
    }

    @Test
    void submitRankings_rankOutOfRange_throwsInvalidRankingException() {
        // Arrange
        List<RankingSubmitDTO> invalidRankings = new ArrayList<>(validRankings_5);
        invalidRankings.set(0, createSubmitDTO(movie1.getMovieId(), 0)); // Rank 0 is invalid
        when(movieRepository.findAll()).thenReturn(availableMovies_5);

        // Act & Assert
        InvalidRankingException exception = assertThrows(InvalidRankingException.class, () -> {
            rankingService.submitRankings(testUser.getUserId(), invalidRankings);
        });
        assertTrue(exception.getMessage().contains("is outside the allowed range"));
    }

    // --- Tests for calculateAndSaveWinner ---

    @Test
    void calculateAndSaveWinner_noRankings_logsWarningAndReturns() {
        // Arrange
        when(userMovieRankingRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        rankingService.calculateAndSaveWinner();

        // Assert
        verify(rankingResultRepository, never()).save(any());
        // How to verify log output? Can use Logback test appenders or similar,
        // but for simplicity, we trust the logic flow here based on the 'never()' verification.
    }

    @Test
    void calculateAndSaveWinner_singleWinner_savesCorrectResult() {
        // Arrange
        User user2 = new User(); user2.setUserId(2L);
        List<UserMovieRanking> rankings = Arrays.asList(
                createRanking(testUser, movie1, 1),
                createRanking(testUser, movie2, 2),
                createRanking(user2, movie1, 1), // movie1 avg = 1.0
                createRanking(user2, movie2, 3)  // movie2 avg = 2.5
        );
        when(userMovieRankingRepository.findAll()).thenReturn(rankings);

        // Act
        rankingService.calculateAndSaveWinner();

        // Assert
        verify(rankingResultRepository, times(1)).save(argThat(result ->
                result.getWinningMovie().getMovieId() == movie1.getMovieId() &&
                result.getAverageRank() == 1.0 &&
                result.getCalculationTimestamp() != null // Check timestamp is set
        ));
    }

    @Test
    void calculateAndSaveWinner_tie_picksFirstEncounteredWinner() {
        // Arrange
        User user2 = new User(); user2.setUserId(2L);
        List<UserMovieRanking> rankings = Arrays.asList(
                createRanking(testUser, movie1, 1), // movie1 avg = 1.0
                createRanking(testUser, movie2, 1), // movie2 avg = 1.0
                createRanking(user2, movie1, 1),
                createRanking(user2, movie2, 1)
        );
        // The order returned by findAll() might influence which is "first" if map iteration order isn't guaranteed
        when(userMovieRankingRepository.findAll()).thenReturn(rankings);

        // Act
        rankingService.calculateAndSaveWinner();

        // Assert
        // In a tie, the implementation detail is that the one appearing first during the stream's min operation wins.
        // We verify *a* winner with the correct average rank is saved.
        verify(rankingResultRepository, times(1)).save(argThat(result ->
                (result.getWinningMovie().getMovieId() == movie1.getMovieId() || result.getWinningMovie().getMovieId() == movie2.getMovieId()) &&
                result.getAverageRank() == 1.0
        ));
    }

    // --- Tests for getLatestRankingResult ---

    @Test
    void getLatestRankingResult_resultExists_returnsResult() {
        // Arrange
        RankingResult expectedResult = new RankingResult();
        expectedResult.setId(1L);
        expectedResult.setWinningMovie(movie1);
        expectedResult.setAverageRank(1.5);
        expectedResult.setCalculationTimestamp(LocalDateTime.now());
        when(rankingResultRepository.findTopByOrderByCalculationTimestampDesc()).thenReturn(Optional.of(expectedResult));

        // Act
        Optional<RankingResult> actualResultOpt = rankingService.getLatestRankingResult();

        // Assert
        assertTrue(actualResultOpt.isPresent());
        assertEquals(expectedResult, actualResultOpt.get());
    }

    @Test
    void getLatestRankingResult_noResult_returnsEmptyOptional() {
        // Arrange
        when(rankingResultRepository.findTopByOrderByCalculationTimestampDesc()).thenReturn(Optional.empty());

        // Act
        Optional<RankingResult> actualResultOpt = rankingService.getLatestRankingResult();

        // Assert
        assertFalse(actualResultOpt.isPresent());
    }


    // --- Helper Methods ---
    private RankingSubmitDTO createSubmitDTO(long movieId, int rank) {
        RankingSubmitDTO dto = new RankingSubmitDTO();
        dto.setMovieId(movieId);
        dto.setRank(rank);
        return dto;
    }

    private UserMovieRanking createRanking(User user, Movie movie, int rank) {
        UserMovieRanking ranking = new UserMovieRanking();
        ranking.setUser(user);
        ranking.setMovie(movie);
        ranking.setRank(rank);
        // Set other fields if necessary (ID is usually auto-generated)
        return ranking;
    }
}
