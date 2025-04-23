package ch.uzh.ifi.hase.soprafs25.service;

import java.util.ArrayList;
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
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

class GroupServiceTest {
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRepository userRepository;


    @Mock
    private MoviePoolService moviePoolService;

    @InjectMocks
    private GroupService groupService;

    private User testUser;
    private Group testGroup;
    private MoviePool testMoviePool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // create test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");

        // create test group
        testGroup = new Group();
        testGroup.setGroupId(1L);
        testGroup.setGroupName("testGroup");
        testGroup.setCreator(testUser);
        testGroup.setMembers(new ArrayList<>());
        testGroup.getMembers().add(testUser);
        // create test movie pool
        testMoviePool = new MoviePool();
        testMoviePool.setGroup(testGroup);
    }

    // test for group creation validation - successful creation 
    @Test
    void createGroup_Success() {
        // when
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(groupRepository.findByGroupName("testGroup")).thenReturn(null);
        when(groupRepository.saveAndFlush(any(Group.class))).thenReturn(testGroup);
        when(moviePoolService.createMoviePool(any(Group.class))).thenReturn(testMoviePool);
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);

        // call
        Group result = groupService.createGroup("testGroup", 1L);

        // then
        assertNotNull(result);
        assertEquals("testGroup", result.getGroupName());
        assertEquals(testUser, result.getCreator());
        assertTrue(result.getMembers().contains(testUser));
        verify(groupRepository).saveAndFlush(any(Group.class));
        verify(moviePoolService).createMoviePool(any(Group.class));
    }

    // test for group creation validation - creation fails due to an empty name 
    @Test
    void createGroup_EmptyName_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.createGroup("", 1L));
        // then
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Group name cannot be empty", exception.getReason());
    }

    // test for group creation validation - creation fails due to duplicate name 
    @Test
    void createGroup_DuplicateName_ThrowsException() {
        // when
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(groupRepository.findByGroupName("testGroup")).thenReturn(testGroup);

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.createGroup("testGroup", 1L));
        // then
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("This group name is already taken", exception.getReason());
    }

    // test for group deletion - successful deletion 
    @Test
    void deleteGroup_Success() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        // call
        groupService.deleteGroup(1L, 1L);
        // then
        verify(groupRepository).delete(testGroup);
    }

    // test for group deletion - deletion failes because user is not the creator
    @Test
    void deleteGroup_NotCreator_ThrowsException() {
        // whehn
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.deleteGroup(1L, 2L));
        // then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Only the group creator can delete the group", exception.getReason());
    }

    // test for group deletion - deletion fails because gropu is not found
    @Test
    void deleteGroup_GroupNotFound_ThrowsException() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());
        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.deleteGroup(1L, 1L));

        // then
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Group not found", exception.getReason());
    }

    // tests for isUserMember method
    @Test
    void isUserMemberOfGroup_Success() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        // call
        boolean result = groupService.isUserMemberOfGroup(1L, 1L);
        // then
        assertTrue(result);
    }

    @Test
    void isUserMemberOfGroup_NotMember_ReturnsFalse() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        // call
        boolean result = groupService.isUserMemberOfGroup(1L, 2L);

        // then
        assertFalse(result);
    }

    // tests for getting group methid
    @Test
    void getGroup_Success() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // call
        Group result = groupService.getGroup(1L, 1L);
        // then
        assertNotNull(result);
        assertEquals(testGroup, result);
    }

    @Test
    void getGroup_NotMember_ThrowsException() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.getGroup(1L, 2L));
        // then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("User is not a member of this group", exception.getReason());
    }

    // test for leaving a group - successful
    @Test
    void leaveGroup_Success() {


        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);
        // call
        groupService.leaveGroup(1L, 1L);

        // then
        verify(groupRepository).save(any(Group.class));
    }

    // leaving a group fails, because user is not member of this group
    @Test
    void leaveGroup_NotMember_ThrowsException() {
        // when
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.leaveGroup(1L, 2L));
        // then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("User is not a member of this group", exception.getReason());
    }
}