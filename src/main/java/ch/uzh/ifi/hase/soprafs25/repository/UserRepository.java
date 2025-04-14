package ch.uzh.ifi.hase.soprafs25.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs25.entity.User;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, Long> {
  // User findByName(String name);

  User findByUsername(String username);
  User findByEmail(String email);
  User findByToken(String token);

  // to find all matches when searching for a friend
  List<User> findByUsernameContainingIgnoreCase(String username);

}
