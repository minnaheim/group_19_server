package ch.uzh.ifi.hase.soprafs25.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs25.entity.GroupInvitation;
import ch.uzh.ifi.hase.soprafs25.service.GroupInvitationService;
import ch.uzh.ifi.hase.soprafs25.service.UserService;

@RestController
@RequestMapping("/groups/invitations")
public class GroupInvitationController {

    private final GroupInvitationService groupInvitationService;
    private final UserService userService;

    public GroupInvitationController(GroupInvitationService groupInvitationService, UserService userService) {
        this.groupInvitationService = groupInvitationService;
        this.userService = userService;
    }

    @PostMapping("/send/{groupId}/{receiverId}")
    public GroupInvitation sendGroupInvitation(@RequestHeader("Authorization") String token, 
                                                @PathVariable Long groupId, 
                                                @PathVariable Long receiverId) {
        Long senderId = userService.getUserByToken(token).getUserId();
        return groupInvitationService.sendInvitation(groupId, senderId, receiverId);
    }

    @PostMapping("/{invitationId}/accept")
    public GroupInvitation acceptGroupInvitation(@RequestHeader("Authorization") String token, 
                                                 @PathVariable Long invitationId) {
        Long userId = userService.getUserByToken(token).getUserId();
        return groupInvitationService.respondToInvitation(invitationId, userId, true);
    }

    @PostMapping("/{invitationId}/reject")
    public GroupInvitation rejectGroupInvitation(@RequestHeader("Authorization") String token, 
                                                 @PathVariable Long invitationId) {
        Long userId = userService.getUserByToken(token).getUserId();
        return groupInvitationService.respondToInvitation(invitationId, userId, false);
    }

    @GetMapping("/sent")
    public List<GroupInvitation> getSentGroupInvitations(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getUserId();
        return groupInvitationService.getSentInvitations(userId);
    }

    @GetMapping("/received")
    public List<GroupInvitation> getReceivedGroupInvitations(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getUserId();
        return groupInvitationService.getReceivedInvitations(userId);
    }

    // additionally - I don't know whether we want to have it or not, but I though that it logically suits
    @DeleteMapping("/{invitationId}")
    public void deleteGroupInvitation(@RequestHeader("Authorization") String token, 
                                      @PathVariable Long invitationId) {
        Long userId = userService.getUserByToken(token).getUserId();
        groupInvitationService.deleteInvitation(invitationId, userId);
    }
}