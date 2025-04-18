package ch.uzh.ifi.hase.soprafs25.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;


@Service
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    
    private final MovieRepository movieRepository;
    private final MoviePoolService moviePoolService;

    @Autowired
    public GroupService(GroupRepository groupRepository, UserRepository userRepository,
                            MovieRepository movieRepository, MoviePoolService moviePoolService){
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.moviePoolService = moviePoolService;
    }

    public Group createGroup(String groupName, Long creatorId){
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found"));

        // System.out.println(groupName);
        validateGroupName(groupName);
        // for creating similar/unique name
        // if (groupName == null || groupName.trim().isEmpty()){
        //     groupName = generateUniqueName();
        // }

        // else if (groupRepository.findByGroupName(groupName) != null){
        //     groupName = generateSimilarName(groupName);
        // }
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
        // Check if the user is the creator of the group
        if (!group.getCreator().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can delete the group");
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
    }

    public List<Group> getGroupsByUserId(Long userId) {
        return groupRepository.findAllByMembers_UserId(userId);
    }
}
