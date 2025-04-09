package ch.uzh.ifi.hase.soprafs25.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.GroupService;
import ch.uzh.ifi.hase.soprafs25.service.UserService;

@RestController

public class GroupController {
    private final GroupService groupService;
    private final UserService userService;

    GroupController(GroupService groupService, UserService userService){
        this.groupService = groupService;
        this.userService = userService;
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupGetDTO createGroup(@RequestHeader("Authorization") String token, @RequestBody GroupPostDTO groupPostDTO){
        Long userId = userService.getUserByToken(token).getUserId();
        Group createdGroup = groupService.createGroup(groupPostDTO.getGroupName(), userId);

        return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(createdGroup);
    }
    
    @DeleteMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long groupId, @RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getUserId();
        groupService.deleteGroup(groupId, userId);
    }
}
