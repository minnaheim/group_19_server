package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

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

  UserController(UserService userService) {
    this.userService = userService;
  }

  // @GetMapping("/users")
  // @ResponseStatus(HttpStatus.OK)
  // @ResponseBody
  // public List<UserGetDTO> getAllUsers() {
  //   // fetch all users in the internal representation
  //   List<User> users = userService.getUsers();
  //   List<UserGetDTO> userGetDTOs = new ArrayList<>();

  //   // convert each user to the API representation
  //   for (User user : users) {
  //     userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
  //   }
  //   return userGetDTOs;
  // }

// registration

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
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
  @ResponseBody
  public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
    User user = userService.loginUser(userPostDTO.getUsername(), userPostDTO.getPassword());
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }

  @GetMapping("/check/username")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public boolean checkUsernameAvailability(@RequestParam String username) {
    return userService.isUsernameAvailable(username);
  }

  @GetMapping("/check/email")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public boolean checkEmailAvailability(@RequestParam String email) {
    return userService.isEmailAvailable(email);
  }
}
