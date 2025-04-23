package ch.uzh.ifi.hase.soprafs25.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.GroupInvitation;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupInvitationRepository;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

class GroupInvitationServiceTest {
    @Mock
    private GroupInvitationRepository groupInvitationRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupInvitationService groupInvitationService;

    private User sender;
    private User receiver;
    private Group group;
    private GroupInvitation invitation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // create test users
        sender = new User();
        sender.setUserId(1L);
        sender.setUsername("sender");
        // receiver
        receiver = new User();
        receiver.setUserId(2L);
        receiver.setUsername("receiver");

        // create test group
        group = new Group();
        group.setGroupId(1L);
        group.setGroupName("testGroup");
        group.setCreator(sender);
        group.setMembers(new ArrayList<>());
        group.getMembers().add(sender);

        // Create test invitation
        invitation = new GroupInvitation();
        invitation.setInvitationId(1L);
        invitation.setGroup(group);
        invitation.setSender(sender);
        invitation.setReceiver(receiver);
        invitation.setCreationTime(LocalDateTime.now());
        invitation.setAccepted(false);
    }

    // test for invitation sending - successful
    @Test
    void sendInvitation_Success() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(groupInvitationRepository.existsByGroupAndReceiver(group, receiver)).thenReturn(false);
        when(groupInvitationRepository.save(any(GroupInvitation.class))).thenReturn(invitation);
        // call
        GroupInvitation result = groupInvitationService.sendInvitation(1L, 1L, 2L);

        // then
        assertNotNull(result);
        assertEquals(group, result.getGroup());
        assertEquals(sender, result.getSender());
        assertEquals(receiver, result.getReceiver());
        assertFalse(result.isAccepted());
        verify(groupInvitationRepository).save(any(GroupInvitation.class));
    }

    // test for invitation sending - fails, because sender is not member of the group
    @Test
    void sendInvitation_SenderNotMember_ThrowsException() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        group.getMembers().clear(); // Remove sender from group

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupInvitationService.sendInvitation(1L, 1L, 2L));
        // then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Only group members can send invitations", exception.getReason());
    }

    // test for invitation sending - fails, because receiver is already member of the group
    @Test
    void sendInvitation_ReceiverAlreadyMember_ThrowsException() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        group.getMembers().add(receiver); 

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupInvitationService.sendInvitation(1L, 1L, 2L));
        // then
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("User is already a member of the group", exception.getReason());
    }

    // test for invitation sending - fails, because invitation is already sent
    @Test
    void sendInvitation_DuplicateInvitation_ThrowsException() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(groupInvitationRepository.existsByGroupAndReceiver(group, receiver)).thenReturn(true);

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupInvitationService.sendInvitation(1L, 1L, 2L));
        // then
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Invitation already exists", exception.getReason());
    }

    // acceptance of an invitation
    @Test
    void respondToInvitation_Accept_Success() {
        // when
        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(groupInvitationRepository.save(any(GroupInvitation.class))).thenReturn(invitation);
        // call
        GroupInvitation result = groupInvitationService.respondToInvitation(1L, 2L, true);

        // then
        assertTrue(result.isAccepted());
        assertNotNull(result.getResponseTime());
        assertTrue(group.getMembers().contains(receiver));
        verify(groupRepository).save(group);
    }

    // rejection of an invitation
    @Test
    void respondToInvitation_Reject_Success() {
        // when
        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(groupInvitationRepository.save(any(GroupInvitation.class))).thenReturn(invitation);
        // call
        GroupInvitation result = groupInvitationService.respondToInvitation(1L, 2L, false);

        // then
        assertFalse(result.isAccepted());
        assertNotNull(result.getResponseTime());
        assertFalse(group.getMembers().contains(receiver));
    }

    // acceptance of invitation fails because user is not the receiver
    @Test
    void respondToInvitation_NotReceiver_ThrowsException() {
        // when
        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupInvitationService.respondToInvitation(1L, 1L, true));
        // then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Not authorized to respond to this invitation", exception.getReason());
    }

    // acceptance of invitation fails because user is not the receiver
    @Test
    void respondToInvitation_AlreadyResponded_ThrowsException() {
        // when
        invitation.setResponseTime(LocalDateTime.now());
        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupInvitationService.respondToInvitation(1L, 2L, true));
        // then
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Invitation already responded to", exception.getReason());
    }

    // test for invitation deletion - successful
    @Test
    void deleteInvitation_Success() {
        // when
        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        // call
        groupInvitationService.deleteInvitation(1L, 1L);

        // then
        verify(groupInvitationRepository).delete(invitation);
    }

    // test for invitation deletion - fails because user is not the sender
    @Test
    void deleteInvitation_NotSender_ThrowsException() {
        // when
        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupInvitationService.deleteInvitation(1L, 2L));
        // then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Not authorized to delete this invitation", exception.getReason());
    }

    // test for getting received invitations
    @Test
    void getPendingReceivedInvitations_Success() {
        // when
        List<GroupInvitation> invitations = List.of(invitation);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(groupInvitationRepository.findAllByReceiver_UserIdAndResponseTimeIsNull(2L)).thenReturn(invitations);

        // call
        List<GroupInvitation> result = groupInvitationService.getPendingReceivedInvitations(2L);
        // then
        assertEquals(1, result.size());
        assertEquals(invitation, result.get(0));
    }
    // test for getting sent invitations
    @Test
    void getPendingSentInvitations_Success() {
        // when
        List<GroupInvitation> invitations = List.of(invitation);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(groupInvitationRepository.findAllBySender_UserIdAndResponseTimeIsNull(1L)).thenReturn(invitations);
        // call
        List<GroupInvitation> result = groupInvitationService.getPendingSentInvitations(1L);
        // then
        assertEquals(1, result.size());
        assertEquals(invitation, result.get(0));
    }
}