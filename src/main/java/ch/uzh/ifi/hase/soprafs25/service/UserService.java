package ch.uzh.ifi.hase.soprafs25.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  private final MovieRepository movieRepository;

  private final MoviePersistenceService moviePersistenceService;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository,
                     @Qualifier("movieRepository") MovieRepository movieRepository,
                     MoviePersistenceService moviePersistenceService) {
    this.userRepository = userRepository;
    this.movieRepository = movieRepository;
    this.moviePersistenceService = moviePersistenceService;
  }

  // for friends functionality
  @Autowired
  private FriendRequestRepository friendRequestRepository;


  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.ONLINE);
    checkIfUserExists(newUser);
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and email
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
    User userByEmail = userRepository.findByEmail(userToBeCreated.getEmail());

    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null && userByEmail != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format(baseErrorMessage, "username and email", "are"));
    } else if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
    } else if (userByEmail != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "email", "is"));
    }
  }
  
  public boolean isUsernameAvailable(String username) {
    return userRepository.findByUsername(username) == null;
  }
  
  public boolean isEmailAvailable(String email) {
    return userRepository.findByEmail(email) == null;
  }
  // login method
  public User loginUser(String username, String password) {

    User user = userRepository.findByUsername(username);
    if (user == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
    if (!user.getPassword().equals(password)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
    }
    user.setStatus(UserStatus.ONLINE);
    user.setToken(UUID.randomUUID().toString());
    return userRepository.save(user);      
  }

  // for user id identification by token
  // public Long getUserIdByToken(String token) {
  //   User user = userRepository.findByToken(token);
  //   if (user == null) {
  //       return null;
  //   }
  //   return user.getUserId();
  // }
  
  public User getUserByToken(String token) {
    User user = userRepository.findByToken(token);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    return user;
  }
  
  public User getUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }
  
  public void logoutUser(String token) {
    User user = userRepository.findByToken(token);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    user.setStatus(UserStatus.OFFLINE);
    user.setToken(null);
    userRepository.save(user);
  }

  
  /**
   * Checks if the given token belongs to the specified user
   */
  public void checkUserToken(Long userId, String token) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    
    // Check if the token is valid for this user
    if (user.getToken() == null || !user.getToken().equals(token)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token for this user");
    }
  }
  
  /**
   * Updates a user's information
   */
  public User updateUser(Long userId, User updatedUser) {
    User existingUser = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    
    // Update fields if provided
    if (updatedUser.getUsername() != null) {
      // Check if the new username is available (unless it's the same as the current one)
      if (!existingUser.getUsername().equals(updatedUser.getUsername()) && 
          userRepository.findByUsername(updatedUser.getUsername()) != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
      }
      existingUser.setUsername(updatedUser.getUsername());
    }
    
    if (updatedUser.getEmail() != null) {
      // Check if the new email is available (unless it's the same as the current one)
      if (!existingUser.getEmail().equals(updatedUser.getEmail()) && 
          userRepository.findByEmail(updatedUser.getEmail()) != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already taken");
      }
      existingUser.setEmail(updatedUser.getEmail());
    }
    
    if (updatedUser.getPassword() != null) {
      existingUser.setPassword(updatedUser.getPassword());
    }
    
    if (updatedUser.getBio() != null) {
      existingUser.setBio(updatedUser.getBio());
    }

    if (updatedUser.getFavoriteMovie() != null) {
      long newMovieId = updatedUser.getFavoriteMovie().getMovieId();
      long oldMovieId = (existingUser.getFavoriteMovie() != null)
          ? existingUser.getFavoriteMovie().getMovieId() : 0L;
      if (newMovieId != oldMovieId) {
        if (newMovieId != 0) {
          // Persist or retrieve the new favorite movie
          Movie managedMovie = moviePersistenceService.saveOrGet(updatedUser.getFavoriteMovie());
          existingUser.setFavoriteMovie(managedMovie);
        } else {
          // Clear favorite movie
          existingUser.setFavoriteMovie(null);
        }
      }
    }
    
    // Save and return the updated user, initializing lazy collections
    User savedUser = userRepository.save(existingUser);
    if (savedUser.getFavoriteMovie() != null) {
        // initialize genres to avoid LazyInitializationException
        savedUser.getFavoriteMovie().getGenres().size();
    }
    return savedUser;
  }

  public User searchUserByUsername(String username) {
    // in case nothing is there
    if (username == null || username.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
    User user = userRepository.findByUsername(username);
    if (user == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
    return user;
  }
}
