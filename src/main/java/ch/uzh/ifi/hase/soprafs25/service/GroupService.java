package ch.uzh.ifi.hase.soprafs25.service;

import java.util.ArrayList;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

@Service
@Transactional
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Autowired
    public GroupService(GroupRepository groupRepository, UserRepository userRepository){
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public Group createGroup(String groupName, Long creatorId){
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found"));

        if (groupName == null || groupName.trim().isEmpty()){
            groupName = generateUniqueName();
        }

        else if (groupRepository.findByGroupName(groupName) != null){
            groupName = generateSimilarName(groupName);
        }

        Group newGroup = new Group();
        newGroup.setGroupName(groupName);
        newGroup.setCreator(creator);
        newGroup.setMembers(new ArrayList<>());
        newGroup.getMembers().add(creator);
        newGroup.setMoviePool(new ArrayList<>());

        return groupRepository.save(newGroup);
    }

    private String generateUniqueName() {
        String[] adjectives = {"Clever", "Brave", "Bright", "Happy", "Lucky", "Funny", "Beatiful", "Creative", "Modern", "Action", "Horrors", "Drama", "Comedy", "Fantasy"};
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
}
