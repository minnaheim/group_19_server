package ch.uzh.ifi.hase.soprafs25.service;

import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.ActiveProfiles;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

@WebAppConfiguration
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class GroupIntegrationTest {
    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupInvitationService groupInvitationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    public void setup() {
        // clean everything up
        groupRepository.deleteAll();
        userRepository.deleteAll();

        // create test users
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setPassword("password");
        testUser1.setEmail("testuser1@test.com");
        testUser1.setToken("token1");
        testUser1.setStatus(UserStatus.ONLINE);
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setPassword("password");
        testUser2.setEmail("testuser2@test.com");
        testUser2.setToken("token2");
        testUser2.setStatus(UserStatus.ONLINE);
        testUser2 = userRepository.save(testUser2);
    }

    // test for group creation - successful
    @Test
    public void groupCreation_Success() {
        Group createdGroup = groupService.createGroup("Test Group", testUser1.getUserId());
        // verify creation
        assertNotNull(createdGroup.getGroupId());
        assertEquals("Test Group", createdGroup.getGroupName());
        assertTrue(createdGroup.getMembers().contains(testUser1));
        assertEquals(testUser1, createdGroup.getCreator());
    }

    // group creation failes because group name is empty
    @Test
    public void groupCreation_InvalidName() {
        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.createGroup("", testUser1.getUserId()));
        // then
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    // test for sending a group invitation - successful
    @Test
    public void groupInvitation_Success() {
        // group creation
        Group group = groupService.createGroup("Test Group", testUser1.getUserId());
        // send invitation
        groupInvitationService.sendInvitation(group.getGroupId(), testUser1.getUserId(), testUser2.getUserId());

        // verify invitation is sent
        assertTrue(!groupInvitationService.getPendingReceivedInvitations(testUser2.getUserId()).isEmpty());

        // simulate acceptance of the invitation by the second user
        groupInvitationService.respondToInvitation(
                groupInvitationService.getPendingReceivedInvitations(testUser2.getUserId()).get(0).getInvitationId(),
                testUser2.getUserId(), true);

        // check membership of the second user in the group
        Group updatedGroup = groupService.getGroup(group.getGroupId(), testUser2.getUserId());
        assertTrue(updatedGroup.getMembers().contains(testUser2));
    }

    @Test
    public void groupInvitation_RejectInvitation_Success() {
        // create a group
        Group group = groupService.createGroup("Test Group", testUser1.getUserId());
        // send invitation
        groupInvitationService.sendInvitation(group.getGroupId(), testUser1.getUserId(), testUser2.getUserId());

        // rejection of invitation
        groupInvitationService.respondToInvitation(
                groupInvitationService.getPendingReceivedInvitations(testUser2.getUserId()).get(0).getInvitationId(),
                testUser2.getUserId(), false);

        // verify sevond user is not a member of the group
        // if user is not a member - getGorup should throw an exception
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.getGroup(group.getGroupId(), testUser2.getUserId()));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("User is not a member of this group", exception.getReason());
    }

    // test for group deletion- successful
    @Test
    public void groupDeletion_Success() {
        Group group = groupService.createGroup("Test Group", testUser1.getUserId());
        // delete the group
        groupService.deleteGroup(group.getGroupId(), testUser1.getUserId());
        // group is deleted
        // getGroup contains check for group existence in groupRepository - thus a
        // NOT_FOUND exception should be thrown
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.getGroup(group.getGroupId(), testUser1.getUserId()));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    // deletion of a group fails, because user is not its creator
    @Test
    public void groupDeletion_Unauthorized() {
        // create a group
        Group group = groupService.createGroup("Test Group", testUser1.getUserId());

        // a non-cretor (second user) tries to delete
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.deleteGroup(group.getGroupId(), testUser2.getUserId()));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Only the group creator can delete the group", exception.getReason());
    }

    // test for leaving a groyp
    @Test
    public void groupLeave_Success() {
        // create a group
        Group group = groupService.createGroup("Test Group", testUser1.getUserId());

        // send invitation to the second user, because creator can't leave the group
        groupInvitationService.sendInvitation(group.getGroupId(), testUser1.getUserId(), testUser2.getUserId());
        // accept it by second user
        groupInvitationService.respondToInvitation(
                groupInvitationService.getPendingReceivedInvitations(testUser2.getUserId()).get(0).getInvitationId(),
                testUser2.getUserId(), true);
        // seccond user leaves the group
        groupService.leaveGroup(group.getGroupId(), testUser2.getUserId());

        // verify second user is no longer a member of the group
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.getGroup(group.getGroupId(), testUser2.getUserId()));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("User is not a member of this group", exception.getReason());

    }
}