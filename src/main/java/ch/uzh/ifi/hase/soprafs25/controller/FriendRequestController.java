package ch.uzh.ifi.hase.soprafs25.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs25.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.rest.dto.FriendRequestGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.FriendRequestService;
import ch.uzh.ifi.hase.soprafs25.service.UserService;
import ch.uzh.ifi.hase.soprafs25.utils.AuthorizationUtil;

@RestController
@RequestMapping("/friends")
public class FriendRequestController {
    private final FriendRequestService friendRequestService;
    private final UserService userService;
    
    public FriendRequestController(FriendRequestService friendRequestService, UserService userService){
        this.friendRequestService = friendRequestService;
        this.userService = userService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    // theoretically we can use List instead of Set, if order of friends matter
    public Set<UserGetDTO> getFriends(@RequestHeader("Authorization") String token) {
        token = AuthorizationUtil.extractToken(token);
        User user = userService.getUserByToken(token);
        Set<User> friends = user.getFriends(); 
        return friends.stream()
                     .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                     .collect(Collectors.toSet());
    }

    @PostMapping("/add/{receiverId}")
    @ResponseStatus(HttpStatus.OK)
    public FriendRequestGetDTO sendFriendRequest(@RequestHeader("Authorization") String token,@PathVariable Long receiverId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        FriendRequest friendRequest = friendRequestService.sendFriendRequest(userId, receiverId);
        return DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
    }

    @PostMapping("/friendrequest/{requestId}/accept")
    @ResponseStatus(HttpStatus.OK)
    public FriendRequestGetDTO acceptFriendRequest(@RequestHeader("Authorization") String token, @PathVariable Long requestId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        FriendRequest friendRequest = friendRequestService.acceptFriendRequest(requestId, userId);
        return DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
    }

    @PostMapping("/friendrequest/{requestId}/reject")
    @ResponseStatus(HttpStatus.OK)
    public FriendRequestGetDTO rejectFriendRequest(@RequestHeader("Authorization") String token, @PathVariable Long requestId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        FriendRequest friendRequest = friendRequestService.rejectFriendRequest(requestId, userId);
        return DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
    }

    @GetMapping("/friendrequests/sent")
    @ResponseStatus(HttpStatus.OK)
    public List<FriendRequestGetDTO> getSentFriendRequests(@RequestHeader("Authorization") String token) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        return friendRequestService.getPendingSentRequests(userId).stream()
                .map(DTOMapper.INSTANCE::convertEntityToFriendRequestGetDTO)
                .toList();
    }

    @GetMapping("/friendrequests/received")
    @ResponseStatus(HttpStatus.OK)
    public List<FriendRequestGetDTO> getReceivedFriendRequests(@RequestHeader("Authorization") String token) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        return friendRequestService.getPendingReceivedRequests(userId).stream()
                .map(DTOMapper.INSTANCE::convertEntityToFriendRequestGetDTO)
                .toList();
    }

    @DeleteMapping("/remove/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@RequestHeader("Authorization") String token, @PathVariable Long friendId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        friendRequestService.removeFriend(userId, friendId);
    }
}
