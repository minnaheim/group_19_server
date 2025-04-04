package ch.uzh.ifi.hase.soprafs25.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // given
    testUser = new User();
    testUser.setUserId(1L);
    testUser.setUsername("testUsername");
    testUser.setEmail("test@example.com");
    testUser.setPassword("password");
    testUser.setStatus(UserStatus.OFFLINE);

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    when(userRepository.save(any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    // Set up repository behavior
    when(userRepository.findByUsername(anyString())).thenReturn(null);
    when(userRepository.findByEmail(anyString())).thenReturn(null);

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // then
    verify(userRepository, times(1)).save(any());
    verify(userRepository).flush();

    assertEquals(testUser.getUserId(), createdUser.getUserId());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateUsername_throwsException() {
    // Set up repository to return an existing user with the same username
    when(userRepository.findByUsername(anyString())).thenReturn(testUser);
    when(userRepository.findByEmail(anyString())).thenReturn(null);

    // then -> attempt to create user with duplicate username -> check that an error is thrown
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.createUser(testUser));
    
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  public void createUser_duplicateEmail_throwsException() {
    // Set up repository to return an existing user with the same email
    when(userRepository.findByUsername(anyString())).thenReturn(null);
    when(userRepository.findByEmail(anyString())).thenReturn(testUser);

    // then -> attempt to create user with duplicate email -> check that an error is thrown
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.createUser(testUser));
    
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
  }

  @Test
  public void loginUser_validCredentials_success() {
    // Setup
    testUser.setToken(null);
    testUser.setStatus(UserStatus.OFFLINE);
    
    when(userRepository.findByUsername("testUsername")).thenReturn(testUser);
    
    // Execute
    User loggedInUser = userService.loginUser("testUsername", "password");
    
    // Verify
    assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
    assertNotNull(loggedInUser.getToken());
    verify(userRepository).save(testUser);
  }
  
  @Test
  public void loginUser_invalidUsername_throwsException() {
    // Setup
    when(userRepository.findByUsername("nonExistentUser")).thenReturn(null);
    
    // Execute & Verify
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.loginUser("nonExistentUser", "password"));
    
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }
  
  @Test
  public void loginUser_invalidPassword_throwsException() {
    // Setup
    when(userRepository.findByUsername("testUsername")).thenReturn(testUser);
    
    // Execute & Verify
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.loginUser("testUsername", "wrongPassword"));
    
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }
  
  @Test
  public void logoutUser_validToken_userLoggedOut() {
    // Setup
    testUser.setToken("validToken");
    testUser.setStatus(UserStatus.ONLINE);
    
    when(userRepository.findByToken("validToken")).thenReturn(testUser);
    
    // Execute
    userService.logoutUser("validToken");
    
    // Verify
    assertEquals(UserStatus.OFFLINE, testUser.getStatus());
    assertNull(testUser.getToken());
    verify(userRepository).save(testUser);
  }
  
  @Test
  public void logoutUser_invalidToken_throwsException() {
    // Setup
    when(userRepository.findByToken("invalidToken")).thenReturn(null);
    
    // Execute & Verify
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.logoutUser("invalidToken"));
    
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }
  
  @Test
  public void getUserByToken_validToken_returnsUser() {
    // Setup
    testUser.setToken("validToken");
    when(userRepository.findByToken("validToken")).thenReturn(testUser);
    
    // Execute
    User foundUser = userService.getUserByToken("validToken");
    
    // Verify
    assertEquals(testUser, foundUser);
  }
  
  @Test
  public void getUserByToken_invalidToken_throwsException() {
    // Setup
    when(userRepository.findByToken("invalidToken")).thenReturn(null);
    
    // Execute & Verify
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> userService.getUserByToken("invalidToken"));
    
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }
  
  @Test
  public void isUsernameAvailable_availableUsername_returnsTrue() {
    // Setup
    when(userRepository.findByUsername("newUsername")).thenReturn(null);
    
    // Execute & Verify
    assertEquals(true, userService.isUsernameAvailable("newUsername"));
  }
  
  @Test
  public void isUsernameAvailable_takenUsername_returnsFalse() {
    // Setup
    when(userRepository.findByUsername("takenUsername")).thenReturn(testUser);
    
    // Execute & Verify
    assertEquals(false, userService.isUsernameAvailable("takenUsername"));
  }
  
  @Test
  public void isEmailAvailable_availableEmail_returnsTrue() {
    // Setup
    when(userRepository.findByEmail("new@example.com")).thenReturn(null);
    
    // Execute & Verify
    assertEquals(true, userService.isEmailAvailable("new@example.com"));
  }
  
  @Test
  public void isEmailAvailable_takenEmail_returnsFalse() {
    // Setup
    when(userRepository.findByEmail("taken@example.com")).thenReturn(testUser);
    
    // Execute & Verify
    assertEquals(false, userService.isEmailAvailable("taken@example.com"));
  }
}
