package ch.uzh.ifi.hase.soprafs25.controller;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.GroupInvitation;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupInvitationRepository;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GroupInvitationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupInvitationRepository groupInvitationRepository;

    private User user1, user2, user3;
    private String token1, token2, token3;
    private Group group;
    private GroupInvitation invitation;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        groupRepository.deleteAll();
        groupInvitationRepository.deleteAll();

        user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("password");
        user1.setEmail("user1@example.com");
        user1.setStatus(UserStatus.ONLINE);
        token1 = "token1";
        user1.setToken(token1);
        user1 = userRepository.saveAndFlush(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("password");
        user2.setEmail("user2@example.com");
        user2.setStatus(UserStatus.ONLINE);
        token2 = "token2";
        user2.setToken(token2);
        user2 = userRepository.saveAndFlush(user2);

        user3 = new User();
        user3.setUsername("user3");
        user3.setPassword("password");
        user3.setEmail("user3@example.com");
        user3.setStatus(UserStatus.ONLINE);
        token3 = "token3";
        user3.setToken(token3);
        user3 = userRepository.saveAndFlush(user3);

        // create test group
        group = new Group();
        group.setGroupName("Test Group");
        group.setCreator(user1);
        group.setMembers(new java.util.ArrayList<>());
        group.getMembers().add(user1);
        group = groupRepository.saveAndFlush(group);

        // test invitation
        invitation = new GroupInvitation();
        invitation.setGroup(group);
        invitation.setSender(user1);
        invitation.setReceiver(user2);
        invitation.setCreationTime(LocalDateTime.now());
        invitation.setAccepted(false);
        invitation = groupInvitationRepository.saveAndFlush(invitation);
    }

    // valid sending of invitation
    @Test
    void sendGroupInvitation_validInput_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/invitations/send/{groupId}/{receiverId}", group.getGroupId(), user3.getUserId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group.groupId").value(group.getGroupId()))
                .andExpect(jsonPath("$.sender.userId").value(user1.getUserId()))
                .andExpect(jsonPath("$.receiver.userId").value(user3.getUserId()))
                .andExpect(jsonPath("$.accepted").value(false));
    }


    // sending an invitation to a user who is already a member of the group
    @Test
    void sendGroupInvitation_toExistingMember_returnsConflict() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/invitations/send/{groupId}/{receiverId}", group.getGroupId(), user1.getUserId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    // successful aception of an invitation
    @Test
    void acceptGroupInvitation_validRequest_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/invitations/{invitationId}/accept", invitation.getInvitationId())
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.responseTime").exists());

        // make sure that user was added to group
        Group updatedGroup = groupRepository.findById(group.getGroupId()).orElseThrow();
        assertTrue(updatedGroup.getMembers().contains(user2), "User should be added to group after accepting invitation");
        // invitation also should be updated
        GroupInvitation updatedInvitation = groupInvitationRepository.findById(invitation.getInvitationId()).orElseThrow();
        assertTrue(updatedInvitation.isAccepted());
        assertNotNull(updatedInvitation.getResponseTime());
    }

    // try to accept an invitation which was already accepted before
    @Test
    void acceptGroupInvitation_alreadyAccepted_returnsConflict() throws Exception {
        // First accept the invitation
        invitation.setAccepted(true);
        invitation.setResponseTime(LocalDateTime.now());
        groupInvitationRepository.saveAndFlush(invitation);

        mockMvc.perform(MockMvcRequestBuilders.post("/groups/invitations/{invitationId}/accept", invitation.getInvitationId())
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    // similar tests for rejection of an invitation
    @Test
    void rejectGroupInvitation_validRequest_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/invitations/{invitationId}/reject", invitation.getInvitationId())
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.responseTime").exists());

        // verify user was not added to group
        Group updatedGroup = groupRepository.findById(group.getGroupId()).orElseThrow();
        assertFalse(updatedGroup.getMembers().contains(user2), "User should not be added to group after rejecting invitation");
        // invitation also should be updated
        GroupInvitation updatedInvitation = groupInvitationRepository.findById(invitation.getInvitationId()).orElseThrow();
        assertFalse(updatedInvitation.isAccepted());
        assertNotNull(updatedInvitation.getResponseTime());
    }

    // retrieve sent invitations
    @Test
    void getPendingSentGroupInvitations_validRequest_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/invitations/sent")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sender.userId").value(user1.getUserId()))
                .andExpect(jsonPath("$[0].receiver.userId").value(user2.getUserId()))
                .andExpect(jsonPath("$[0].accepted").value(false));
    }

    // similar for received 
    @Test
    void getPendingReceivedGroupInvitations_validRequest_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/invitations/received")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sender.userId").value(user1.getUserId()))
                .andExpect(jsonPath("$[0].receiver.userId").value(user2.getUserId()))
                .andExpect(jsonPath("$[0].accepted").value(false));
    }

    // successful cancelation of invitation
    @Test
    void deleteGroupInvitation_validRequest_returnsNoContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/invitations/{invitationId}", invitation.getInvitationId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // verify invitation was deleted
        assertFalse(groupInvitationRepository.existsById(invitation.getInvitationId()));
    }

    // try to delete invtitation not by creator of it
    @Test
    void deleteGroupInvitation_notSender_returnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/invitations/{invitationId}", invitation.getInvitationId())
                .header("Authorization", "Bearer " + token3)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteGroupInvitation_alreadyAccepted_returnsConflict() throws Exception {
        // accept the invitation
        invitation.setAccepted(true);
        invitation.setResponseTime(LocalDateTime.now());
        groupInvitationRepository.saveAndFlush(invitation);

        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/invitations/{invitationId}", invitation.getInvitationId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void sendGroupInvitation_duplicateInvitation_returnsConflict() throws Exception {
        // invitation already exists in setup between user1 and user2
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/invitations/send/{groupId}/{receiverId}", group.getGroupId(), user2.getUserId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    // try to accept invitation by not receiver
    @Test
    void respondToInvitation_notReceiver_returnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/invitations/{invitationId}/accept", invitation.getInvitationId())
                .header("Authorization", "Bearer " + token3)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPendingReceivedGroupInvitations_onlyReturnsPending() throws Exception {
        // create an accepted invitation
        GroupInvitation acceptedInvitation = new GroupInvitation();
        acceptedInvitation.setGroup(group);
        acceptedInvitation.setSender(user1);
        acceptedInvitation.setReceiver(user3);
        acceptedInvitation.setAccepted(true);
        acceptedInvitation.setCreationTime(LocalDateTime.now().minusMinutes(1));
        acceptedInvitation.setResponseTime(LocalDateTime.now());
        groupInvitationRepository.saveAndFlush(acceptedInvitation);

        mockMvc.perform(MockMvcRequestBuilders.get("/groups/invitations/received")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].receiver.userId").value(user2.getUserId()));
    }
}