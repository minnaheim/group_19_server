package ch.uzh.ifi.hase.soprafs25.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

  @Qualifier("userRepository")
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @BeforeEach
  public void setup() {
    userRepository.deleteAll();
  }

  @Test
  public void createUser_validInputs_success() {
    // given
    assertNull(userRepository.findByUsername("testUsername"));

    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");

    // when
    User createdUser = userService.createUser(testUser);

    // then
    assertNotNull(createdUser.getUserId());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertEquals(testUser.getPassword(), createdUser.getPassword());
    assertEquals(testUser.getEmail(), createdUser.getEmail());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateUsername_throwsException() {
    // Create first user
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");
    userService.createUser(testUser);

    // Attempt to create second user with same username
    User testUser2 = new User();
    testUser2.setUsername("testUsername");
    testUser2.setPassword("password2");
    testUser2.setEmail("test2@example.com");

    // Check that an error is thrown
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.createUser(testUser2));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  public void createUser_duplicateEmail_throwsException() {
    // Create first user
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");
    userService.createUser(testUser);

    // Attempt to create second user with same email
    User testUser2 = new User();
    testUser2.setUsername("testUsername2");
    testUser2.setPassword("password2");
    testUser2.setEmail("test@example.com");

    // Check that an error is thrown
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.createUser(testUser2));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  public void loginUser_validCredentials_success() {
    // Setup - create a user
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");
    userService.createUser(testUser);

    // Execute - set status to offline and remove token to simulate logged out state
    User savedUser = userRepository.findByUsername("testUsername");
    savedUser.setStatus(UserStatus.OFFLINE);
    savedUser.setToken(null);
    userRepository.save(savedUser);

    // Login
    User loggedInUser = userService.loginUser("testUsername", "password");

    // Verify
    assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
    assertNotNull(loggedInUser.getToken());
  }

  @Test
  public void loginUser_invalidUsername_throwsException() {
    // Attempt to login with non-existent username
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.loginUser("nonExistentUser", "password"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }

  @Test
  public void loginUser_invalidPassword_throwsException() {
    // Setup - create a user
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");
    userService.createUser(testUser);

    // Attempt to login with wrong password
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.loginUser("testUsername", "wrongPassword"));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }

  @Test
  public void logoutUser_validToken_userLoggedOut() {
    // Setup - create a user
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");
    User createdUser = userService.createUser(testUser);

    String token = createdUser.getToken();

    // Execute
    userService.logoutUser(token);

    // Verify
    User loggedOutUser = userRepository.findByUsername("testUsername");
    assertEquals(UserStatus.OFFLINE, loggedOutUser.getStatus());
    assertNull(loggedOutUser.getToken());
  }

  @Test
  public void logoutUser_invalidToken_throwsException() {
    // Attempt to logout with invalid token
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.logoutUser("invalidToken"));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }

  @Test
  public void getUserByToken_validToken_returnsUser() {
    // Setup - create a user
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");
    User createdUser = userService.createUser(testUser);

    String token = createdUser.getToken();

    // Execute
    User foundUser = userService.getUserByToken(token);

    // Verify
    assertNotNull(foundUser);
    assertEquals("testUsername", foundUser.getUsername());
  }

  @Test
  public void getUserByToken_invalidToken_throwsException() {
    // Attempt to get user with invalid token
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.getUserByToken("invalidToken"));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }

  @Test
  public void isUsernameAvailable_availableUsername_returnsTrue() {
    // Check availability of non-existent username
    boolean isAvailable = userService.isUsernameAvailable("newUsername");

    assertTrue(isAvailable);
  }

  @Test
  public void isUsernameAvailable_takenUsername_returnsFalse() {
    // Setup - create a user
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");
    userService.createUser(testUser);

    // Check availability of existing username
    boolean isAvailable = userService.isUsernameAvailable("testUsername");

    assertEquals(false, isAvailable);
  }

  @Test
  public void isEmailAvailable_availableEmail_returnsTrue() {
    // Check availability of non-existent email
    boolean isAvailable = userService.isEmailAvailable("new@example.com");

    assertTrue(isAvailable);
  }

  @Test
  public void isEmailAvailable_takenEmail_returnsFalse() {
    // Setup - create a user
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("password");
    testUser.setEmail("test@example.com");
    userService.createUser(testUser);

    // Check availability of existing email
    boolean isAvailable = userService.isEmailAvailable("test@example.com");

    assertEquals(false, isAvailable);
  }
}
