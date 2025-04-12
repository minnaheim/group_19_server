package ch.uzh.ifi.hase.soprafs25.repository;

import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("userMovieRankingRepository")
public interface UserMovieRankingRepository extends JpaRepository<UserMovieRanking, Long> {

    // Find rankings by user
    List<UserMovieRanking> findByUser(User user);

    // Find a specific ranking by user and movie
    Optional<UserMovieRanking> findByUserAndMovie_MovieId(User user, long movieId);

    // Delete all rankings by a user (useful for re-ranking)
    void deleteByUser(User user);

    // You might need more specific queries later, e.g., finding all rankings for a specific "ranking period" if you add that concept.
}
