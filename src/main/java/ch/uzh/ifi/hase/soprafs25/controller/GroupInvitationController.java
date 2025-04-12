package ch.uzh.ifi.hase.soprafs25.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs25.entity.GroupInvitation;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupInvitationGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
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
    @ResponseStatus(HttpStatus.OK)
    public GroupInvitationGetDTO sendGroupInvitation(@RequestHeader("Authorization") String token, 
                                                @PathVariable Long groupId, 
                                                @PathVariable Long receiverId) {
        Long senderId = userService.getUserByToken(token).getUserId();
        GroupInvitation groupInvitation = groupInvitationService.sendInvitation(groupId, senderId, receiverId);
        return DTOMapper.INSTANCE.convertEntityToGroupInvitationGetDTO(groupInvitation);
    }

    @PostMapping("/{invitationId}/accept")
    @ResponseStatus(HttpStatus.OK)
    public GroupInvitationGetDTO acceptGroupInvitation(@RequestHeader("Authorization") String token, @PathVariable Long invitationId) {
        Long userId = userService.getUserByToken(token).getUserId();
        GroupInvitation invitation = groupInvitationService.respondToInvitation(invitationId, userId, true);
        return DTOMapper.INSTANCE.convertEntityToGroupInvitationGetDTO(invitation);
    }

    @PostMapping("/{invitationId}/reject")
    @ResponseStatus(HttpStatus.OK)
    public GroupInvitationGetDTO rejectGroupInvitation(@RequestHeader("Authorization") String token,
                                                      @PathVariable Long invitationId) {
        Long userId = userService.getUserByToken(token).getUserId();
        GroupInvitation invitation = groupInvitationService.respondToInvitation(invitationId, userId, false);
        return DTOMapper.INSTANCE.convertEntityToGroupInvitationGetDTO(invitation);
    }

    @GetMapping("/sent")
    @ResponseStatus(HttpStatus.OK)
    public List<GroupInvitationGetDTO> getSentGroupInvitations(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getUserId();
        return groupInvitationService.getSentInvitations(userId).stream()
                .map(DTOMapper.INSTANCE::convertEntityToGroupInvitationGetDTO)
                .toList();
    }

    @GetMapping("/received")
    @ResponseStatus(HttpStatus.OK)
    public List<GroupInvitationGetDTO> getReceivedGroupInvitations(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getUserId();
        return groupInvitationService.getReceivedInvitations(userId).stream()
                .map(DTOMapper.INSTANCE::convertEntityToGroupInvitationGetDTO)
                .toList();
    }

    // additionally - I don't know whether we want to have it or not, but I though that it logically suits
    @DeleteMapping("/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroupInvitation(@RequestHeader("Authorization") String token, @PathVariable Long invitationId) {
        Long userId = userService.getUserByToken(token).getUserId();
        groupInvitationService.deleteInvitation(invitationId, userId);
    }
}