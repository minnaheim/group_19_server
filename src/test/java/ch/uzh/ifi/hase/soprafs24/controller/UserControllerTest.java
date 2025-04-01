package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  /* )
  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    // user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        // .andExpect(jsonPath("$[0].name", is(user.getName())))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }
        */

 @Test
  public void createUser_validInput_userCreated() throws Exception {
      // given
      User user = new User();
      user.setUsername("testUsername");
      user.setEmail("test@example.com"); // Add required email
      user.setPassword("password"); // Add required password
      user.setToken("1");
      user.setStatus(UserStatus.ONLINE);

      UserPostDTO userPostDTO = new UserPostDTO();
      userPostDTO.setUsername("testUsername");
      userPostDTO.setEmail("test@example.com"); // Add email
      userPostDTO.setPassword("password"); // Add password

      given(userService.createUser(Mockito.any())).willReturn(user);

      // when/then -> do the request + validate the result
      MockHttpServletRequestBuilder postRequest = post("/register") // Changed endpoint
          .contentType(MediaType.APPLICATION_JSON)
          .content(asJsonString(userPostDTO));

      // then
      mockMvc.perform(postRequest)
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.username", is(user.getUsername())))
          .andExpect(jsonPath("$.status", is(user.getStatus().toString())))
          .andExpect(jsonPath("$.token", is(user.getToken()))); // Add token validation
  }

  @Test
  public void loginUser_validCredentials_userLoggedIn() throws Exception {
      // given
      User user = new User();
      user.setUsername("testUsername");
      user.setPassword("password");
      user.setEmail("test@example.com");
      user.setToken("token123");
      user.setStatus(UserStatus.ONLINE);

      UserPostDTO loginCredentials = new UserPostDTO();
      loginCredentials.setUsername("testUsername");
      loginCredentials.setPassword("password");

      given(userService.loginUser(anyString(), anyString())).willReturn(user);

      // when/then
      MockHttpServletRequestBuilder postRequest = post("/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(asJsonString(loginCredentials));

      mockMvc.perform(postRequest)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username", is(user.getUsername())))
          .andExpect(jsonPath("$.token", is(user.getToken())))
          .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  @Test
  public void logoutUser_validToken_userLoggedOut() throws Exception {
      // given
      String token = "token123";
      doNothing().when(userService).logoutUser(anyString());

      // when/then
      MockHttpServletRequestBuilder postRequest = post("/logout")
          .param("token", token);

      mockMvc.perform(postRequest)
          .andExpect(status().isOk());

      verify(userService).logoutUser(token);
  }

  @Test
  public void validateSession_validToken_returnsUser() throws Exception {
      // given
      User user = new User();
      user.setUsername("testUsername");
      user.setEmail("test@example.com");
      user.setToken("token123");
      user.setStatus(UserStatus.ONLINE);

      given(userService.getUserByToken(anyString())).willReturn(user);

      // when/then
      MockHttpServletRequestBuilder getRequest = get("/session")
          .param("token", "token123");

      mockMvc.perform(getRequest)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.username", is(user.getUsername())))
          .andExpect(jsonPath("$.token", is(user.getToken())));
  }

  @Test
  public void checkUsernameAvailability_usernameAvailable_returnsTrue() throws Exception {
      // given
      given(userService.isUsernameAvailable(anyString())).willReturn(true);

      // when/then
      MockHttpServletRequestBuilder getRequest = get("/check/username")
          .param("username", "newUsername");

      mockMvc.perform(getRequest)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", is(true)));
  }

  @Test
  public void checkEmailAvailability_emailAvailable_returnsTrue() throws Exception {
      // given
      given(userService.isEmailAvailable(anyString())).willReturn(true);

      // when/then
      MockHttpServletRequestBuilder getRequest = get("/check/email")
          .param("email", "new@example.com");

      mockMvc.perform(getRequest)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", is(true)));
  }

  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}