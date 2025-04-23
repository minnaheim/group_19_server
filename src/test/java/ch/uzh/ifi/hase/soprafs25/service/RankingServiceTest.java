package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.*;
import ch.uzh.ifi.hase.soprafs25.exceptions.*;
import ch.uzh.ifi.hase.soprafs25.repository.*;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private UserMovieRankingRepository userMovieRankingRepository;
    @Mock
    private RankingResultRepository rankingResultRepository;
    @Mock
    private RankingSubmissionLogRepository rankingSubmissionLogRepository; // Added

    @InjectMocks
    private RankingService rankingService;

    private User testUser;
    private Group testGroup;
    private MoviePool testMoviePool;
    private Movie movie1, movie2, movie3, movie4, movie5, movie6; // movie6 is NOT in pool
    private List<Movie> availableMovies_5;
    private List<RankingSubmitDTO> validRankings_5; // Ranks 1-5 for movies 1-5
    private List<RankingSubmitDTO> validRankings_3; // Ranks 1-3 for movies 1-3 (for pools < 5 movies)
    private final Long testUserId = 1L;
    private final Long testGroupId = 10L;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(testUserId); // Set ID directly
        testUser.setUsername("testUser");

        testGroup = new Group();
        testGroup.setGroupId(testGroupId); // Set ID directly
        testGroup.setGroupName("Test Group");
        // Add testUser to group members - important if service checks membership
        testGroup.setMembers(new ArrayList<>(Collections.singletonList(testUser)));

        // Setup Movie Pool specific to the group
        testMoviePool = new MoviePool();
        movie1 = createMovie(101L);
        movie2 = createMovie(102L);
        movie3 = createMovie(103L);
        movie4 = createMovie(104L);
        movie5 = createMovie(105L);
        movie6 = createMovie(106L); // Not added to the pool initially
        availableMovies_5 = Arrays.asList(movie1, movie2, movie3, movie4, movie5);
        testMoviePool.setMovies(new ArrayList<>(availableMovies_5));
        testGroup.setMoviePool(testMoviePool); // Associate pool with group

        // Prepare valid ranking DTOs based on availableMovies_5
        validRankings_5 = availableMovies_5.stream()
                .map(m -> createSubmitDTO(m.getMovieId(), availableMovies_5.indexOf(m) + 1))
                .collect(Collectors.toList());
        // Prepare valid ranking DTOs for smaller pool scenario
        validRankings_3 = Arrays.asList(
                createSubmitDTO(movie1.getMovieId(), 1),
                createSubmitDTO(movie2.getMovieId(), 2),
                createSubmitDTO(movie3.getMovieId(), 3)
        );


        // --- Mock Basic Repository Interactions for testUser/testGroup ---
        // Use lenient() for mocks in @BeforeEach that might not be used by all tests
        lenient().when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        lenient().when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));

        // Mock finding existing rankings for the group (initially empty)
        lenient().when(userMovieRankingRepository.findByGroup(testGroup)).thenReturn(new ArrayList<>());

        // Mock movie repository lookups (assuming these are needed by multiple tests)
        lenient().when(movieRepository.findById(movie1.getMovieId())).thenReturn(Optional.of(movie1));
        lenient().when(movieRepository.findById(movie2.getMovieId())).thenReturn(Optional.of(movie2));
        lenient().when(movieRepository.findById(movie3.getMovieId())).thenReturn(Optional.of(movie3));
        lenient().when(movieRepository.findById(movie4.getMovieId())).thenReturn(Optional.of(movie4));
        lenient().when(movieRepository.findById(movie5.getMovieId())).thenReturn(Optional.of(movie5));

        // Mock save operations (often needed, make lenient)
        lenient().when(userMovieRankingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(rankingResultRepository.save(any(RankingResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock finding latest result (make lenient as only specific tests use it)
        lenient().when(rankingResultRepository.findTopByGroupOrderByCalculationTimestampDesc(testGroup)).thenReturn(Optional.empty());
    }

    // Helper method to create Movie
    private Movie createMovie(Long id) {
        Movie movie = new Movie();
        try {
            java.lang.reflect.Field idField = Movie.class.getDeclaredField("movieId");
            idField.setAccessible(true);
            idField.set(movie, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set movieId for testing", e);
        }
        movie.setTitle("Movie " + id);
        return movie;
    }

    // Helper method to create RankingSubmitDTO
    private RankingSubmitDTO createSubmitDTO(Long movieId, Integer rank) {
        RankingSubmitDTO dto = new RankingSubmitDTO();
        dto.setMovieId(movieId);
        dto.setRank(rank);
        return dto;
    }

    // --- Tests for submitRankings ---

    @Test
    void submitRankings_validInput_5Movies_success() {
        // Arrange - uses default setUp with 5 movies in pool

        // Set group phase to VOTING for valid submission
        testGroup.setPhase(Group.GroupPhase.VOTING);
        // Act
        rankingService.submitRankings(testUserId, testGroupId, validRankings_5);

        // Assert
        ArgumentCaptor<List<UserMovieRanking>> rankingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(userMovieRankingRepository, times(1)).deleteByUserAndGroup(eq(testUser), eq(testGroup));
        verify(userMovieRankingRepository, times(1)).saveAll(rankingsCaptor.capture());
        verify(rankingSubmissionLogRepository, times(1)).save(any(RankingSubmissionLog.class));

        List<UserMovieRanking> savedRankings = rankingsCaptor.getValue();
        assertEquals(5, savedRankings.size());
        // Check group association and details for one ranking
        assertEquals(testGroup, savedRankings.get(0).getGroup());
        assertEquals(testUser, savedRankings.get(0).getUser());
        assertEquals(movie1.getMovieId(), savedRankings.get(0).getMovie().getMovieId()); // Use == for long
        assertEquals(1, savedRankings.get(0).getRank());
    }

    @Test
    void submitRankings_validInput_3Movies_success() {
         // Arrange - Modify the pool for this test
         List<Movie> availableMovies_3 = Arrays.asList(movie1, movie2, movie3);
         testMoviePool.setMovies(new ArrayList<>(availableMovies_3)); // Update pool in testGroup for this test

         // Set group phase to VOTING for valid submission
         testGroup.setPhase(Group.GroupPhase.VOTING);
         // Act
         rankingService.submitRankings(testUserId, testGroupId, validRankings_3); // Submit 3 rankings

         // Assert
         ArgumentCaptor<List<UserMovieRanking>> rankingsCaptor = ArgumentCaptor.forClass(List.class);
         verify(userMovieRankingRepository, times(1)).deleteByUserAndGroup(eq(testUser), eq(testGroup));
         verify(userMovieRankingRepository, times(1)).saveAll(rankingsCaptor.capture());
         verify(rankingSubmissionLogRepository, times(1)).save(any(RankingSubmissionLog.class));

         List<UserMovieRanking> savedRankings = rankingsCaptor.getValue();
         assertEquals(3, savedRankings.size()); // Expect 3 rankings
         assertEquals(testGroup, savedRankings.get(0).getGroup());
         assertEquals(movie1.getMovieId(), savedRankings.get(0).getMovie().getMovieId());
         assertEquals(1, savedRankings.get(0).getRank());
    }


    @Test
    void submitRankings_userNotFound_throwsUserNotFoundException() {
        // Arrange
        Long nonExistentUserId = 99L;
        // Ensure findById for this specific ID returns empty
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Set group phase to VOTING for phase check
        testGroup.setPhase(Group.GroupPhase.VOTING);
        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            rankingService.submitRankings(nonExistentUserId, testGroupId, validRankings_5);
        });
         verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
         verify(userMovieRankingRepository, never()).saveAll(anyList());
    }

    @Test
    void submitRankings_groupNotFound_throwsGroupNotFoundException() {
        // Arrange
        Long nonExistentGroupId = 99L;
         // Ensure findById for this specific ID returns empty
        when(groupRepository.findById(nonExistentGroupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(GroupNotFoundException.class, () -> {
            rankingService.submitRankings(testUserId, nonExistentGroupId, validRankings_5);
        });
         verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
         verify(userMovieRankingRepository, never()).saveAll(anyList());
    }

     @Test
     void submitRankings_noMoviePool_throwsConflict() {
         // Arrange
         testGroup.setMoviePool(null); // Explicitly set pool to null for this test

         // Act & Assert
         ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            rankingService.submitRankings(testUserId, testGroupId, validRankings_5);
        });
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
         verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
         verify(userMovieRankingRepository, never()).saveAll(anyList());
     }

     @Test
     void submitRankings_noMoviesInPool_throwsConflict() {
         // Arrange
         testMoviePool.setMovies(new ArrayList<>()); // Empty movie list
         testGroup.setMoviePool(testMoviePool);

         // Act & Assert
         ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            // Need to submit an empty list as requiredRankings will be 0
            rankingService.submitRankings(testUserId, testGroupId, new ArrayList<>());
        });
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
         verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
         verify(userMovieRankingRepository, never()).saveAll(anyList());
     }


     @Test
     void submitRankings_invalidNumberOfRankings_throwsConflict() {
         // Arrange - Default pool has 5 movies, requires 5 rankings
         List<RankingSubmitDTO> tooFewRankings = Collections.singletonList(createSubmitDTO(movie1.getMovieId(), 1));

         // Act & Assert
         ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            rankingService.submitRankings(testUserId, testGroupId, tooFewRankings);
        });
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
         verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
         verify(userMovieRankingRepository, never()).saveAll(anyList());
     }

     @Test
     void submitRankings_duplicateMovieId_throwsConflict() {
         // Arrange - Pool has 5 movies
         List<RankingSubmitDTO> rankingsWithDuplicateMovie = Arrays.asList(
                 createSubmitDTO(movie1.getMovieId(), 1),
                 createSubmitDTO(movie2.getMovieId(), 2),
                 createSubmitDTO(movie3.getMovieId(), 3),
                 createSubmitDTO(movie4.getMovieId(), 4),
                 createSubmitDTO(movie1.getMovieId(), 5) // Duplicate movie1
         );

         // Act & Assert
         ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            rankingService.submitRankings(testUserId, testGroupId, rankingsWithDuplicateMovie);
        });
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
          verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
          verify(userMovieRankingRepository, never()).saveAll(anyList());
     }

     @Test
     void submitRankings_movieNotAvailableInPool_throwsConflict() {
         // Arrange - Pool has movies 1-5. Movie 6 is not in the pool.
         List<RankingSubmitDTO> rankingsWithUnavailableMovie = Arrays.asList(
                 createSubmitDTO(movie1.getMovieId(), 1),
                 createSubmitDTO(movie2.getMovieId(), 2),
                 createSubmitDTO(movie3.getMovieId(), 3),
                 createSubmitDTO(movie4.getMovieId(), 4),
                 createSubmitDTO(movie6.getMovieId(), 5) // Movie 6 not in pool
         );

         // Act & Assert
         ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            rankingService.submitRankings(testUserId, testGroupId, rankingsWithUnavailableMovie);
        });
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
          verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
          verify(userMovieRankingRepository, never()).saveAll(anyList());
     }

     @Test
     void submitRankings_duplicateRank_throwsConflict() {
          // Arrange - Pool has 5 movies
         List<RankingSubmitDTO> rankingsWithDuplicateRank = Arrays.asList(
                 createSubmitDTO(movie1.getMovieId(), 1),
                 createSubmitDTO(movie2.getMovieId(), 2),
                 createSubmitDTO(movie3.getMovieId(), 3),
                 createSubmitDTO(movie4.getMovieId(), 4),
                 createSubmitDTO(movie5.getMovieId(), 4) // Duplicate rank 4
         );

         // Act & Assert
         ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            rankingService.submitRankings(testUserId, testGroupId, rankingsWithDuplicateRank);
        });
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
          verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
          verify(userMovieRankingRepository, never()).saveAll(anyList());
     }

     @Test
     void submitRankings_rankOutOfRange_throwsConflict() {
         // Arrange - Pool has 5 movies, ranks should be 1-5
         List<RankingSubmitDTO> rankingsWithOutOfRangeRank = Arrays.asList(
                 createSubmitDTO(movie1.getMovieId(), 1),
                 createSubmitDTO(movie2.getMovieId(), 2),
                 createSubmitDTO(movie3.getMovieId(), 3),
                 createSubmitDTO(movie4.getMovieId(), 4),
                 createSubmitDTO(movie5.getMovieId(), 6) // Rank 6 is out of range
         );

         // Act & Assert
         ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            rankingService.submitRankings(testUserId, testGroupId, rankingsWithOutOfRangeRank);
        });
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
          verify(userMovieRankingRepository, never()).deleteByUserAndGroup(any(), any());
          verify(userMovieRankingRepository, never()).saveAll(anyList());
     }

    // --- Tests for calculateAndSaveWinner ---

    @Test
    void calculateAndSaveWinner_groupNotFound_throwsGroupNotFoundException() {
        // Arrange
        Long nonExistentGroupId = 99L;
        // Mock repo to find no group for this ID
        when(groupRepository.findById(nonExistentGroupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(GroupNotFoundException.class, () -> {
            rankingService.calculateAndSaveWinner(nonExistentGroupId);
        });
        verify(rankingResultRepository, never()).save(any(RankingResult.class));
    }

    @Test
    void calculateAndSaveWinner_noRankings_logsWarningAndReturns() {
         // Arrange - Default setup has no rankings in userMovieRankingRepository.findByGroup(testGroup)

         // Act
         rankingService.calculateAndSaveWinner(testGroupId);

         // Assert
         // Check logs (if possible/needed) or just verify no result was saved
         verify(rankingResultRepository, never()).save(any(RankingResult.class));
         // We could also capture log messages if using a test logger appender
    }

    @Test
    void calculateAndSaveWinner_singleWinner_savesCorrectResult() {
        // Arrange
        User user2 = new User(); user2.setUsername("user2");
        user2.setUserId(2L); // Set ID directly
        List<UserMovieRanking> rankings = Arrays.asList(
                createRanking(testUser, movie1, 1, testGroup), // User 1 ranks Movie 1 as #1
                createRanking(testUser, movie2, 2, testGroup),
                createRanking(user2, movie1, 1, testGroup),    // User 2 also ranks Movie 1 as #1
                createRanking(user2, movie3, 2, testGroup)
        );
        when(userMovieRankingRepository.findByGroup(testGroup)).thenReturn(rankings); // Mock repo to return these rankings

        // Act
        rankingService.calculateAndSaveWinner(testGroupId);

        // Assert
        ArgumentCaptor<RankingResult> resultCaptor = ArgumentCaptor.forClass(RankingResult.class);
        verify(rankingResultRepository, times(1)).save(resultCaptor.capture());

        RankingResult savedResult = resultCaptor.getValue();
        assertEquals(movie1.getMovieId(), savedResult.getWinningMovie().getMovieId()); // Use == for long
        assertEquals(1.0, savedResult.getAverageRank());
        assertEquals(testGroup, savedResult.getGroup()); // Verify group is set
        assertNotNull(savedResult.getCalculationTimestamp());
    }

     @Test
     void calculateAndSaveWinner_tie_picksFirstEncounteredWinner() {
         // Arrange
         User user2 = new User(); user2.setUsername("user2");
         user2.setUserId(2L); // Set ID directly
         // Movie 1: Rank 1 (user1), Rank 2 (user2) -> Avg 1.5
         // Movie 2: Rank 2 (user1), Rank 1 (user2) -> Avg 1.5 (TIE)
         List<UserMovieRanking> rankings = Arrays.asList(
                 createRanking(testUser, movie1, 1, testGroup),
                 createRanking(testUser, movie2, 2, testGroup),
                 createRanking(user2, movie1, 2, testGroup),
                 createRanking(user2, movie2, 1, testGroup)
         );
         when(userMovieRankingRepository.findByGroup(testGroup)).thenReturn(rankings);

         // Act
         rankingService.calculateAndSaveWinner(testGroupId);

         // Assert
         ArgumentCaptor<RankingResult> resultCaptor = ArgumentCaptor.forClass(RankingResult.class);
         verify(rankingResultRepository, times(1)).save(resultCaptor.capture());
         RankingResult savedResult = resultCaptor.getValue();

         // Depending on iteration order, winner could be movie1 or movie2, both avg 1.5
         assertTrue(savedResult.getWinningMovie().getMovieId() == movie1.getMovieId() || savedResult.getWinningMovie().getMovieId() == movie2.getMovieId());
         assertEquals(1.5, savedResult.getAverageRank());
         assertEquals(testGroup, savedResult.getGroup());
     }


    // --- Tests for getLatestRankingResult ---

    @Test
    void getLatestRankingResult_groupNotFound_throwsGroupNotFoundException() {
        // Arrange
        Long nonExistentGroupId = 99L;
        // Mock repo to find no group for this ID
        when(groupRepository.findById(nonExistentGroupId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(GroupNotFoundException.class, () -> {
            rankingService.getLatestRankingResult(nonExistentGroupId);
        });
    }

    @Test
void getLatestRankingResult_noResultFound_throwsNotFoundIfNotResultsPhase() {
    // Arrange - set group phase to something other than RESULTS
    testGroup.setPhase(Group.GroupPhase.VOTING);
    when(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup));

    // Act & Assert
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
        rankingService.getLatestRankingResult(testGroupId);
    });
    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    assertTrue(ex.getReason().contains("Ranking results can only be viewed during the RESULTS phase"));
}

    @Test
    void getLatestRankingResult_resultFound_returnsResult() {
        // Set group phase to RESULTS for result retrieval
        testGroup.setPhase(Group.GroupPhase.RESULTS);
        // Arrange
        RankingResult latestResult = new RankingResult();
        latestResult.setId(50L); // Set ID directly
        latestResult.setGroup(testGroup);
        latestResult.setWinningMovie(movie1);
        latestResult.setAverageRank(1.2);
        latestResult.setCalculationTimestamp(LocalDateTime.now());
        // Mock repo to return this result for the test group
        when(rankingResultRepository.findTopByGroupOrderByCalculationTimestampDesc(testGroup)).thenReturn(Optional.of(latestResult));

        // Act
        RankingResult result = rankingService.getLatestRankingResult(testGroupId)
                .orElseThrow(() -> new AssertionError("Expected result not found"));

        // Assert
        assertNotNull(result);
        assertEquals(latestResult.getId(), result.getId()); // Corrected: Use getId() (already correct here)
        assertEquals(latestResult.getWinningMovie().getMovieId(), result.getWinningMovie().getMovieId());
        assertEquals(testGroup, result.getGroup()); // Verify group is set
    }

    // Helper method to create UserMovieRanking instances for testing calculateWinner
    private UserMovieRanking createRanking(User user, Movie movie, int rank, Group group) {
        UserMovieRanking ranking = new UserMovieRanking();
        ranking.setUser(user);
        ranking.setMovie(movie);
        ranking.setRank(rank);
        ranking.setGroup(group); // Set the group
        return ranking;
    }

}
