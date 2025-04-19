package ch.uzh.ifi.hase.soprafs25.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs25.entity.Group;

@Repository("groupRepository")
public interface GroupRepository extends JpaRepository<Group, Long> {
    Group findByGroupName(String groupName);
    List<Group> findAllByMembers_UserId(Long userId);
} 