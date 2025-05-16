package ch.uzh.ifi.hase.soprafs25.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.GroupInvitation;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.UserMovieRanking;
import ch.uzh.ifi.hase.soprafs25.repository.GroupInvitationRepository;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.RankingResultRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserMovieRankingRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs25.rest.dto.VotingStatusDTO;

/**
 * Service class for handling group-related operations.
 */
@Service
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final MoviePoolService moviePoolService;
    private final UserMovieRankingRepository userMovieRankingRepository;
    private final RankingResultRepository rankingResultRepository;
    private final GroupInvitationRepository groupInvitationRepository;

    @Autowired
    public GroupService(GroupRepository groupRepository, UserRepository userRepository,
                            MovieRepository movieRepository, MoviePoolService moviePoolService,
                            UserMovieRankingRepository userMovieRankingRepository,
                            RankingResultRepository rankingResultRepository,
                            GroupInvitationRepository groupInvitationRepository){
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.moviePoolService = moviePoolService;
        this.userMovieRankingRepository = userMovieRankingRepository;
        this.rankingResultRepository = rankingResultRepository;
        this.groupInvitationRepository = groupInvitationRepository;
    }

    public Group createGroup(String groupName, Long creatorId){
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found"));

        validateGroupName(groupName);

        Group newGroup = new Group();
        newGroup.setGroupName(groupName);
        newGroup.setCreator(creator);
        newGroup.setMembers(new ArrayList<>());
        newGroup.getMembers().add(creator);
        groupRepository.saveAndFlush(newGroup);

        MoviePool moviePool = moviePoolService.createMoviePool(newGroup);
        newGroup.setMoviePool(moviePool);

        return groupRepository.save(newGroup);
    }

    private void validateGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name cannot be empty");
        }

        if (groupRepository.findByGroupName(groupName) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This group name is already taken");
        }

        String groupName_removed_all_spaces_in_front_and_back = groupName.trim();
        if (groupRepository.findByGroupName(groupName_removed_all_spaces_in_front_and_back) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This group name is already taken");
        }
    }

    private String generateUniqueName() {
        String[] adjectives = {"Clever", "Brave", "Bright", "Happy", "Lucky", "Funny", "Beautiful", "Creative", "Modern", "Action", "Horrors", "Drama", "Comedy", "Fantasy"};
        String[] nouns = {"Group", "Team", "Squad", "Crew", "Gang", "Party", "Guild", "MovieEnjoyers"};
        Random random = new Random();

        String name;
        do {
            String adj = adjectives[random.nextInt(adjectives.length)];
            String noun = nouns[random.nextInt(nouns.length)];
            Long number = random.nextLong(1_000_000_000);
            name = String.format("%s%s%d", adj, noun, number);
        } while (groupRepository.findByGroupName(name) != null);

        return name;
    }

    private String generateSimilarName(String baseGroupName){
        String similarName;
        int counter = 1;
        do {
            similarName = String.format("%s%d", baseGroupName, counter);
            counter++;
        } while (groupRepository.findByGroupName(similarName) != null);

        return similarName;
    }

    public void deleteGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can delete the group");
        }
        // Remove user movie rankings associated with this group
        List<UserMovieRanking> groupRankings = userMovieRankingRepository.findByGroup(group);
        if (!groupRankings.isEmpty()) {
            userMovieRankingRepository.deleteAll(groupRankings);
        }
        // Remove ranking results associated with this group
        List<RankingResult> results = rankingResultRepository.findByGroup(group);
        if (!results.isEmpty()) {
            rankingResultRepository.deleteAll(results);
        }
        // Remove all group invitations tied to this group to avoid FK constraint
        List<GroupInvitation> invites = groupInvitationRepository.findByGroup_GroupId(groupId);
        if (!invites.isEmpty()) {
            groupInvitationRepository.deleteAll(invites);
        }
        groupRepository.delete(group);
    }

    public boolean isUserMemberOfGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        return group.getMembers().stream()
                .anyMatch(member -> member.getUserId().equals(userId));
    }

    public Group getGroup(Long groupId, Long userId){
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        boolean isMember = isUserMemberOfGroup(groupId, userId);
        if(isMember){
            return group;
        }
        else{
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this group");
        }
    }

    public void leaveGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // check if provided user is actually member of provided group
        if (!group.getMembers().contains(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this group");
        }

        group.getMembers().remove(user);
        groupRepository.save(group);

        // Clean up any pending invitations for this user in this group
        List<GroupInvitation> pendingInvites = groupInvitationRepository.findAllByGroupAndReceiverAndResponseTimeIsNull(group, user);
        if (!pendingInvites.isEmpty()) {
            groupInvitationRepository.deleteAll(pendingInvites);
        }
    }

    public List<Group> getGroupsByUserId(Long userId) {
        return groupRepository.findAllByMembers_UserId(userId);
    }

    public Group updateGroupName(Long groupId, Long userId, String newName) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can update the group");
        }
        if (newName == null || newName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name cannot be empty");
        }
        if (!group.getGroupName().equals(newName) && groupRepository.findByGroupName(newName) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This group name is already taken");
        }
        group.setGroupName(newName);
        return groupRepository.save(group);
    }

    public void removeMember(Long groupId, Long memberId, Long adminUserId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getCreator().getUserId().equals(adminUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can remove members");
        }
        User member = userRepository.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!group.getMembers().contains(member)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a member of this group");
        }
        group.getMembers().remove(member);
        groupRepository.save(group);

        // Clean up any pending invitations for this user in this group
        List<GroupInvitation> pendingInvitesRm = groupInvitationRepository.findAllByGroupAndReceiverAndResponseTimeIsNull(group, member);
        if (!pendingInvitesRm.isEmpty()) {
            groupInvitationRepository.deleteAll(pendingInvitesRm);
        }
    }

    public void startPoolPhase(Long groupId, Long userId) {

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can start the pool phase");
        }

        group.setPhase(Group.GroupPhase.POOLING);
        group.setPhaseStartTime(LocalDateTime.now());
        groupRepository.save(group);
    }
    
    public void startVotingPhase(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        // make sure only creator can do it
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can start the voting phase");
        }
        if (group.getPhase() != Group.GroupPhase.POOLING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voting phase can only be started from POOLING phase");
        }
        
        group.setPhase(Group.GroupPhase.VOTING);
        // for timer handling
        group.setPhaseStartTime(LocalDateTime.now());
        groupRepository.save(group);
    }
    // for timer handling
    public void startPoolTimer(Long groupId, Long userId) {

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can start the pool phase");
        }
        if (group.getPhase() != Group.GroupPhase.POOLING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pool timer can only be started from POOLING phase");
        }
        if (group.getPoolPhaseDuration() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide duration before starting the timer");
        }
        // group.setPhase(Group.GroupPhase.POOLING);
        group.setPhaseStartTime(LocalDateTime.now());
        groupRepository.save(group);
    }

    public void startVotingTimer(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        // make sure only creator can do it
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can start the voting timer");
        }
        if (group.getPhase() == Group.GroupPhase.RESULTS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voting timer can only be started from POOLING or VOTING phases");
        }
        if (group.getVotingPhaseDuration() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide duration before starting the timer");
        }
        group.setPhase(Group.GroupPhase.VOTING);
        // for timer handling
        group.setPhaseStartTime(LocalDateTime.now());
        groupRepository.save(group);
    }

    public void showResultsPhase(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can show results phase");
        }
        if (group.getPhase() != Group.GroupPhase.VOTING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Results phase can only be started from VOTING phase");
        }
        group.setPhase(Group.GroupPhase.RESULTS);
        groupRepository.save(group);
    }

    // fixed rate is in ms
    @Scheduled(fixedRate = 5000) // check every 5 sec
    public void checkPhaseTimers() {
        // get all currenct groups and iterate over them
        List<Group> activeGroups = groupRepository.findAll();
        for (Group group : activeGroups) {

            if (group.getPhaseStartTime() != null) {
                int passedSeconds = (int) Duration.between(group.getPhaseStartTime(), LocalDateTime.now()).getSeconds();
                
                if (group.getPhase() == Group.GroupPhase.POOLING && group.getPoolPhaseDuration() != null && passedSeconds >= group.getPoolPhaseDuration()) {
                    // switch to voting phase
                    group.setPhase(Group.GroupPhase.VOTING);
                    group.setPhaseStartTime(LocalDateTime.now());
                    groupRepository.save(group);
                } 

                else if (group.getPhase() == Group.GroupPhase.VOTING && group.getVotingPhaseDuration() != null && 
                passedSeconds >= group.getVotingPhaseDuration()) {
                    // switch to results phase
                    group.setPhase(Group.GroupPhase.RESULTS);
                    groupRepository.save(group);
                }
            }
        }
    }

    // to get time left
    public int getRemainingTime(Long groupId) {

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (group.getPhaseStartTime() == null || group.getPhase() == Group.GroupPhase.RESULTS ||
            (group.getPhase() == Group.GroupPhase.POOLING && group.getPoolPhaseDuration() == null) ||
            (group.getPhase() == Group.GroupPhase.VOTING && group.getVotingPhaseDuration() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active timer for this group");
        }
        
        int elapsed = (int) Duration.between(group.getPhaseStartTime(), LocalDateTime.now()).getSeconds();
        int totalDuration;
        if(group.getPhase() == Group.GroupPhase.POOLING){
            totalDuration = group.getPoolPhaseDuration();
        }
        else{
            totalDuration = group.getVotingPhaseDuration();}

        // either 0 or time left
        return Math.max(0, totalDuration - elapsed);
    }
    
    // setting pool duration
    public void setPoolPhaseDuration(Long groupId, Long userId, Integer duration) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can set timer duration");
        }
        group.setPoolPhaseDuration(duration);
        groupRepository.save(group);
    }
    // setting votting duration
    public void setVotingPhaseDuration(Long groupId, Long userId, Integer duration) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can set timer duration");
        }
        group.setVotingPhaseDuration(duration);
        groupRepository.save(group);
    }

    // for check which users have already submitted voting and which not
    public List<VotingStatusDTO> getVotingStatus(Long groupId, Long requestingUserId){
    
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    
    // only group creator can see it
    if (!group.getCreator().getUserId().equals(requestingUserId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group creator can view voting status");
    }

    // get all users who have submitted rankings, thus - voted
    Set<Long> usersWhoVoted = userMovieRankingRepository.findByGroup(group)
        .stream()
        .map(ranking -> ranking.getUser().getUserId())
        .collect(Collectors.toSet());

    // return list of VotingStatusDTO objects 
    return group.getMembers().stream().map(user -> new VotingStatusDTO(
            user.getUserId(),
            user.getUsername(),
            usersWhoVoted.contains(user.getUserId())
        )).collect(Collectors.toList());
    }
}
