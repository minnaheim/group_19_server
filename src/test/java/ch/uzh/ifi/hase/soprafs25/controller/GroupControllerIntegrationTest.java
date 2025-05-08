package ch.uzh.ifi.hase.soprafs25.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MovieRepository movieRepository;


    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;
    private Group testGroup;


    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        groupRepository.deleteAll();
        movieRepository.deleteAll();

        // create test users
        user1 = new User();
        user1.setUsername("testUser1");
        user1.setPassword("password");
        user1.setEmail("test@mail.com");
        user1.setStatus(UserStatus.ONLINE);
        user1.setToken("token1"); 
        user1 = userRepository.saveAndFlush(user1);
        user1Token = user1.getToken();

        user2 = new User();
        user2.setUsername("testUser2");
        user2.setPassword("password");
        user2.setEmail("test2@mail.com");
        user2.setStatus(UserStatus.ONLINE);
        user2.setToken("token2"); 
        user2 = userRepository.saveAndFlush(user2);
        user2Token = user2.getToken();

        // create test group
        testGroup = new Group();
        testGroup.setGroupName("Test Group");
        testGroup.setCreator(user1);
        testGroup.setMembers(new ArrayList<>()); 
        testGroup.getMembers().add(user1);
        testGroup = groupRepository.save(testGroup);
    }

    @Test
    void createGroup_validInput_returnsCreated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupName\":\"New Group\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupName").value("New Group"));
    }

    @Test
    void getGroup_validId_returnsGroup() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(testGroup.getGroupId()))
                .andExpect(jsonPath("$.groupName").value(testGroup.getGroupName()));
    }

    @Test
    void getUserGroups_returnsUserGroups() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(testGroup.getGroupId()));
    }

    @Test
    void deleteGroup_validRequest_returnsNoContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());
                // Verify that the group no longer exists in the database
        boolean groupExists = groupRepository.existsById(testGroup.getGroupId());
        assertFalse(groupExists, "The group should have been deleted from the database.");
                
    }

    @Test
    void getGroupMembers_returnsMembers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/members", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(user1.getUserId()));
    }

    @Test
    void leaveGroup_validRequest_returnsNoContent() throws Exception {
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);

        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}/leave", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNoContent());
        Group updatedGroup = groupRepository.findById(testGroup.getGroupId()).orElse(null);
        // check that user2 is no longer a member of the group
        assertNotNull(updatedGroup, "The group should still exist.");
        assertFalse(updatedGroup.getMembers().contains(user2), "User2 should have left the group.");
    }

    @Test
    void updateGroupName_validRequest_returnsUpdatedGroup() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupName\":\"Updated Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("Updated Name"));
    }

    @Test
    void startVotingPhase_validRequest_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-voting", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Voting phase started."));
    }

    @Test
    void showResultsPhase_validRequest_returnsOk() throws Exception {
        testGroup.setPhase(Group.GroupPhase.VOTING);
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/show-results", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Results phase started."));
    }


    @Test
    void removeMember_validRequest_returnsNoContent() throws Exception {

        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}/members/{memberId}", 
                testGroup.getGroupId(), user2.getUserId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

    // get the group from the database
    Group updatedGroup = groupRepository.findById(testGroup.getGroupId()).orElse(null);
    // check that user2 is no longer a member of the group
    assertNotNull(updatedGroup, "The group should still exist.");
    assertFalse(updatedGroup.getMembers().contains(user2), "User2 should have been removed from the group.");
    }


    @Test
    void setPoolPhaseDuration_validRequest_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/groups/{groupId}/pool-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("60"))
                .andExpect(status().isOk());
    }

    @Test
    void setVotingPhaseDuration_validRequest_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/groups/{groupId}/voting-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("60"))
                .andExpect(status().isOk());
    }

    @Test
    void getRemainingTime_returnsTimeInSeconds() throws Exception {
        testGroup.setPhase(Group.GroupPhase.POOL);
        testGroup.setPoolPhaseDuration(300);
        testGroup.setPhaseStartTime(LocalDateTime.now());
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    // negative test cases
    @Test
    void createGroup_missingName_returnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGroup_notMember_returnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteGroup_notOwner_returnsForbidden() throws Exception {
        
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);

        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeMember_notOwner_returnsForbidden() throws Exception {
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);

        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}/members/{memberId}", 
                testGroup.getGroupId(), user1.getUserId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void startVotingPhase_notOwner_returnsForbidden() throws Exception {


        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);

        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-voting", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void addMovieToGroupPool_notMember_returnsForbidden() throws Exception {
        testGroup.getMembers().add(user1);
        groupRepository.saveAndFlush(testGroup);
        Movie testMovie = new Movie();
        testMovie.setMovieId(1L);
        movieRepository.save(testMovie);

        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/pool/{movieId}", 
                testGroup.getGroupId(), testMovie.getMovieId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void setPoolPhaseDuration_notOwner_returnsForbidden() throws Exception {
        testGroup.getMembers().add(user2);
        
        groupRepository.save(testGroup);

        mockMvc.perform(MockMvcRequestBuilders.put("/groups/{groupId}/pool-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("60"))
                .andExpect(status().isForbidden());
    }
}