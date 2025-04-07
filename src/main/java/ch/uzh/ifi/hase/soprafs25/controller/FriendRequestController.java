package ch.uzh.ifi.hase.soprafs25.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs25.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.service.FriendRequestService;
import ch.uzh.ifi.hase.soprafs25.service.UserService;

@RestController
@RequestMapping("/friends")
public class FriendRequestController {
    private final FriendRequestService friendRequestService;
    private final UserService userService;
    
    FriendRequestController(FriendRequestService friendRequestService, UserService userService){
        this.friendRequestService = friendRequestService;
        this.userService = userService;
    }


    @PostMapping("/add/{receiverId}")
    // we should discuss how do we manage token and user authentication - currently I suppose that token is included in requests as header
    // token is needed to identify which user is sending the request - for this I created a special method in UserService
    public User sendFriendRequest(@RequestHeader("Authorization") String token, @PathVariable Long receiverId){
        Long userId = userService.getUserByToken(token).getUserId();
        return friendRequestService.sendFriendRequest(userId, receiverId);
    }

    @PostMapping("/friendRequest/{requestId}/accept")
    public FriendRequest acceptFriendRequest(@RequestHeader("Authorization") String token, @PathVariable Long requestId){
        Long userId = userService.getUserByToken(token).getUserId();
        return friendRequestService.acceptFriendRequest(requestId, userId);
    }

    @PostMapping("/friendRequest/{requestId}/reject")
    public FriendRequest rejectFriendRequest(@RequestHeader("Authorization") String token, @PathVariable Long requestId){
        Long userId = userService.getUserByToken(token).getUserId();
        return friendRequestService.rejectFriendRequest(requestId, userId);
    }

    @GetMapping("/friendRequests/sent")
    public List<FriendRequest> getSentFriendRequests(@RequestHeader("Authorization") String token){
        Long userId = userService.getUserByToken(token).getUserId();
        return friendRequestService.getPendingSentRequests(userId);
    }

    @GetMapping("/friendRequests/received")
    public List<FriendRequest> getReceivedFriendRequests(@RequestHeader("Authorization") String token){
        Long userId = userService.getUserByToken(token).getUserId();
        return friendRequestService.getPendingReceivedRequests(userId);
    }

    @DeleteMapping("/remove/{friendId}")
    public User removeFriend(@RequestHeader("Authorization") String token, @PathVariable Long friendId){
        Long userId = userService.getUserByToken(token).getUserId();
        return friendRequestService.removeFriend(userId, friendId);
    }

}
