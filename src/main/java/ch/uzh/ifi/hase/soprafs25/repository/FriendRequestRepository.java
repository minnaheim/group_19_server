package ch.uzh.ifi.hase.soprafs25.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs25.entity.FriendRequest;

@Repository("userRepository")
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

}
