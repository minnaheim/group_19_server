package ch.uzh.ifi.hase.soprafs25.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.UserService;
import ch.uzh.ifi.hase.soprafs25.utils.AuthorizationUtil;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

    @GetMapping("/users/all")
    @ResponseStatus(HttpStatus.OK)
    public List<UserGetDTO> getAllUsers(@RequestHeader("Authorization") String token) {
        // Extract and validate token
        token = AuthorizationUtil.extractToken(token);
        userService.getUserByToken(token); // Verify user is authenticated

        // Fetch all users
        List<User> users = userService.getUsers();

        // Convert to DTO and return
        return DTOMapper.INSTANCE.convertEntityListToUserGetDTOList(users);
    }

// registration

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // create user
    User createdUser = userService.createUser(userInput);
    // convert internal representation of user back to API
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }

// login

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
    User user = userService.loginUser(userPostDTO.getUsername(), userPostDTO.getPassword());
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }
  
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.OK)
  public void logoutUser(@RequestHeader("Authorization") String token) {
    // just for safety
    token = AuthorizationUtil.extractToken(token);
    User user = userService.getUserByToken(token);
    userService.logoutUser(token);
  }
  
  @GetMapping("/check/username")
  @ResponseStatus(HttpStatus.OK)
  public boolean checkUsernameAvailability(@RequestParam String username) {
    return userService.isUsernameAvailable(username);
  }

  @GetMapping("/check/email")
  @ResponseStatus(HttpStatus.OK)
  public boolean checkEmailAvailability(@RequestParam String email) {
    return userService.isEmailAvailable(email);
  }

  /**
   * GET /users/{userId}/profile - Get user profile with complete data including watchlist
   */
  @GetMapping("/users/{userId}/profile")
  @ResponseStatus(HttpStatus.OK)
  @Transactional(readOnly = true)
  public UserGetDTO getUserProfile(@PathVariable("userId") Long userId, @RequestHeader("Authorization") String token) {
    token = AuthorizationUtil.extractToken(token);
    User user = userService.getUserByToken(token);

    if (!user.getUserId().equals(userId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this profile");
    }
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }
  
  /**
   * PUT /users/{userId}/profile - Update user profile
   */
  @PutMapping("/users/{userId}/profile")
  @ResponseStatus(HttpStatus.OK)
  @Transactional
  public UserGetDTO updateUserProfile(
        @PathVariable("userId") Long userId,
        @RequestBody UserPostDTO userPostDTO,
        @RequestParam(required = false) String token,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    
    // Extract token from Authorization header if present
    String effectiveToken = token;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        effectiveToken = authHeader.substring(7);
    }
    
    // Check if the token matches the user
    userService.checkUserToken(userId, effectiveToken);
    
    // Convert DTO to entity
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
    
    // Update the user
    User updatedUser = userService.updateUser(userId, userInput);
    
    // Return updated user data
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
  }


  @GetMapping("/users/search")
  @ResponseStatus(HttpStatus.OK)
  public List<UserGetDTO> searchUsersByUsername(@RequestParam(required = false) String username, @RequestHeader("Authorization") String token) {
      token = AuthorizationUtil.extractToken(token);
      userService.getUserByToken(token);
      List<User> users = userService.searchUsersByUsername(username);
      return DTOMapper.INSTANCE.convertEntityListToUserGetDTOList(users);
  }

}
