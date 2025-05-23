package ch.uzh.ifi.hase.soprafs25.rest.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs25.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.GroupInvitation;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.MoviePool;
import ch.uzh.ifi.hase.soprafs25.entity.RankingResult;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.rest.dto.FriendRequestGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.FriendRequestPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupInvitationGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupInvitationPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MoviePoolGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MoviePoolPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieRankGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.RankingResultGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPostDTO;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for creating information (POST).
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DTOMapper {

    DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "password", target = "password")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "favoriteMovie", target = "favoriteMovie")
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "password", target = "password")
    @Mapping(source = "token", target = "token")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "favoriteGenres", target = "favoriteGenres")
    @Mapping(source = "favoriteActors", target = "favoriteActors")
    @Mapping(source = "favoriteDirectors", target = "favoriteDirectors")
    @Mapping(source = "favoriteMovie", target = "favoriteMovie")
    @Mapping(source = "watchlist", target = "watchlist")
    @Mapping(source = "watchedMovies", target = "watchedMovies")
    @Mapping(source = "status", target = "status")
    UserGetDTO convertEntityToUserGetDTO(User user);

    // for friends search
    List<UserGetDTO> convertEntityListToUserGetDTOList(List<User> users);

    @Mapping(source = "movieId", target = "movieId")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "genres", target = "genres")
    @Mapping(source = "year", target = "year")
    @Mapping(source = "actors", target = "actors")
    @Mapping(source = "directors", target = "directors")
    @Mapping(source = "spokenlanguages", target = "spokenlanguages")
    @Mapping(source = "originallanguage", target = "originallanguage")
    @Mapping(source = "trailerURL", target = "trailerURL")
    @Mapping(source = "posterURL", target = "posterURL")
    @Mapping(source = "description", target = "description")
    Movie convertMovieGetDTOtoEntity(MovieGetDTO movieGetDTO);

    @Mapping(source = "movieId", target = "movieId")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "genres", target = "genres")
    @Mapping(source = "year", target = "year")
    @Mapping(source = "actors", target = "actors")
    @Mapping(source = "directors", target = "directors")
    @Mapping(source = "spokenlanguages", target = "spokenlanguages")
    @Mapping(source = "originallanguage", target = "originallanguage")
    @Mapping(source = "posterURL", target = "posterURL")
    @Mapping(source = "trailerURL", target = "trailerURL")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "tmdbRating", target = "tmdbRating")
    MovieGetDTO convertEntityToMovieGetDTO(Movie movie);

    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "moviePool", ignore = true)
    @Mapping(source = "groupName", target = "groupName")
    Group convertGroupPostDTOtoEntity(GroupPostDTO groupPostDTO);

    @Mapping(source = "groupId", target = "groupId")
    @Mapping(source = "groupName", target = "groupName")
    @Mapping(source = "creator", target = "creator")
    @Mapping(source = "creator.userId", target = "creatorId")
    @Mapping(source = "members", target = "memberIds")
    @Mapping(source = "moviePool.movies", target = "movieIds")
    @Mapping(expression = "java(group.getPhase().name())", target = "phase")
    @Mapping(source = "poolPhaseDuration", target = "poolPhaseDuration")
    @Mapping(source = "votingPhaseDuration", target = "votingPhaseDuration")
    @Mapping(source = "phaseStartTime", target = "phaseStartTime")
    @Mapping(target = "remainingTime", ignore = true)
    GroupGetDTO convertEntityToGroupGetDTO(Group group);

    List<GroupGetDTO> convertEntityListToGroupGetDTOList(List<Group> groups);


    @Mapping(source = "movieId", target = "movieId")
    @Mapping(source = "title", target = "title")
    MovieRankGetDTO convertEntityToMovieRankGetDTO(Movie movie);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "winningMovie", target = "winningMovie")
    @Mapping(source = "averageRank", target = "averageRank")
    @Mapping(source = "calculationTimestamp", target = "calculationTimestamp")
    RankingResultGetDTO convertEntityToRankingResultGetDTO(RankingResult rankingResult);

    @Mapping(source = "requestId", target = "requestId")
    @Mapping(source = "sender", target = "sender")
    @Mapping(source = "receiver", target = "receiver")
    @Mapping(source = "creationTime", target = "creationTime")
    @Mapping(source = "accepted", target = "accepted")
    @Mapping(source = "responseTime", target = "responseTime")
    FriendRequestGetDTO convertEntityToFriendRequestGetDTO(FriendRequest friendRequest);

    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    @Mapping(target = "creationTime", ignore = true)
    @Mapping(target = "accepted", ignore = true)
    @Mapping(target = "responseTime", ignore = true)
    FriendRequest convertFriendRequestPostDTOtoEntity(FriendRequestPostDTO friendRequestPostDTO);

    @Mapping(source = "invitationId", target = "invitationId")
    @Mapping(source = "sender", target = "sender")
    @Mapping(source = "receiver", target = "receiver")
    @Mapping(source = "group", target = "group")
    @Mapping(source = "creationTime", target = "creationTime")
    @Mapping(source = "accepted", target = "accepted")
    @Mapping(source = "responseTime", target = "responseTime")
    GroupInvitationGetDTO convertEntityToGroupInvitationGetDTO(GroupInvitation groupInvitation);


    @Mapping(target = "invitationId", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "creationTime", ignore = true)
    @Mapping(target = "accepted", ignore = true)
    @Mapping(target = "responseTime", ignore = true)
    GroupInvitation convertGroupInvitationPostDTOtoEntity(GroupInvitationPostDTO groupInvitationPostDTO);

    @Mapping(source = "poolId", target = "poolId")
    @Mapping(source = "group.groupId", target = "groupId")
    @Mapping(source = "movies", target = "movies")
    @Mapping(source = "lastUpdated", target = "lastUpdated")
    MoviePoolGetDTO convertEntityToMoviePoolGetDTO(MoviePool moviePool);


    @Mapping(target = "poolId", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    MoviePool convertMoviePoolPostDTOtoEntity(MoviePoolPostDTO moviePoolPostDTO);

    default List<Long> mapUsersToIds(List<User> users) {
      if (users == null) return null;
      return users.stream()
                 .map(User::getUserId)
                 .collect(Collectors.toList());
    }

    default List<Long> mapMoviesToIds(List<Movie> movies) {
      if (movies == null) return null;
      return movies.stream()
                  .map(Movie::getMovieId)
                  .collect(Collectors.toList());
    }

    // for convenient transformation of list of movies to list of movieGetDTO
    default List<MovieGetDTO> convertEntityListToMovieGetDTOList(List<Movie> movies) {
      if (movies == null) return null;
      return movies.stream()
                  .map(this::convertEntityToMovieGetDTO)
                  .collect(Collectors.toList());
  }
}