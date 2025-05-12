package ch.uzh.ifi.hase.soprafs25.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private String testToken;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        // create test user
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@mail.com");
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ONLINE);
        testToken = "testToken";
        testUser.setToken(testToken);
        testUser = userRepository.saveAndFlush(testUser);
    }

    // successful registration of user
    @Test
    void registerUser_validInput_returnsCreated() throws Exception {
        // send the request with some mock data
        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newUser\",\"email\":\"new@mail.com\",\"password\":\"password\"}"))
                .andExpect(status().isCreated())
                // check name and status
                .andExpect(jsonPath("$.username").value("newUser"))
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    // registration fails because such name already exists
    @Test
    void registerUser_duplicateUsername_returnsBadRequest() throws Exception {
        // send the request
        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                // username is the same as in the setup - badrequest should be returned
                .content("{\"username\":\"testUser\",\"email\":\"new@mail.com\",\"password\":\"password\"}"))
                .andExpect(status().isBadRequest());
    }

    // successful login
    @Test
    void loginUser_validCredentials_returnsOk() throws Exception {
        // send request with mock data
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testUser\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                // check that returned kbject has same username as was provided
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    // login fails because of wrong password
    @Test
    void loginUser_invalidCredentials_returnsUnauthorized() throws Exception {
        // send request with wrong password
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testUser\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    // successful logout
    @Test
    void logoutUser_validToken_returnsOk() throws Exception {
        // send request
        mockMvc.perform(MockMvcRequestBuilders.post("/logout")
                .header("Authorization", "Bearer "+testToken))
                .andExpect(status().isOk());

        // check that user still exists, but status has been changed to offline
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertEquals(UserStatus.OFFLINE, updatedUser.getStatus());
    }


    // check for available userna,e
    @Test
    void checkUsernameAvailability_available_returnsTrue() throws Exception {
        // send request with name which differs from our single testuser
        mockMvc.perform(MockMvcRequestBuilders.get("/check/username")
                .param("username", "available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    // check for username which is already taken
    @Test
    void checkUsernameAvailability_taken_returnsFalse() throws Exception {
        // send request with username of existed testuser
        mockMvc.perform(MockMvcRequestBuilders.get("/check/username")
                .param("username", "testUser"))
                .andExpect(status().isOk())
                // gets false
                .andExpect(jsonPath("$").value(false));
    }

    // same tests for email check
    @Test
    void checkEmailAvailability_available_returnsTrue() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/check/email")
                .param("email", "available@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void checkEmailAvailability_taken_returnsFalse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/check/email")
                .param("email", "test@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    // successful fetch of user profile
    @Test
    void getUserProfile_authorized_returnsProfile() throws Exception {
        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/users/{userId}/profile", testUser.getUserId())
                .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                // make sure that user with the testuser name is returned
                .andExpect(jsonPath("$.username").value("testUser"));
    }

    // fetching of user profile fails because it's tried to be done by another user
    @Test
    void getUserProfile_unauthorized_returnsForbidden() throws Exception {
        // add second user 
        User testUser2 = new User();
        testUser2.setUsername("testUser2");
        testUser2.setEmail("test2@mail.com");
        testUser2.setPassword("password");
        testUser2.setStatus(UserStatus.ONLINE);
        String testToken2 = "testToken2";
        testUser2.setToken(testToken2);
        testUser2 = userRepository.saveAndFlush(testUser2);
        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/users/{userId}/profile", testUser2.getUserId())
                .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isForbidden());
    }

    // succesful update of the profile
    @Test
    void updateUserProfile_validInput_returnsUpdatedUser() throws Exception {
        // send request with some updated data
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}/profile", testUser.getUserId())
                .header("Authorization", "Bearer " + testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"updated\",\"email\":\"updated@mail.com\",\"bio\":\"new bio\"}"))
                .andExpect(status().isOk())
                // make sure that name and bio have been changed
                .andExpect(jsonPath("$.username").value("updated"))
                .andExpect(jsonPath("$.bio").value("new bio"));
    }

    // succesful search for a user
    @Test
    void searchUsersByUsername_returnsMatchingUsers() throws Exception {
        // add a second user 
        User testUser2 = new User();
        testUser2.setUsername("testUser2");
        testUser2.setEmail("test2@mail.com");
        testUser2.setPassword("password");
        testUser2.setStatus(UserStatus.ONLINE);
        String testToken2 = "testToken2";
        testUser2.setToken(testToken2);
        testUser2 = userRepository.saveAndFlush(testUser2);

        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/users/search")
        // looking for testUser2
                .param("username", "testUser2")
                .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                // user with this name is returned
                .andExpect(jsonPath("$[0].username").value("testUser2"));
    }


    @Test
    void getAllUsers_returnsAllUsers() throws Exception {
        // add a second user 
        User testUser2 = new User();
        testUser2.setUsername("testUser2");
        testUser2.setEmail("test2@mail.com");
        testUser2.setPassword("password");
        testUser2.setStatus(UserStatus.ONLINE);
        String testToken2 = "testToken2";
        testUser2.setToken(testToken2);
        testUser2 = userRepository.saveAndFlush(testUser2);

        // send request
        mockMvc.perform(MockMvcRequestBuilders.get("/users/all")
                .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                // bith users appear
                .andExpect(jsonPath("$[0].username").value("testUser"))
                .andExpect(jsonPath("$[1].username").value("testUser2"));
    }
}