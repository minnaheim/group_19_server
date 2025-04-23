package ch.uzh.ifi.hase.soprafs25.controller;

import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.VoteStateGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingSubmitDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.GroupService;
import ch.uzh.ifi.hase.soprafs25.service.MoviePoolService;
import ch.uzh.ifi.hase.soprafs25.service.RankingService;
import ch.uzh.ifi.hase.soprafs25.service.UserService;
import ch.uzh.ifi.hase.soprafs25.utils.AuthorizationUtil;

@RestController
public class GroupController {

    @PostMapping("/groups/{groupId}/start-voting")
    @ResponseStatus(HttpStatus.OK)
    public String startVotingPhase(@RequestHeader("Authorization") String token, @PathVariable Long groupId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        groupService.startVotingPhase(groupId, userId);
        return "Voting phase started.";
    }

    @PostMapping(value = "/groups/{groupId}/show-results", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> showResultsPhase(@RequestHeader("Authorization") String token, @PathVariable Long groupId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        groupService.showResultsPhase(groupId, userId);
        // Calculate and save ranking results immediately after ending voting
        rankingService.calculateAndSaveWinner(groupId);
        return Collections.singletonMap("message", "Results phase started.");
    }


    private final GroupService groupService;
    private final UserService userService;
    private final MoviePoolService moviePoolService;
    private final RankingService rankingService;

    public GroupController(GroupService groupService, UserService userService, MoviePoolService moviePoolService, RankingService rankingService){
        this.groupService = groupService;
        this.userService = userService;
        this.moviePoolService = moviePoolService;
        this.rankingService = rankingService;
    }
    
        /**
     * Get a single group by its ID.
     * @param token The Bearer token of the user.
     * @param groupId The ID of the group to fetch.
     * @return GroupGetDTO containing group details including phase.
     * @throws UserNotFoundException if the user is not found or unauthorized.
     * @throws GroupNotFoundException if the group does not exist or the user is not a member.
     */
    @GetMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    public GroupGetDTO getGroup(@RequestHeader("Authorization") String token, @PathVariable Long groupId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        Group group = groupService.getGroup(groupId, userId);
        return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
    }

    @GetMapping("/groups")
    @ResponseStatus(HttpStatus.OK)
    public List<GroupGetDTO> getUserGroups(@RequestHeader("Authorization") String token) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        List<Group> userGroups = groupService.getGroupsByUserId(userId);
        return DTOMapper.INSTANCE.convertEntityListToGroupGetDTOList(userGroups);
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupGetDTO createGroup(@RequestHeader("Authorization") String token, @RequestBody GroupPostDTO groupPostDTO){
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        Group createdGroup = groupService.createGroup(groupPostDTO.getGroupName(), userId);

        return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(createdGroup);
    }
    
    @DeleteMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long groupId, @RequestHeader("Authorization") String token) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        groupService.deleteGroup(groupId, userId);
    }

    @GetMapping("/groups/{groupId}/members")
    @ResponseStatus(HttpStatus.OK)
    public List<UserGetDTO> getGroupMembers(@RequestHeader("Authorization") String token, @PathVariable Long groupId){
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        Group group = groupService.getGroup(groupId, userId);
        return DTOMapper.INSTANCE.convertEntityListToUserGetDTOList(group.getMembers());
    }

    // get movie pool 
    @GetMapping("/groups/{groupId}/pool")
    @ResponseStatus(HttpStatus.OK)
    public List<MovieGetDTO> getGroupMoviePool( @RequestHeader("Authorization") String token, @PathVariable Long groupId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        MoviePool moviePool = moviePoolService.getMoviePool(groupId, userId);
        return DTOMapper.INSTANCE.convertEntityListToMovieGetDTOList(moviePool.getMovies());
    }

    // add movie
    @PostMapping("/groups/{groupId}/pool/{movieId}")
    @ResponseStatus(HttpStatus.OK)
    public List<MovieGetDTO> addMovieToGroupPool(@RequestHeader("Authorization") String token, @PathVariable Long groupId, @PathVariable Long movieId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        MoviePool moviePool = moviePoolService.addMovie(groupId, movieId, userId);
        return DTOMapper.INSTANCE.convertEntityListToMovieGetDTOList(moviePool.getMovies());
    }

    @DeleteMapping("/groups/{groupId}/pool/{movieId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMovieFromGroupPool(@RequestHeader("Authorization") String token, @PathVariable Long groupId, @PathVariable Long movieId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        moviePoolService.removeMovie(groupId, movieId, userId);
    }

    @DeleteMapping("/groups/{groupId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveGroup(@RequestHeader("Authorization") String token, @PathVariable Long groupId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        groupService.leaveGroup(groupId, userId);
    }

    @PutMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    public GroupGetDTO updateGroupName(@PathVariable Long groupId,
                                       @RequestHeader("Authorization") String token,
                                       @RequestBody GroupPostDTO groupPostDTO) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        ch.uzh.ifi.hase.soprafs25.entity.Group updatedGroup =
            groupService.updateGroupName(groupId, userId, groupPostDTO.getGroupName());
        return DTOMapper.INSTANCE.convertEntityToGroupGetDTO(updatedGroup);
    }

    /**
     * GET /groups/{groupId}/vote-state
     * Returns both the movie pool and any existing user rankings.
     */
    @GetMapping("/groups/{groupId}/vote-state")
    @ResponseStatus(HttpStatus.OK)
    public VoteStateGetDTO getVoteState(@RequestHeader("Authorization") String token,
                                        @PathVariable Long groupId) {
        token = AuthorizationUtil.extractToken(token);
        Long userId = userService.getUserByToken(token).getUserId();
        List<MovieGetDTO> pool = DTOMapper.INSTANCE
            .convertEntityListToMovieGetDTOList(
                moviePoolService.getMoviePool(groupId, userId).getMovies());
        List<RankingSubmitDTO> ranks = rankingService.getUserRankings(userId, groupId);
        VoteStateGetDTO dto = new VoteStateGetDTO();
        dto.setPool(pool);
        dto.setRankings(ranks);
        return dto;
    }
}
