package ch.uzh.ifi.hase.soprafs25.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

@Service
@Transactional
public class FriendRequestService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;

    @Autowired
    public FriendRequestService(UserRepository userRepository, FriendRequestRepository friendRequestRepository) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    public FriendRequest sendFriendRequest(Long senderId, Long receiverId) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found"));
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver not found"));

        if (sender.equals(receiver)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send request to yourself");
        }

        if (sender.getFriends().contains(receiver)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already friends");
        }

        if (hasPendingRequest(sender, receiver)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Friend request already exists - wait for response on it");
        }

        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        return friendRequestRepository.save(request);
    }

    public FriendRequest acceptFriendRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));
        
        // maybe it's better to move this check to controller 
        if (!request.getReceiver().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to accept this request");
        }

        request.setAccepted(true);
        // request.setResponseTime(LocalDateTime.now());
        
        User sender = request.getSender();
        User receiver = request.getReceiver();


        // add friends mutually
        sender.getFriends().add(receiver);
        receiver.getFriends().add(sender);
        
        userRepository.save(sender);
        userRepository.save(receiver);
        return friendRequestRepository.save(request);
    }

    public FriendRequest rejectFriendRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));
        
        // once again maybe it's better to move this check to controller  
        if (!request.getReceiver().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to reject this request");
        }

        request.setAccepted(false);
        // request.setResponseTime(LocalDateTime.now());
        return friendRequestRepository.save(request);
    }

    public List<FriendRequest> getPendingReceivedRequests(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return user.getReceivedFriendRequests().stream()
        // check if Responsetime is set which happens only after actual Response
            .filter(req -> req.getResponseTime() == null)
            .collect(Collectors.toList());
    }

    public List<FriendRequest> getPendingSentRequests(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return user.getSentFriendRequests().stream()
            .filter(req -> req.getResponseTime() == null)
            .collect(Collectors.toList());
    }

    public User removeFriend(Long userId, Long friendId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));

        if (!user.getFriends().contains(friend)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This user is not your friend");
        }

        user.getFriends().remove(friend);
        friend.getFriends().remove(user);
        userRepository.save(user);
        return userRepository.save(friend);
    }

    // auxilary function which is used in sendRequest to prevent duplicate requests
    private boolean hasPendingRequest(User sender, User receiver) {
        return receiver.getReceivedFriendRequests().stream()
            .anyMatch(req -> req.getSender().equals(sender) && req.getResponseTime() == null);
    }
} 