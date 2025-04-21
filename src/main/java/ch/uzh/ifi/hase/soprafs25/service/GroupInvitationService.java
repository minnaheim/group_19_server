package ch.uzh.ifi.hase.soprafs25.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.GroupInvitation;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupInvitationRepository;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

@Service
@Transactional
public class GroupInvitationService {

    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Autowired
    public GroupInvitationService(GroupInvitationRepository groupInvitationRepository, GroupRepository groupRepository, UserRepository userRepository) {
        this.groupInvitationRepository = groupInvitationRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public GroupInvitation sendInvitation(Long groupId, Long senderId, Long receiverId) {
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found"));
        
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver not found"));

        // Check if the sender is a member of the group
        if (!group.getMembers().contains(sender)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group members can send invitations");
        }

        // Check if the receiver is already a member
        if (group.getMembers().contains(receiver)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of the group");
        }

        // Check if an invitation already exists
        if (groupInvitationRepository.existsByGroupAndReceiver(group, receiver)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already exists");
        }

        GroupInvitation invitation = new GroupInvitation();
        invitation.setGroup(group);
        invitation.setSender(sender);
        invitation.setReceiver(receiver);
        invitation.setCreationTime(LocalDateTime.now());
        invitation.setAccepted(false);
        return groupInvitationRepository.save(invitation);
    }

    public List<GroupInvitation> getReceivedInvitations(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return groupInvitationRepository.findByReceiver(user);
    }

    public List<GroupInvitation> getSentInvitations(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return groupInvitationRepository.findBySender(user);
    }

    public List<GroupInvitation> getPendingSentInvitations(Long userId) {
        // only to ensure that user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return groupInvitationRepository.findAllBySender_UserIdAndResponseTimeIsNull(userId);
    }

    public List<GroupInvitation> getPendingReceivedInvitations(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return groupInvitationRepository.findAllByReceiver_UserIdAndResponseTimeIsNull(userId);
    }

    // instead of reject/accept one fucntion
    public GroupInvitation respondToInvitation(Long invitationId, Long userId, boolean accept) {

        GroupInvitation invitation = groupInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!invitation.getReceiver().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to respond to this invitation");
        }

        if (invitation.getResponseTime() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already responded to");
        }

        invitation.setResponseTime(LocalDateTime.now());
        invitation.setAccepted(accept);
        if (accept) {
            Group group = invitation.getGroup();
            group.getMembers().add(invitation.getReceiver());
            groupRepository.save(group);
        }

        return groupInvitationRepository.save(invitation);
    }


    public void deleteInvitation(Long invitationId, Long userId) {
        
        GroupInvitation invitation = groupInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
    
        // Check if the user is authorized to delete the invitation
        if (!invitation.getSender().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this invitation");
        }

        groupInvitationRepository.delete(invitation);
    }
} 