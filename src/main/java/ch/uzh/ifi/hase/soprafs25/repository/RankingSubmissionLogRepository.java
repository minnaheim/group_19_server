package ch.uzh.ifi.hase.soprafs25.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs25.entity.RankingSubmissionLog;
import ch.uzh.ifi.hase.soprafs25.entity.User;

@Repository("rankingSubmissionLogRepository")
public interface RankingSubmissionLogRepository extends JpaRepository<RankingSubmissionLog, Long> {

    // Find logs by user (optional, might be useful for history)
    List<RankingSubmissionLog> findByUserOrderBySubmissionTimeDesc(User user);
    
    // Find all submission logs for a specific user
    List<RankingSubmissionLog> findByUser(User user);

    // Potentially add findByUserAndRankingPeriodId if that concept is added
}
