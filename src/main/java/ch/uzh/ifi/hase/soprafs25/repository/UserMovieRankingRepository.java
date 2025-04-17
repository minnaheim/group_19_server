package ch.uzh.ifi.hase.soprafs25.repository;

import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository("userMovieRankingRepository")
public interface UserMovieRankingRepository extends JpaRepository<UserMovieRanking, Long> {

    // Find rankings by user
    List<UserMovieRanking> findByUser(User user);

    // Find rankings by group
    List<UserMovieRanking> findByGroup(Group group);

    // Find a specific ranking by user and movie
    Optional<UserMovieRanking> findByUserAndMovie_MovieId(User user, Long movieId);

    // Delete all rankings by a user (useful for re-ranking)
    @Transactional
    void deleteByUser(User user);

    // Delete all rankings by a user for a specific group
    @Transactional
    void deleteByUserAndGroup(User user, Group group);

    // Find rankings by user and group
    List<UserMovieRanking> findByUserAndGroup(User user, Group group);

    // You might need more specific queries later, e.g., finding all rankings for a specific "ranking period" if you add that concept.
}
