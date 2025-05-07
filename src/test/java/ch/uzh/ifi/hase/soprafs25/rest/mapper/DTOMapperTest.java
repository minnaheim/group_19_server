package ch.uzh.ifi.hase.soprafs25.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupGetDTO;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {
  @Test
  public void testCreateUser_fromUserPostDTO_toUser_success() {
    // create UserPostDTO
    UserPostDTO userPostDTO = new UserPostDTO();
    // userPostDTO.setName("name");
    userPostDTO.setUsername("username");

    // MAP -> Create user
    User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // check content
    // assertEquals(userPostDTO.getName(), user.getName());
    assertEquals(userPostDTO.getUsername(), user.getUsername());
  }

  @Test
  public void testGetUser_fromUser_toUserGetDTO_success() {
    // create User
    User user = new User();
    // user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);
    user.setToken("1");

    // MAP -> Create UserGetDTO
    UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

    // check content
    assertEquals(user.getUserId(), userGetDTO.getUserId());
    // assertEquals(user.getName(), userGetDTO.getName());
    assertEquals(user.getUsername(), userGetDTO.getUsername());
    assertEquals(user.getStatus(), userGetDTO.getStatus());
  }

  @Test
  public void testGetGroup_fromGroup_toGroupGetDTO_includesPhase() {
    // create Group
    Group group = new Group();
    group.setGroupId(42L);
    group.setGroupName("Test Group");
    group.setPhase(Group.GroupPhase.VOTING);
    // MAP -> Create GroupGetDTO
    GroupGetDTO groupGetDTO = DTOMapper.INSTANCE.convertEntityToGroupGetDTO(group);
    // check content
    assertEquals(group.getGroupId(), groupGetDTO.getGroupId());
    assertEquals(group.getGroupName(), groupGetDTO.getGroupName());
    assertEquals(group.getPhase().name(), groupGetDTO.getPhase());
  }

  @Test
  public void testMapMoviesToIds_success() {
    // Create a list of movies
    List<Movie> movies = new ArrayList<>();

    Movie movie1 = new Movie();
    movie1.setMovieId(1L);
    movie1.setTitle("Test Movie 1");

    Movie movie2 = new Movie();
    movie2.setMovieId(2L);
    movie2.setTitle("Test Movie 2");

    movies.add(movie1);
    movies.add(movie2);

    // Map the movies to IDs
    List<Long> movieIds = DTOMapper.INSTANCE.mapMoviesToIds(movies);

    // Check content
    assertEquals(2, movieIds.size());
    assertEquals(1L, movieIds.get(0));
    assertEquals(2L, movieIds.get(1));
  }

  @Test
  public void testMapMoviesToIds_emptyList() {
    // Create an empty list of movies
    List<Movie> movies = new ArrayList<>();

    // Map the movies to IDs
    List<Long> movieIds = DTOMapper.INSTANCE.mapMoviesToIds(movies);

    // Check content
    assertEquals(0, movieIds.size());
  }

  @Test
  public void testMapMoviesToIds_nullList() {
    // Map null to IDs
    List<Long> movieIds = DTOMapper.INSTANCE.mapMoviesToIds(null);

    // Check content
    assertNotNull(movieIds); // Should not be null
    assertTrue(movieIds.isEmpty()); // Should be an empty list
  }

}
