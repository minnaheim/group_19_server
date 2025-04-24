package ch.uzh.ifi.hase.soprafs25.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;


@WebAppConfiguration
@SpringBootTest
@Transactional
public class FriendRequestServiceIntegrationTest {

    @Autowired
    private FriendRequestService friendRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private User sender;
    private User receiver;

    @BeforeEach
    public void setup() {
        // clean up 
        friendRequestRepository.deleteAll();
        userRepository.deleteAll();
        // create test users
        // sender
        sender = new User();
        sender.setUsername("sender");
        sender.setPassword("password");
        sender.setEmail("sender@test.com");
        sender.setStatus(UserStatus.ONLINE);
        sender.setToken("sender-token");
        sender = userRepository.save(sender);
        // receiver
        receiver = new User();
        receiver.setUsername("receiver");
        receiver.setPassword("password");
        receiver.setEmail("receiver@test.com");
        receiver.setStatus(UserStatus.ONLINE);
        receiver.setToken("receiver-token");
        receiver = userRepository.save(receiver);
    }

    // successfull test for friend request workflow
    @Test
    void fullFriendRequestFlow_Success() {
        // Send friend request
        FriendRequest request = friendRequestService.sendFriendRequest(sender.getUserId(), receiver.getUserId());
        assertNotNull(request);
        assertEquals(sender, request.getSender());
        assertEquals(receiver, request.getReceiver());
        assertFalse(request.isAccepted());

        // get pending requests
        // received
        List<FriendRequest> pendingReceived = friendRequestService.getPendingReceivedRequests(receiver.getUserId());
        assertEquals(1, pendingReceived.size());
        assertEquals(request.getRequestId(), pendingReceived.get(0).getRequestId());
        // sent
        List<FriendRequest> pendingSent = friendRequestService.getPendingSentRequests(sender.getUserId());
        assertEquals(1, pendingSent.size());
        assertEquals(request.getRequestId(), pendingSent.get(0).getRequestId());

        // Accept friend request
        FriendRequest acceptedRequest = friendRequestService.acceptFriendRequest(request.getRequestId(), receiver.getUserId());
        assertTrue(acceptedRequest.isAccepted());
        assertNotNull(acceptedRequest.getResponseTime());

        // check that mutual friendship is established
        User updatedSender = userRepository.findById(sender.getUserId()).get();
        User updatedReceiver = userRepository.findById(receiver.getUserId()).get();
        assertTrue(updatedSender.getFriends().contains(updatedReceiver));
        assertTrue(updatedReceiver.getFriends().contains(updatedSender));

        // Verify no more pending requests
        assertTrue(friendRequestService.getPendingReceivedRequests(receiver.getUserId()).isEmpty());
        assertTrue(friendRequestService.getPendingSentRequests(sender.getUserId()).isEmpty());
    }

    // friend request rejection workflow test 
    @Test
    void friendRequestRejection_Success() {
        // send friend request
        FriendRequest request = friendRequestService.sendFriendRequest(sender.getUserId(), receiver.getUserId());

        // reject friend request
        FriendRequest rejectedRequest = friendRequestService.rejectFriendRequest(request.getRequestId(), receiver.getUserId());
        assertFalse(rejectedRequest.isAccepted());
        assertNotNull(rejectedRequest.getResponseTime());

        // check that friendship is not established (as supposed)
        User updatedSender = userRepository.findById(sender.getUserId()).get();
        User updatedReceiver = userRepository.findById(receiver.getUserId()).get();
        assertFalse(updatedSender.getFriends().contains(updatedReceiver));
        assertFalse(updatedReceiver.getFriends().contains(updatedSender));
    }

    // test duplicate requests validation
    @Test
    void duplicateFriendRequest_ThrowsException() {
        // send first friend request
        friendRequestService.sendFriendRequest(sender.getUserId(), receiver.getUserId());
        // Attempt to send duplicate request should result in an exception
        assertThrows(ResponseStatusException.class, () -> 
            friendRequestService.sendFriendRequest(sender.getUserId(), receiver.getUserId())
        );
    }

    // test for checking validation of "self-sent" request
    @Test
    void selfFriendRequest_ThrowsException() {
        assertThrows(ResponseStatusException.class, () ->
            friendRequestService.sendFriendRequest(sender.getUserId(), sender.getUserId())
        );
    }

    // test for deleting (cancelling) a request  - should success
    @Test
    void deleteRequest_Success() {
        // send friend request
        FriendRequest request = friendRequestService.sendFriendRequest(sender.getUserId(), receiver.getUserId());
        // cancel request
        friendRequestService.deleteRequest(request.getRequestId(), sender.getUserId());

        // Verify request is deleted
        assertTrue(friendRequestService.getPendingReceivedRequests(receiver.getUserId()).isEmpty());
        assertTrue(friendRequestService.getPendingSentRequests(sender.getUserId()).isEmpty());
    }

    // deletion of a request fails, because user is not the sender
    @Test
    void deleteRequest_NotAuthorized() {
        // send friend request
        FriendRequest request = friendRequestService.sendFriendRequest(sender.getUserId(), receiver.getUserId());

        // attempt to delete request as non-sender
        assertThrows(ResponseStatusException.class, () ->
            friendRequestService.deleteRequest(request.getRequestId(), receiver.getUserId())
        );
    }


    // same logic for accepting/rejection as non-receiver
    @Test
    void acceptRequest_NotAuthorized() {
        //send friend request
        FriendRequest request = friendRequestService.sendFriendRequest(sender.getUserId(), receiver.getUserId());

        // attempt to accept request as non-receiver
        assertThrows(ResponseStatusException.class, () ->
            friendRequestService.acceptFriendRequest(request.getRequestId(), sender.getUserId())
        );
    }

    @Test
    void rejectRequest_NotAuthorized() {
        // send friend request
        FriendRequest request = friendRequestService.sendFriendRequest(sender.getUserId(), receiver.getUserId());

        // attempt to reject request as non-receiver
        assertThrows(ResponseStatusException.class, () ->
            friendRequestService.rejectFriendRequest(request.getRequestId(), sender.getUserId())
        );
    }
} 