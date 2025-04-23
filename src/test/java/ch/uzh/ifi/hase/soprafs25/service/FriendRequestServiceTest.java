package ch.uzh.ifi.hase.soprafs25.service;

import java.util.Optional;
import java.util.Set;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

class FriendRequestServiceTest {
    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendRequestService friendRequestService;

    private User sender;
    private User receiver;
    private FriendRequest testRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // create test users
        sender = new User();
        sender.setUserId(1L);
        sender.setUsername("sender");

        receiver = new User();
        receiver.setUserId(2L);
        receiver.setUsername("receiver");

        // create test friendrequest
        testRequest = new FriendRequest();
        testRequest.setRequestId(1L);
        testRequest.setSender(sender);
        testRequest.setReceiver(receiver);
        // testRequest.setAccepted(false);
    }

    // first test for send method
    @Test
    void sendFriendRequest_Success() {
        // set up repo behaviour
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(testRequest);

        // call
        FriendRequest result = friendRequestService.sendFriendRequest(1L, 2L);

        // then
        assertNotNull(result);
        assertEquals(sender, result.getSender());
        assertEquals(receiver, result.getReceiver());
        assertFalse(result.isAccepted());
        verify(friendRequestRepository).save(any(FriendRequest.class));
    }

    // second test for send method - checks behvaiour when sender is not found
    @Test
    void sendFriendRequest_SenderNotFound() {
        // when
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // call 
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendRequestService.sendFriendRequest(1L, 2L));
        // then
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Sender not found", exception.getReason());
    }

    // third test for send method - same for receiver
    @Test
    void sendFriendRequest_ReceiverNotFound() {
        // when
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendRequestService.sendFriendRequest(1L, 2L));
        // then
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Receiver not found", exception.getReason());
    }

    // test for accept method
    @Test
    void acceptFriendRequest_Success() {
        // when
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(testRequest);
        when(userRepository.save(any(User.class))).thenReturn(sender).thenReturn(receiver);

        // call
        FriendRequest result = friendRequestService.acceptFriendRequest(1L, 2L);

        // then
        assertTrue(result.isAccepted());
        assertTrue(sender.getFriends().contains(receiver));
        assertTrue(receiver.getFriends().contains(sender));
        assertNotNull(result.getResponseTime());
        verify(userRepository, times(2)).save(any(User.class));
    }

    // similar test for reject
    @Test
    void rejectFriendRequest_Success() {
        // when
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(testRequest);
        // call
        FriendRequest result = friendRequestService.rejectFriendRequest(1L, 2L);

        // then
        assertFalse(result.isAccepted());
        assertNotNull(result.getResponseTime());
        verify(friendRequestRepository).save(any(FriendRequest.class));
    }

    // test for delete method - should be successful
    @Test
    void deleteRequest_Success() {
        // when
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // call
        friendRequestService.deleteRequest(1L, 1L);
        // then
        verify(friendRequestRepository).delete(testRequest);
    }

    // test for delete should fail, because not a sender tries to delete a request
    @Test
    void deleteRequest_NotAuthorized() {
        // when
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // call
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendRequestService.deleteRequest(1L, 3L));
        // then
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Not authorized to delete this request", exception.getReason());
    }

    // successfull retrieveing of sent requests
    @Test
    void getPendingSentRequests_Success() {
        // when
        Set<FriendRequest> sentRequests = Set.of(testRequest);
        sender.setSentFriendRequests(sentRequests);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        // call
        var result = friendRequestService.getPendingSentRequests(1L);
        // then
        assertEquals(1, result.size());
        assertEquals(testRequest, result.get(0));
    }


    // successfull retrieveing of received requests
    @Test
    void getPendingReceivedRequests_Success() {
        // when
        Set<FriendRequest> receivedRequests = Set.of(testRequest);
        receiver.setReceivedFriendRequests(receivedRequests);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));

        // call
        var result = friendRequestService.getPendingReceivedRequests(2L);

        // then
        assertEquals(1, result.size());
        assertEquals(testRequest, result.get(0));
    }

} 