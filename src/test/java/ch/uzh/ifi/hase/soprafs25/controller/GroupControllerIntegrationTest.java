package ch.uzh.ifi.hase.soprafs25.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.hamcrest.Matchers.hasSize;
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
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MoviePoolRepository;
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

    @Autowired
    private MoviePoolRepository moviePoolRepository;


    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;
    private Group testGroup;
    private MoviePool moviePool;


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

        // create and associate movie pool with test group
        moviePool = new MoviePool();
        moviePool.setGroup(testGroup);
        moviePool.setMovies(new ArrayList<>());
        moviePool.setLastUpdated(LocalDateTime.now());
        moviePool = moviePoolRepository.save(moviePool);
        testGroup.setMoviePool(moviePool);
        testGroup = groupRepository.save(testGroup);
    }

    // successful creation of group
    @Test
    void createGroup_validInput_returnsCreated() throws Exception {
        // send request with some mock data
        mockMvc.perform(MockMvcRequestBuilders.post("/groups")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupName\":\"New Group\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupName").value("New Group"));
    }

    // successfully retrive group by id
    @Test
    void getGroup_validId_returnsGroup() throws Exception {
        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                // make sure it;s test group
                .andExpect(jsonPath("$.groupId").value(testGroup.getGroupId()))
                .andExpect(jsonPath("$.groupName").value(testGroup.getGroupName()));
    }

    // successfully retrive groups of user (by token)
    @Test
    void getUserGroups_returnsUserGroups() throws Exception {
        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/groups")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(testGroup.getGroupId()));
    }

    // successfully delete group
    @Test
    void deleteGroup_validRequest_returnsNoContent() throws Exception {
        // send request
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());
        // verify that the group no longer exists in the database
        boolean groupExists = groupRepository.existsById(testGroup.getGroupId());
        assertFalse(groupExists, "The group should have been deleted from the database.");
                
    }

    // successfully retrieve members of group
    @Test
    void getGroupMembers_returnsMembers() throws Exception {
        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/members", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                // creator of the group and the only member is there
                .andExpect(jsonPath("$[0].userId").value(user1.getUserId()));
    }

    // successfully leave group
    @Test
    void leaveGroup_validRequest_returnsNoContent() throws Exception {
        // add one more user
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);
        // user2 leaves the group
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}/leave", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNoContent());
        Group updatedGroup = groupRepository.findById(testGroup.getGroupId()).orElse(null);
        // check that user2 is no longer a member of the group
        assertNotNull(updatedGroup, "The group should still exist.");
        assertFalse(updatedGroup.getMembers().contains(user2), "User2 should have left the group.");
    }

    // successfully update groupname
    @Test
    void updateGroupName_validRequest_returnsUpdatedGroup() throws Exception {
        // send request wirh some new mock grouoname
        mockMvc.perform(MockMvcRequestBuilders.put("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupName\":\"Updated Name\"}"))
                .andExpect(status().isOk())
                // make sure it has been changed
                .andExpect(jsonPath("$.groupName").value("Updated Name"));
    }

    // successfully start voting phase
    @Test
    void startVotingPhase_validRequest_returnsOk() throws Exception {
        // send request
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-voting", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Voting phase started."));
    }

    // same for results phase
    @Test
    void showResultsPhase_validRequest_returnsOk() throws Exception {
        testGroup.setPhase(Group.GroupPhase.VOTING);
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/show-results", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Results phase started."));
    }


    // successfully remvoe member of the group
    @Test
    void removeMember_validRequest_returnsNoContent() throws Exception {
        // add one more member
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}/members/{memberId}", 
                testGroup.getGroupId(), user2.getUserId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNoContent());

        // get the group from the database
        Group updatedGroup = groupRepository.findById(testGroup.getGroupId()).orElse(null);
        // check that user2 is no longer a member of the group
        assertNotNull(updatedGroup);
        assertFalse(updatedGroup.getMembers().contains(user2), "User2 should have been removed from the group.");
    }


    // succesfully set pool timer
    @Test
    void setPoolPhaseDuration_validRequest_returnsOk() throws Exception {
        // send request
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/pool-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("300"))
                .andExpect(status().isOk());
        // make sure that duration was updated
        Group updatedGroup = groupRepository.findById(testGroup.getGroupId()).orElse(null);
        assertNotNull(updatedGroup.getPoolPhaseDuration());
    }

    // same for voting timer
    @Test
    void setVotingPhaseDuration_validRequest_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/voting-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("300"))
                .andExpect(status().isOk());
        Group updatedGroup = groupRepository.findById(testGroup.getGroupId()).orElse(null);
        assertNotNull(updatedGroup.getVotingPhaseDuration());
    }

    // succesfully returns remaining time of current phase
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

    // try to create group without username etc
    @Test
    void createGroup_missingName_returnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/groups")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // user2 who is not member of testgroup tries to get info about the group - fails
    @Test
    void getGroup_notMember_returnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    // deletion of the group fails because user is not owner
    @Test
    void deleteGroup_notOwner_returnsForbidden() throws Exception {
        // add user2 to members
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);
        // send request with user2 token (not owner)
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    // remove of member fails because user is not owner
    @Test
    void removeMember_notOwner_returnsForbidden() throws Exception {
        // add user2 to members 
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);
        // send request by not owner
        mockMvc.perform(MockMvcRequestBuilders.delete("/groups/{groupId}/members/{memberId}", 
                testGroup.getGroupId(), user1.getUserId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }
    // start of pool phase fails because user is not onwer
    @Test
    void setPoolPhaseDuration_notOwner_returnsForbidden() throws Exception {
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);

        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/pool-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("300"))
                .andExpect(status().isForbidden());
    }
    // start of voting phase fails because user is not owner
    @Test
    void startVotingPhase_notOwner_returnsForbidden() throws Exception {
        // add user2
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);

        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-voting", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    // add movie to moviepool failes because user is not member of the group
    @Test
    void addMovieToGroupPool_notMember_returnsForbidden() throws Exception {
        // create mock movie
        Movie testMovie = new Movie();
        testMovie.setMovieId(1L);
        movieRepository.save(testMovie);
        // send request from user2
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/pool/{movieId}", 
                testGroup.getGroupId(), testMovie.getMovieId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }


    // successfully start timer for pool phase
    @Test
    void startPoolTimer_validRequest_returnsOk() throws Exception {
        // set duration
        testGroup.setPoolPhaseDuration(300);
        groupRepository.save(testGroup);
        
        // send request
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-pool-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());
        
        // make sure that timer was started
        Group updatedGroup = groupRepository.findById(testGroup.getGroupId()).orElseThrow();
        assertNotNull(updatedGroup.getPhaseStartTime());
    }

    // start of timer fails, because no duration was set previosly
    @Test
    void startPoolTimer_noDurationSet_returnsBadRequest() throws Exception {
        // send request, but no duration was set 
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-pool-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest());
    }

    // start of timer fails, because user is not creator of the group
    @Test
    void startPoolTimer_notOwner_returnsForbidden() throws Exception {
        testGroup.setPoolPhaseDuration(300);
        groupRepository.save(testGroup);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-pool-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    // start of timer fails, because phase is not POOl
    @Test
    void startPoolTimer_wrongPhase_returnsConflict() throws Exception {
        testGroup.setPoolPhaseDuration(300);
        // set phase to voting as an example
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.save(testGroup);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-pool-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isConflict());
    }
    // exactly same four tets for voting timer
    @Test
    void startVotingTimer_validRequest_returnsOk() throws Exception {
        // set a duration and switch to voting phase
        testGroup.setVotingPhaseDuration(60);
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.save(testGroup);
        
        // send request
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-voting-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());
        
        // verify timer was started
        Group updatedGroup = groupRepository.findById(testGroup.getGroupId()).orElseThrow();
        assertNotNull(updatedGroup.getPhaseStartTime(), "Timer should have been started");
    }

    @Test
    void startVotingTimer_noDurationSet_returnsBadRequest() throws Exception {
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.save(testGroup);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-voting-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startVotingTimer_notOwner_returnsForbidden() throws Exception {
        testGroup.setVotingPhaseDuration(60);
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.save(testGroup);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-voting-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void startVotingTimer_wrongPhase_returnsConflict() throws Exception {
        testGroup.setVotingPhaseDuration(300);
        // set phase to results
        testGroup.setPhase(Group.GroupPhase.RESULTS);
        groupRepository.save(testGroup);
        
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/start-voting-timer", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isConflict());
    }

    // successfully retrive moviepool
    @Test
    void getGroupMoviePool_returnsMoviePool() throws Exception {
        // create mock movie
        Movie testMovie = new Movie();
        testMovie.setMovieId(1L);
        testMovie.setTitle("Test Movie");
        testMovie.setYear(2025);
        testMovie.setGenres(new ArrayList<>());
        testMovie.setActors(new ArrayList<>());
        testMovie.setDirectors(new ArrayList<>());
        testMovie.setOriginallanguage("rus");
        testMovie.setTrailerURL("http://test.com/trailer");
        testMovie.setPosterURL("http://test.com/poster");
        testMovie.setDescription("Test description");
        testMovie = movieRepository.saveAndFlush(testMovie);

        // add movie to group pool
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/pool/{movieId}", 
                testGroup.getGroupId(), testMovie.getMovieId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // get movie pool
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/pool", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                // moviepool should have only 1 movie - test movie added by user1
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].movie.movieId").value(testMovie.getMovieId()))
                .andExpect(jsonPath("$[0].movie.title").value(testMovie.getTitle()))
                .andExpect(jsonPath("$[0].addedBy").value(user1.getUserId()));
    }

    // succesfully retrieve rankings and pool (that's what the endpoints does)
    @Test
    void getVoteState_returnsPoolAndRankings() throws Exception {
        // create movie
        Movie testMovie = new Movie();
        testMovie.setMovieId(1L);
        testMovie.setTitle("Test Movie");
        testMovie = movieRepository.saveAndFlush(testMovie);

        // add movie to the pool
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{groupId}/pool/{movieId}", 
                testGroup.getGroupId(), testMovie.getMovieId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // swritch phase to voting 
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.save(testGroup);

        // get vote state
        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/vote-state", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                // movie pool should have only 1 movie and currently no rankings
                .andExpect(jsonPath("$.pool", hasSize(1)))
                .andExpect(jsonPath("$.pool[0].movieId").value(testMovie.getMovieId()))
                .andExpect(jsonPath("$.rankings", hasSize(0)));
    }

    // succesfully get voting statuses
    @Test
    void getVotingStatus_returnsVotingStatus() throws Exception {
        // add second user to group
        testGroup.getMembers().add(user2);
        groupRepository.save(testGroup);

        // swithc to the voting phase
        testGroup.setPhase(Group.GroupPhase.VOTING);
        groupRepository.save(testGroup);

        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/voting-status", testGroup.getGroupId())
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                // there should be 2 users and both haven't voted yet
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(user1.getUserId()))
                .andExpect(jsonPath("$[0].hasVoted").value(false))
                .andExpect(jsonPath("$[1].userId").value(user2.getUserId()))
                .andExpect(jsonPath("$[1].hasVoted").value(false));
    }

    // negative cases
    // fail to get pool, because user is not member of the group
    @Test
    void getGroupMoviePool_notMember_returnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/pool", testGroup.getGroupId())
        // use user2 token
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    // fail to get voting state, because user is not member of the group
    @Test
    void getVoteState_notMember_returnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/vote-state", testGroup.getGroupId())
        // use user 2 token
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    // fail to get voting status, because user is not member of the group
    @Test
    void getVotingStatus_notMember_returnsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/groups/{groupId}/voting-status", testGroup.getGroupId())
        // use user 2 token
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }
}