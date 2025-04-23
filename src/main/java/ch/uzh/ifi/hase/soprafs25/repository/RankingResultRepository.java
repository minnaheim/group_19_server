package ch.uzh.ifi.hase.soprafs25.repository;

import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("rankingResultRepository")
public interface RankingResultRepository extends JpaRepository<RankingResult, Long> {

    // Find the latest calculated result (ordered by timestamp descending)
    Optional<RankingResult> findTopByOrderByCalculationTimestampDesc();

    // Find the most recent result for a specific group
    Optional<RankingResult> findTopByGroupOrderByCalculationTimestampDesc(Group group);

    // Find all ranking results for a specific group
    List<RankingResult> findByGroup(Group group);

    // Could add methods to find results by rankingPeriodId if that's implemented
}
