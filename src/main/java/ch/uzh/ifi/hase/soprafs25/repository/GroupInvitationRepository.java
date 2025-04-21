package ch.uzh.ifi.hase.soprafs25.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.GroupInvitation;
import ch.uzh.ifi.hase.soprafs25.entity.User;

@Repository("groupInvitationRepository")
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {


    List<GroupInvitation> findByReceiver(User receiver);
    List<GroupInvitation> findBySender(User sender);

    // otherwise an error appear, due to some naming collisions
    List<GroupInvitation> findByGroup_GroupId(Long groupId);

    boolean existsByGroupAndReceiver(Group group, User receiver);

    // for handling "pending" requests
    List<GroupInvitation> findAllBySender_UserIdAndResponseTimeIsNull(Long senderId);
    List<GroupInvitation> findAllByReceiver_UserIdAndResponseTimeIsNull(Long receiverId);
} 