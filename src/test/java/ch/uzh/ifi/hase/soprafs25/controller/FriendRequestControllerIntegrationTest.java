package ch.uzh.ifi.hase.soprafs25.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class FriendRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private User user1;
    private User user2;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        friendRequestRepository.deleteAll();

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
    }

    @Test
    void sendFriendRequest_validRequest_returnsCreated() throws Exception {
        mockMvc.perform(post("/friends/add/{receiverId}", user2.getUserId())
                .header("Authorization", "Bearer " + user1Token)) 
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sender.userId").value(user1.getUserId()))
            .andExpect(jsonPath("$.receiver.userId").value(user2.getUserId()));
    }

    @Test
    void sendFriendRequest_selfRequest_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/friends/add/{receiverId}", user1.getUserId())
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isBadRequest());
    }

    @Test
    void acceptFriendRequest_validRequest_returnsAcceptedRequest() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setSender(user1);
        request.setReceiver(user2);
        request.setAccepted(false); 
        request = friendRequestRepository.saveAndFlush(request);
        
        mockMvc.perform(post("/friends/friendrequest/{requestId}/accept", request.getRequestId())
                .header("Authorization", "Bearer " + user2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accepted").value(true)); 
    }

    @Test
    void rejectFriendRequest_validRequest_returnsRejectedRequest() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setSender(user1);
        request.setReceiver(user2);
        request = friendRequestRepository.saveAndFlush(request);

        mockMvc.perform(post("/friends/friendrequest/{requestId}/reject", request.getRequestId())
                .header("Authorization", "Bearer " + user2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accepted").value(false));
    }

    @Test
    void getSentFriendRequests_returnsPendingRequests() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setSender(user1);
        request.setReceiver(user2);
        friendRequestRepository.saveAndFlush(request);

        user1.getSentFriendRequests().add(request);
        user2.getReceivedFriendRequests().add(request);
        userRepository.save(user1);
        userRepository.save(user2);

        mockMvc.perform(get("/friends/friendrequests/sent")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sender.userId").value(user1.getUserId()));
    }

    @Test
    void getReceivedFriendRequests_returnsPendingRequests() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setSender(user1);
        request.setReceiver(user2);
        friendRequestRepository.save(request);
        user1.getSentFriendRequests().add(request);
        user2.getReceivedFriendRequests().add(request);
        userRepository.save(user1);
        userRepository.save(user2);

        mockMvc.perform(get("/friends/friendrequests/received")
                .header("Authorization", "Bearer " + user2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].receiver.userId").value(user2.getUserId()));
    }

    @Test
    void deleteFriendRequest_validRequest_returnsNoContent() throws Exception {
        FriendRequest request = new FriendRequest();
        request.setSender(user1);
        request.setReceiver(user2);
        request = friendRequestRepository.save(request);

        mockMvc.perform(delete("/friends/friendrequest/{requestId}", request.getRequestId())
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isNoContent());
    }

    @Test
    void removeFriend_validRequest_returnsNoContent() throws Exception {
        user1.getFriends().add(user2);
        user2.getFriends().add(user1);
        userRepository.saveAll(List.of(user1, user2));

        mockMvc.perform(delete("/friends/remove/{friendId}", user2.getUserId())
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isNoContent());
    }

    // succesfully retrieve friends list
    @Test
    void getFriends_returnsFriendsList() throws Exception {
        // add user2 as a friend
        user1.getFriends().add(user2);
        user2.getFriends().add(user1);
        userRepository.saveAll(List.of(user1, user2));
        // send request
        mockMvc.perform(get("/friends")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            // user1 has only 1 friend - user2
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].userId").value(user2.getUserId()))
            .andExpect(jsonPath("$[0].username").value(user2.getUsername()));
    }

    // in case there are no friends - empty list should be returned
    @Test
    void getFriends_noFriends_returnsEmptyList() throws Exception {
        // user1 currently has no friends
        // send request
        mockMvc.perform(get("/friends")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            // user1 has no friends - empty list
            .andExpect(jsonPath("$", hasSize(0)));
    }
}