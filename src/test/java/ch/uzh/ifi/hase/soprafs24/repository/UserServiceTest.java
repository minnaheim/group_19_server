
package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    public void createUser_validInputs_success() {
        // given
        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setStatus(UserStatus.OFFLINE);

        // Mock repository behavior
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0, User.class);
            savedUser.setUserId(1L);
            return savedUser;
        });
        when(userRepository.findByUsername(anyString())).thenReturn(null);
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        // when
        User createdUser = userService.createUser(testUser);

        // then
        assertNotNull(createdUser.getUserId());
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertEquals(testUser.getPassword(), createdUser.getPassword());
        assertEquals(testUser.getEmail(), createdUser.getEmail());
        assertNotNull(createdUser.getToken());
        assertEquals(UserStatus.ONLINE, createdUser.getStatus());
        
        // Verify repository was called
        Mockito.verify(userRepository).save(any(User.class));
    }
    
    @Test
    public void loginUser_validCredentials_success() {
        // given
        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setStatus(UserStatus.OFFLINE);
        testUser.setToken(null);
        
        // Mock repository behavior
        when(userRepository.findByUsername("testUsername")).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // when
        User loggedInUser = userService.loginUser("testUsername", "password");
        
        // then
        assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
        assertNotNull(loggedInUser.getToken());
        Mockito.verify(userRepository).save(any(User.class));
    }
    
    @Test
    public void loginUser_invalidUsername_throwsException() {
        // Mock repository behavior
        when(userRepository.findByUsername("nonExistentUser")).thenReturn(null);
        
        // then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> userService.loginUser("nonExistentUser", "password"));
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
    
    @Test
    public void loginUser_invalidPassword_throwsException() {
        // given
        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("correctPassword");
        
        // Mock repository behavior
        when(userRepository.findByUsername("testUsername")).thenReturn(testUser);
        
        // then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> userService.loginUser("testUsername", "wrongPassword"));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }
    
    @Test
    public void logoutUser_validToken_success() {
        // given
        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("validToken");
        
        // Mock repository behavior
        when(userRepository.findByToken("validToken")).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // when
        userService.logoutUser("validToken");
        
        // then
        assertEquals(UserStatus.OFFLINE, testUser.getStatus());
        assertNull(testUser.getToken());
        Mockito.verify(userRepository).save(testUser);
    }
    
    @Test
    public void logoutUser_invalidToken_throwsException() {
        // Mock repository behavior
        when(userRepository.findByToken("invalidToken")).thenReturn(null);
        
        // then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> userService.logoutUser("invalidToken"));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }
    
    @Test
    public void getUserByToken_validToken_returnsUser() {
        // given
        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setToken("validToken");
        
        // Mock repository behavior
        when(userRepository.findByToken("validToken")).thenReturn(testUser);
        
        // when
        User foundUser = userService.getUserByToken("validToken");
        
        // then
        assertEquals(testUser, foundUser);
    }
    
    @Test
    public void getUserByToken_invalidToken_throwsException() {
        // Mock repository behavior
        when(userRepository.findByToken("invalidToken")).thenReturn(null);
        
        // then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> userService.getUserByToken("invalidToken"));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }
    
    @Test
    public void isUsernameAvailable_usernameAvailable_returnsTrue() {
        // Mock repository behavior
        when(userRepository.findByUsername("availableUsername")).thenReturn(null);
        
        // when & then
        assertTrue(userService.isUsernameAvailable("availableUsername"));
    }
    
    @Test
    public void isUsernameAvailable_usernameTaken_returnsFalse() {
        // given
        User testUser = new User();
        testUser.setUsername("takenUsername");
        
        // Mock repository behavior
        when(userRepository.findByUsername("takenUsername")).thenReturn(testUser);
        
        // when & then
        assertFalse(userService.isUsernameAvailable("takenUsername"));
    }
    
    @Test
    public void isEmailAvailable_emailAvailable_returnsTrue() {
        // Mock repository behavior
        when(userRepository.findByEmail("available@example.com")).thenReturn(null);
        
        // when & then
        assertTrue(userService.isEmailAvailable("available@example.com"));
    }
    
    @Test
    public void isEmailAvailable_emailTaken_returnsFalse() {
        // given
        User testUser = new User();
        testUser.setEmail("taken@example.com");
        
        // Mock repository behavior
        when(userRepository.findByEmail("taken@example.com")).thenReturn(testUser);
        
        // when & then
        assertFalse(userService.isEmailAvailable("taken@example.com"));
    }
}