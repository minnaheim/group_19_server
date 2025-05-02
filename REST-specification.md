# REST API Specification

## Authentication
All authenticated API requests must include the Authorization header with a Bearer token:
```
Authorization: Bearer <token>
```
The token is obtained during login and is returned in the response headers.

## Error Responses
When an error occurs (e.g., invalid input, resource not found, server error), the API returns a standardized JSON response body with the appropriate HTTP status code.

**Format:**
```json
{
  "timestamp": 1678886400000, // Unix timestamp in milliseconds
  "status": 400,             // HTTP status code
  "error": "Bad Request",    // HTTP status phrase
  "message": "Specific error message detailing the issue.", // Developer-friendly error description
  "path": "/users/123/rankings" // The request path that caused the error
}
```

**Common Status Codes:**
- **400 Bad Request:** Invalid input, missing parameters, validation errors (e.g., invalid ranking data).
- **401 Unauthorized:** Missing or invalid authentication token.
- **403 Forbidden:** Authenticated user lacks permission to access the resource.
- **404 Not Found:** Resource not found (e.g., user ID, movie ID, group ID).
- **409 Conflict:** Action cannot be completed due to a conflict with the current state of the resource (e.g., username already exists).
- **500 Internal Server Error:** An unexpected server-side error occurred.



## Entities

### Movie Object

A `Movie object` is a JSON object with the following structure:

```json
{
  "movieId": integer,           // (e.g., 27205)
  "title": string,              // (e.g., "Inception")
  "genres": [string],           // (e.g., ["Action", "Science Fiction", "Adventure"])
  "year": integer,              // (e.g., 2010)
  "actors": [string],           // (e.g., ["Leonardo DiCaprio", "Joseph Gordon-Levitt", "Ken Watanabe", "Tom Hardy", "Elliot Page"])
  "directors": [string],        // (e.g., ["Christopher Nolan", "Emma Thomas"])
  "originallanguage": string,   // (e.g., "English")
  "trailerURL": string,         // (e.g., "https://www.youtube.com/watch?v=mpj9dL7swwk")
  "posterURL": string,          // (e.g., "https://image.tmdb.org/t/p/w500/ljsZTbVsrQSqZgWeep2B1QiDKuh.jpg")
  "description": string         // (e.g., "Cobb, a skilled thief who commits corporate espionage by infiltrating the subconscious of his targets is offered a chance to regain his old life as payment for a task considered to be impossible: 'inception', the implantation of another person's idea into a target's subconscious.")
}
```
**Notes:**
- Lists can be empty (genres, actors, directors) if information is not available in TMDb.
- Description strings may have "weird" format (i.e. escaped quotes like \"inception\")

### Actor Object

An `Actor object` is a JSON object with the following structure:

```json
{
  "actorId": integer,    // (e.g., 6193)
  "name": string         // (e.g., "Leonardo DiCaprio")
}
```

### Director Object

A `Director object` is a JSON object with the following structure:

```json
{
  "directorId": integer, // (e.g., 525)
  "name": string         // (e.g., "Christopher Nolan")
}
```

### Genres Object

A `Genres object` is a JSON object with the following structure:

```json
{
  "id": integer,         // (e.g., 28)
  "name": string         // (e.g., "Action")
}
```

### User Object

A `User object` is a JSON object with the following structure:

```json
{
  "userId": integer,
  "username": string,
  "email": string,
  "password": string,
  "bio": string,
  "favoriteGenres": [string],
  "favoriteMovie": Movie object,
  "favoriteActors": [string],
  "favoriteDirectors": [string],
  "watchlist": [Movie object],
  "watchedMovies": [Movie object]
}
```

### RankingSubmitDTO Object

A `RankingSubmitDTO object` is a JSON object with the following structure:

```json
{
  "movieId": integer,
  "rank": integer
}
```

### RankingResultGetDTO Object

A `RankingResultGetDTO object` is a JSON object with the following structure:

```json
{
  "calculationTimestamp": string, // ISO 8601 format
  "winningMovie": Movie object
}
```

### UserFavoritesGenresDTO Object

A `UserFavoritesGenresDTO object` is a JSON object with the following structure:

```json
{
  "genreIds": [string]
}
```

### UserFavoritesMovieDTO Object

A `UserFavoritesMovieDTO object` is a JSON object with the following structure:

```json
{
  "movieId": integer
}
```

### UserFavoritesActorsDTO Object

A `UserFavoritesActorsDTO object` is a JSON object with the following structure:

```json
{
  "actorIds": [string]
}
```

### UserFavoritesDirectorsDTO Object

A `UserFavoritesDirectorsDTO object` is a JSON object with the following structure:

```json
{
  "directorIds": [string]
}
```

#### Review Object

A `Review object` is a JSON object with the following structure:

```json
{
  "reviewId": integer,
  "movieId": integer,
  "userId": integer,
  "rating": integer,      // e.g., 1-5
  "comment": string,
  "timestamp": string     // ISO 8601 date-time
}
```



## API Endpoints
### User Management

| Endpoint | Method | Parameters | Parameter Location | Status Code | Response | Description |
|----------|--------|------------|-------------------|-------------|----------|-------------|
| `/register` | POST | userPostDTO\<UserPostDTO\> | Body | 201 Created | UserGetDTO | Register a new user |
| `/register` | POST | userPostDTO\<UserPostDTO\> | Body | 400 Bad Request | Error: reason\<string\> | Invalid input or missing fields |
| `/register` | POST | userPostDTO\<UserPostDTO\> | Body | 409 Conflict | Error: reason\<string\> | Username or email already exists |
| `/login` | POST | userPostDTO\<UserPostDTO\> | Body | 200 OK | UserGetDTO | User successfully logged in |
| `/login` | POST | userPostDTO\<UserPostDTO\> | Body | 400 Bad Request | Error: reason\<string\> | Invalid input or missing fields |
| `/login` | POST | userPostDTO\<UserPostDTO\> | Body | 404 Not Found | Error: reason\<string\> | User not found |
| `/login` | POST | userPostDTO\<UserPostDTO\> | Body | 401 Unauthorized | Error: reason\<string\> | Invalid password |
| `/logout` | POST | token\<string\> | Query | 200 OK | - | User logs out |
| `/logout` | POST | token\<string\> | Query | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/session` | GET | token\<string\> | Query | 200 OK | UserGetDTO | Validate session |
| `/session` | GET | token\<string\> | Query | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/session` | GET | token\<string\> | Query | 404 Not Found | Error: reason\<string\> | User not found |
| `/check/username` | GET | username\<string\> | Query | 200 OK | boolean | Check username availability |
| `/check/username` | GET | username\<string\> | Query | 400 Bad Request | Error: reason\<string\> | Invalid or missing username |
| `/check/email` | GET | email\<string\> | Query | 200 OK | boolean | Check email availability |
| `/check/email` | GET | email\<string\> | Query | 400 Bad Request | Error: reason\<string\> | Invalid or missing email |
| `/users/{userId}/profile` | GET | userId\<long\> | Path | 200 OK | UserGetDTO | Retrieve user profile |
| `/users/{userId}/profile` | GET | userId\<long\> | Path | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/profile` | PUT | userId\<long\>, userPostDTO\<UserPostDTO\>, token\<string\> or Authorization\<string\> | Path, Body, Query/Header | 200 OK | UserGetDTO | Update user profile |
| `/users/{userId}/profile` | PUT | userId\<long\>, userPostDTO\<UserPostDTO\>, token\<string\> or Authorization\<string\> | Path, Body, Query/Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/profile` | PUT | userId\<long\>, userPostDTO\<UserPostDTO\>, token\<string\> or Authorization\<string\> | Path, Body, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/profile` | PUT | userId\<long\>, userPostDTO\<UserPostDTO\>, token\<string\> or Authorization\<string\> | Path, Body, Query/Header | 403 Forbidden | Error: reason\<string\> | Unauthorized to update user |
| `/users/{userId}/profile` | PUT | userId\<long\>, userPostDTO\<UserPostDTO\>, token\<string\> or Authorization\<string\> | Path, Body, Query/Header | 409 Conflict | Error: reason\<string\> | Username or email already taken |
| `/users/search` | GET | username\<string\>, Authorization\<string\> | Query, Header | 200 OK | List\<User\> | Search users by username |
| `/users/search` | GET | username\<string\>, Authorization\<string\> | Query, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |

#### Friend Management & Requests
| Endpoint | Method | Parameters | Parameter Location | Status Code | Response | Description |
|----------|--------|------------|-------------------|-------------|----------|-------------|
| `/friends` | GET | Authorization\<string\> | Header | 200 OK | Set\<User\> | Get friends |
| `/friends` | GET | Authorization\<string\> | Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/friends` | GET | Authorization\<string\> | Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/friends/add/{receiverId}` | POST | Authorization\<string\>, receiverId\<long\> | Header, Path | 200 OK | FriendRequest | Send friend request |
| `/friends/add/{receiverId}` | POST | Authorization\<string\>, receiverId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid or missing receiverId |
| `/friends/add/{receiverId}` | POST | Authorization\<string\>, receiverId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | User not found |
| `/friends/friendrequest/{requestId}/accept` | POST | Authorization\<string\>, requestId\<long\> | Header, Path | 200 OK | FriendRequest | Accept friend request |
| `/friends/friendrequest/{requestId}/accept` | POST | Authorization\<string\>, requestId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid or missing requestId |
| `/friends/friendrequest/{requestId}/accept` | POST | Authorization\<string\>, requestId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Friend request not found |
| `/friends/friendrequest/{requestId}/reject` | POST | Authorization\<string\>, requestId\<long\> | Header, Path | 200 OK | FriendRequest | Reject friend request |
| `/friends/friendrequest/{requestId}/reject` | POST | Authorization\<string\>, requestId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid or missing requestId |
| `/friends/friendrequest/{requestId}/reject` | POST | Authorization\<string\>, requestId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Friend request not found |
| `/friends/friendrequests/sent` | GET | Authorization\<string\> | Header | 200 OK | List\<FriendRequest\> | Get sent friend requests |
| `/friends/friendrequests/sent` | GET | Authorization\<string\> | Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/friends/friendrequests/received` | GET | Authorization\<string\> | Header | 200 OK | List\<FriendRequest\> | Get received friend requests |
| `/friends/friendrequests/received` | GET | Authorization\<string\> | Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/friends/remove/{friendId}` | DELETE | Authorization\<string\>, friendId\<long\> | Header, Path | 204 No Content | - | Remove friend |
| `/friends/remove/{friendId}` | DELETE | Authorization\<string\>, friendId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid or missing friendId |
| `/friends/remove/{friendId}` | DELETE | Authorization\<string\>, friendId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Friend or User not found |
| `/friends/remove/{friendId}` | DELETE | Authorization\<string\>, friendId\<long\> | Header, Path | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/friends/friendrequest/{requestId}` | DELETE | Authorization\<string\>, requestId\<long\> | Header, Path | 204 No Content | - | Delete a friend request |
| `/friends/friendrequest/{requestId}` | DELETE | Authorization\<string\>, requestId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid or missing requestId |
| `/friends/friendrequest/{requestId}` | DELETE | Authorization\<string\>, requestId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Friend request not found |
| `/friends/friendrequest/{requestId}` | DELETE | Authorization\<string\>, requestId\<long\> | Header, Path | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/friends/friendrequest/{requestId}` | DELETE | Authorization\<string\>, requestId\<long\> | Header, Path | 403 Forbidden | Error: reason\<string\> | User not authorized to delete this friend request |
| `/users/{userId}/friends` | GET | userId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<User\> | Get friends for specific user |
| `/users/{userId}/friends` | GET | userId\<long\>, Authorization\<string\> | Path, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/friends` | GET | userId\<long\>, Authorization\<string\> | Path, Header | 404 Not Found | Error: reason\<string\> | User not found |

### User Watchlist & Watched-Movies Management

| Endpoint | Method | Parameters | Parameter Location | Status Code | Response | Description |
|----------|--------|------------|-------------------|-------------|----------|-------------|
| `/users/{userId}/watchlist` | GET | userId\<long\> | Path | 200 OK | List\<MovieGetDTO\> | Get all movies in a user's watchlist |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 201 Created | List\<MovieGetDTO\> | Add movie to user's watchlist |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 409 Conflict | Error: reason\<string\> | Movie is already in your watchlist |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 404 Not Found | Error: reason\<string\> | User or Movie not found |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify this watchlist |
| `/users/{userId}/watchlist/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 200 OK | List\<MovieGetDTO\> | Remove movie from user's watchlist |
| `/users/{userId}/watchlist/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 404 Not Found | Error: reason\<string\> | User, Movie, or entry not found |
| `/users/{userId}/watchlist/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/watchlist/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify this watchlist |
| `/users/{userId}/watched` | GET | userId\<long\> | Path | 200 OK | List\<MovieGetDTO\> | Get all movies in a user's watched list |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 201 Created | List\<MovieGetDTO\> | Add movie to user's watched list |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 409 Conflict | Error: reason\<string\> | Movie is already in your watched movies list |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 404 Not Found | Error: reason\<string\> | User or Movie not found |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify this watched list |
| `/users/{userId}/watched/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 200 OK | List\<MovieGetDTO\> | Remove movie from user's watched list |
| `/users/{userId}/watched/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 404 Not Found | Error: reason\<string\> | User, Movie, or entry not found |
| `/users/{userId}/watched/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/watched/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify this watched list |

### User Favorites Management

| Endpoint | Method | Parameters | Parameter Location | Status Code | Response | Description |
|----------|--------|------------|-------------------|-------------|----------|-------------|
| `/users/{userId}/favorites/genres` | POST | UserFavoritesGenresDTO\<object\>, Authorization\<string\> | Body, Header | 200 OK | UserFavoritesGenresDTO | Save genre favorites for a user |
| `/users/{userId}/favorites/genres` | POST | UserFavoritesGenresDTO\<object\>, Authorization\<string\> | Body, Header | 400 Bad Request | Error: reason\<string\> | Invalid genre (not in TMDb list) |
| `/users/{userId}/favorites/genres` | POST | UserFavoritesGenresDTO\<object\>, Authorization\<string\> | Body, Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/favorites/genres` | POST | UserFavoritesGenresDTO\<object\>, Authorization\<string\> | Body, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites/genres` | POST | UserFavoritesGenresDTO\<object\>, Authorization\<string\> | Body, Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify these favorites |
| `/users/{userId}/favorites/genres` | GET | - | - | 200 OK | { "genreIds": ["Action", "Adventure"] } | Get genre favorites for a user |
| `/users/{userId}/favorites/genres` | GET | - | - | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites/genres` | GET | - | - | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/favorites/movie` | POST | UserFavoritesMovieDTO\<object\>, Authorization\<string\> | Body, Header | 200 OK | UserFavoritesMovieDTO | Save favorite movie for a user |
| `/users/{userId}/favorites/movie` | POST | UserFavoritesMovieDTO\<object\>, Authorization\<string\> | Body, Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/favorites/movie` | POST | UserFavoritesMovieDTO\<object\>, Authorization\<string\> | Body, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites/movie` | POST | UserFavoritesMovieDTO\<object\>, Authorization\<string\> | Body, Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify these favorites |
| `/users/{userId}/favorites/movie` | GET | - | - | 200 OK | { "movie": { ...Movie fields... } } | Get favorite movie for a user |
| `/users/{userId}/favorites/movie` | GET | - | - | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites/movie` | GET | - | - | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/favorites/actors` | POST | UserFavoritesActorsDTO\<object\>, Authorization\<string\> | Body, Header | 200 OK | UserFavoritesActorsDTO | Save favorite actors for a user |
| `/users/{userId}/favorites/actors` | POST | UserFavoritesActorsDTO\<object\>, Authorization\<string\> | Body, Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/favorites/actors` | POST | UserFavoritesActorsDTO\<object\>, Authorization\<string\> | Body, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites/actors` | POST | UserFavoritesActorsDTO\<object\>, Authorization\<string\> | Body, Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify these favorites |
| `/users/{userId}/favorites/actors` | GET | - | - | 200 OK | { "actorIds": ["Leonardo DiCaprio", "Tom Hanks"] } | Get favorite actors for a user |
| `/users/{userId}/favorites/actors` | GET | - | - | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites/actors` | GET | - | - | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/favorites/directors` | POST | UserFavoritesDirectorsDTO\<object\>, Authorization\<string\> | Body, Header | 200 OK | UserFavoritesDirectorsDTO | Save favorite directors for a user |
| `/users/{userId}/favorites/directors` | POST | UserFavoritesDirectorsDTO\<object\>, Authorization\<string\> | Body, Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/favorites/directors` | POST | UserFavoritesDirectorsDTO\<object\>, Authorization\<string\> | Body, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites/directors` | POST | UserFavoritesDirectorsDTO\<object\>, Authorization\<string\> | Body, Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify these favorites |
| `/users/{userId}/favorites/directors` | GET | - | - | 200 OK | { "directorIds": ["Christopher Nolan", "Steven Spielberg"] } | Get favorite directors for a user |
| `/users/{userId}/favorites/directors` | GET | - | - | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites/directors` | GET | - | - | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/favorites` | GET | - | - | 200 OK | { "favoriteGenres": ["Action", "Adventure"], "favoriteMovie": { ...Movie fields... }, "favoriteActors": ["Leonardo DiCaprio", "Tom Hanks"], "favoriteDirectors": ["Christopher Nolan", "Steven Spielberg"] } | Get all favorites for a user |
| `/users/{userId}/favorites` | GET | - | - | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/favorites` | GET | - | - | 404 Not Found | Error: reason\<string\> | User not found |

### Movie Management

| Endpoint                            | Method | Parameters                                                                        | Location    | Status Code   | Response               | Description                                                |
|-------------------------------------|--------|-----------------------------------------------------------------------------------|-------------|---------------|------------------------|------------------------------------------------------------|
| `/movies`                           | GET    | title<string>?, genres<List<string>>?, year<integer>?, actors<List<string>>?, directors<List<string>>?, page<integer> | Query       | 200 OK        | List<MovieGetDTO>      | Search movies by title, genres, year, actors, or directors. |
| `/movies`                           | GET    | same as above                                                                    | Query       | 400 Bad Request| Error: reason<string>   | Invalid or missing search parameters or no results found.   |
| `/movies/{movieId}`                 | GET    | movieId<long>                                                                    | Path        | 200 OK        | MovieGetDTO            | Retrieve movie details by ID.                              |
| `/movies/{movieId}`                 | GET    | movieId<long>                                                                    | Path        | 404 Not Found  | Error: reason<string>   | Movie not found.                                           |
| `/movies/suggestions/{userId}`      | GET    | userId<long>                                                                     | Path        | 200 OK        | List<MovieGetDTO>      | Get personalized suggestions for a user.                   |
| `/movies/suggestions/{userId}`      | GET    | userId<long>                                                                     | Path        | 404 Not Found  | Error: reason<string>   | User not found or suggestions unavailable.                 |
| `/movies/genres`                    | GET    | -                                                                                 | -           | 200 OK        | JsonNode               | Retrieve all genres from TMDb.                              |
| `/movies/genres`                    | GET    | -                                                                                 | -           | 503 Service Unavailable | Error: reason<string> | TMDb service unavailable.                                  |

### Group Management

| Endpoint | Method | Parameters | Parameter Location | Status Code | Response | Description |
|----------|--------|------------|-------------------|-------------|----------|-------------|
| `/groups` | GET | Authorization\<string\> | Header | 200 OK | List\<Group\> | Get groups for user |
| `/groups` | POST | Authorization\<string\>, groupPostDTO\<GroupPostDTO\> | Header, Body | 201 Created | Group | Create group |
| `/groups/{groupId}` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 204 No Content | - | Delete group |
| `/groups/{groupId}` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 404 Not Found | Error: reason\<string\> | Group not found |
| `/groups/{groupId}` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/{groupId}` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 403 Forbidden | Error: reason\<string\> | Only the group creator can delete the group |
| `/groups/{groupId}` | GET | Authorization\<string\>, groupId\<long\> | Header, Path | 200 OK | GroupGetDTO | Get group details |
| `/groups/{groupId}` | GET | Authorization\<string\>, groupId\<long\> | Header, Path | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/{groupId}` | GET | Authorization\<string\>, groupId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Group not found or user not a member |
| `/groups/{groupId}` | PUT | Authorization\<string\>, groupId\<long\>, groupPostDTO\<object\> | Header, Path, Body | 200 OK | GroupGetDTO | Update group name |
| `/groups/{groupId}` | PUT | Authorization\<string\>, groupId\<long\>, groupPostDTO\<object\> | Header, Path, Body | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/{groupId}` | PUT | Authorization\<string\>, groupId\<long\>, groupPostDTO\<object\> | Header, Path, Body | 403 Forbidden | Error: reason\<string\> | Only the group creator can update the group |
| `/groups/{groupId}` | PUT | Authorization\<string\>, groupId\<long\>, groupPostDTO\<object\> | Header, Path, Body | 404 Not Found | Error: reason\<string\> | Group not found |
| `/groups/{groupId}/members` | GET | groupId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<User\> | Get group members |
| `/groups/{groupId}/members/{memberId}` | DELETE | groupId\<long\>, memberId\<long\>, Authorization\<string\> | Path, Header | 204 No Content | - | Remove member from group |
| `/groups/{groupId}/members/{memberId}` | DELETE | groupId\<long\>, memberId\<long\>, Authorization\<string\> | Path, Header | 404 Not Found | Error: reason\<string\> | Group or user not found |
| `/groups/{groupId}/members/{memberId}` | DELETE | groupId\<long\>, memberId\<long\>, Authorization\<string\> | Path, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/{groupId}/members/{memberId}` | DELETE | groupId\<long\>, memberId\<long\>, Authorization\<string\> | Path, Header | 403 Forbidden | Error: reason\<string\> | Only the group creator can remove members |
| `/groups/{groupId}/pool` | GET | groupId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<Movie\> | Get movie pool for group |
| `/groups/{groupId}/pool/{movieId}` | POST | groupId\<long\>, movieId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<Movie\> | Add movie to group pool |
| `/groups/{groupId}/pool/{movieId}` | DELETE | groupId\<long\>, movieId\<long\>, Authorization\<string\> | Path, Header | 204 No Content | - | Remove movie from group pool |
| `/groups/{groupId}/leave` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 204 No Content | - | Leave group |
| `/groups/{groupId}/leave` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 404 Not Found | Error: reason\<string\> | Group or User not found |
| `/groups/{groupId}/leave` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/{groupId}/leave` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 403 Forbidden | Error: reason\<string\> | User is not a member of this group |
| `/groups/invitations/send/{groupId}/{receiverId}` | POST | Authorization\<string\>, groupId\<long\>, receiverId\<long\> | Header, Path | 200 OK | GroupInvitationGetDTO | Send group invitation |
| `/groups/invitations/send/{groupId}/{receiverId}` | POST | Authorization\<string\>, groupId\<long\>, receiverId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid group or receiver ID |
| `/groups/invitations/send/{groupId}/{receiverId}` | POST | Authorization\<string\>, groupId\<long\>, receiverId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Group or user not found |
| `/groups/invitations/send/{groupId}/{receiverId}` | POST | Authorization\<string\>, groupId\<long\>, receiverId\<long\> | Header, Path | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/invitations/send/{groupId}/{receiverId}` | POST | Authorization\<string\>, groupId\<long\>, receiverId\<long\> | Header, Path | 403 Forbidden | Error: reason\<string\> | User is not authorized to invite to this group |
| `/groups/invitations/{invitationId}/accept` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 200 OK | GroupInvitationGetDTO | Accept group invitation |
| `/groups/invitations/{invitationId}/accept` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid invitation ID |
| `/groups/invitations/{invitationId}/accept` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Invitation not found |
| `/groups/invitations/{invitationId}/accept` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/invitations/{invitationId}/accept` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 403 Forbidden | Error: reason\<string\> | User not authorized to accept this invitation |
| `/groups/invitations/{invitationId}/reject` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 200 OK | GroupInvitationGetDTO | Reject group invitation |
| `/groups/invitations/{invitationId}/reject` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid invitation ID |
| `/groups/invitations/{invitationId}/reject` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Invitation not found |
| `/groups/invitations/{invitationId}/reject` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/invitations/{invitationId}/reject` | POST | Authorization\<string\>, invitationId\<long\> | Header, Path | 403 Forbidden | Error: reason\<string\> | User not authorized to reject this invitation |
| `/groups/invitations/sent` | GET | Authorization\<string\> | Header | 200 OK | List\<GroupInvitationGetDTO\> | Get pending sent group invitations |
| `/groups/invitations/sent` | GET | Authorization\<string\> | Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/invitations/received` | GET | Authorization\<string\> | Header | 200 OK | List\<GroupInvitationGetDTO\> | Get pending received group invitations |
| `/groups/invitations/received` | GET | Authorization\<string\> | Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/invitations/{invitationId}` | DELETE | Authorization\<string\>, invitationId\<long\> | Header, Path | 204 No Content | - | Delete a group invitation |
| `/groups/invitations/{invitationId}` | DELETE | Authorization\<string\>, invitationId\<long\> | Header, Path | 400 Bad Request | Error: reason\<string\> | Invalid invitation ID |
| `/groups/invitations/{invitationId}` | DELETE | Authorization\<string\>, invitationId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Invitation not found |
| `/groups/invitations/{invitationId}` | DELETE | Authorization\<string\>, invitationId\<long\> | Header, Path | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/invitations/{invitationId}` | DELETE | Authorization\<string\>, invitationId\<long\> | Header, Path | 403 Forbidden | Error: reason\<string\> | User not authorized to delete this invitation |

### Voting System & Group Phases

#### Group Phase Attribute
Each group has a `phase` attribute:
- `POOL`: Movie pool is open for additions.
- `VOTING`: Pool is locked; voting is open.
- `RESULTS`: Voting is closed; results can be viewed.

#### Phase Transition Endpoints
| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/groups/{groupId}/start-voting` | POST | groupId\<integer\>, userId\<integer\> | Path, Body | 200 OK | Success message\<string\> | Trigger transition from POOL to VOTING phase (only by group creator or auto when all have submitted) |
| `/groups/{groupId}/show-results` | POST | groupId\<integer\>, userId\<integer\> | Path, Body | 200 OK | Success message\<string\> | Trigger transition from VOTING to RESULTS phase (only by group creator or auto when all have voted) |

#### Voting Endpoints (Phase Restricted)
| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description | Allowed Phase |
|----------|--------|--------------------------------------------------------------|---------------|-------------|----------------------|-------------|---------------|
| `/groups/{groupId}/vote` | POST | groupId\<integer\>, userId\<integer\>, vote\<List\<Movie\>\> | Body | 200 OK | Success message\<string\> | Submit a vote | VOTING |
| `/groups/{groupId}/vote` | POST | groupId\<integer\>, userId\<integer\>, vote\<List\<Movie\>\> | Body | 400 Bad Request | Error: reason\<string\> | Submit a vote failed due to invalid vote request | VOTING |
| `/groups/{groupId}/vote` | POST | groupId\<integer\>, userId\<integer\>, vote\<List\<Movie\>\> | Body | 404 Not Found | Error: reason\<string\> | Group not found | VOTING |
| `/groups/{groupId}/results` | GET | groupId\<integer\> | Path | 200 OK | List\<Movie\> | Retrieve voting results - list of movies with maximum number of votes | RESULTS |
| `/groups/{groupId}/results` | GET | groupId\<integer\> | Path | 400 Bad Request | Error: reason\<string\> | Retrieve voting results failed due to invalid group ID | RESULTS |
| `/groups/{groupId}/movies/rankable` | GET | groupId\<long\> | Path | 200 OK | List\<MovieGetDTO\> | Retrieve the list of movies available for ranking in the group's pool | VOTING |
| `/groups/{groupId}/rankings/details` | GET | groupId\<long\> | Path | 200 OK | List\<MovieAverageRankDTO\> | Retrieve detailed average ranking results for all movies (sorted by best rank) | RESULTS |
| `/groups/{groupId}/users/{userId}/rankings` | POST | groupId\<long\>, userId\<long\>, rankingSubmitDTOs\<List\<RankingSubmitDTO\>\> | Path, Body | 204 No Content | - | Submit a user's movie rankings for a group | VOTING |
| `/groups/{groupId}/users/{userId}/rankings` | POST | groupId\<long\>, userId\<long\>, rankingSubmitDTOs\<List\<RankingSubmitDTO\>\> | Path, Body | 400 Bad Request | Error: reason\<string\> | Invalid ranking data (e.g., invalid rank, duplicate movies, wrong number of movies ranked) | VOTING |
| `/groups/{groupId}/users/{userId}/rankings` | POST | groupId\<long\>, userId\<long\>, rankingSubmitDTOs\<List\<RankingSubmitDTO\>\> | Path, Body | 404 Not Found | Error: reason\<string\> | User or Group with the given ID not found | VOTING |
| `/groups/{groupId}/rankings/result` | GET | groupId\<long\> | Path | 404 Not Found | Error: reason\<string\> | Group not found or no ranking result exists for the group yet | RESULTS |
| `/groups/{groupId}/movies/rankable` | GET | groupId\<long\> | Path | 404 Not Found | Error: reason\<string\> | Group not found or group has no movie pool | VOTING |
| `/groups/{groupId}/rankings/details` | GET | groupId\<long\> | Path | 404 Not Found | Error: reason\<string\> | Group not found | RESULTS |
| `/groups/{groupId}/vote-state` | GET | Authorization\<string\>, groupId\<long\> | Header, Path | 200 OK | VoteStateGetDTO | Get current vote state (pool and user rankings) | VOTING |
| `/groups/{groupId}/vote-state` | GET | Authorization\<string\>, groupId\<long\> | Header, Path | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token | VOTING |
| `/groups/{groupId}/vote-state` | GET | Authorization\<string\>, groupId\<long\> | Header, Path | 404 Not Found | Error: reason\<string\> | Group not found or user not a member | VOTING |

**Note:**
- Adding movies to pool is only allowed in the POOL phase.
- Voting is only allowed in the VOTING phase.
- Results are only viewable in the RESULTS phase.
- Endpoints must return `409 Conflict` if the operation is not allowed in the current phase.
- The group resource and responses should always include the current `phase`.

### Reviews

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
{{ ... }}
| `/reviews` | POST | Review \<object\> | Body | 400 Bad Request | Error: reason \<string\> | Submit a review failed due to invalid or missing parameters |
| `/reviews` | POST | Review \<object\> | Body | 409 Conflict | Error: reason \<string\> | Submit a review failed because review already exists |
| `/movies/{movieId}/reviews` | PUT | Review \<object\> | Body | 204 No Content | Success message \<string\> | Update a review |
| `/movies/{movieId}/reviews` | DELETE | reviewId \<integer\> | Path | 204 No Content | Success message \<string\> | Delete a review |

### Ranking System (Phase-Aware)

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description | Allowed Phase |
|----------|--------|--------------------------------------------------------------|---------------|-------------|----------------------|-------------|---------------|
| `/groups/{groupId}/users/{userId}/rankings` | POST | groupId\<long\>, userId\<long\>, rankingSubmitDTOs\<List\<RankingSubmitDTO\>\> | Path, Body | 204 No Content | - | Submit a user's movie rankings for a group | VOTING |
| `/groups/{groupId}/rankings/result` | GET | groupId\<long\> | Path | 200 OK | RankingResultGetDTO | Retrieve the latest calculated ranking result for the group (the winning movie and its average rank) | RESULTS |
| `/groups/{groupId}/movies/rankable` | GET | groupId\<long\> | Path | 200 OK | List\<MovieGetDTO\> | Retrieve the list of movies available for ranking in the group's pool | VOTING |
| `/groups/{groupId}/rankings/details` | GET | groupId\<long\> | Path | 200 OK | List\<MovieAverageRankDTO\> | Retrieve the detailed average ranking results for all movies in the group (each movie with its average rank, sorted by best rank) | RESULTS |
| `/groups/{groupId}/users/{userId}/rankings` | POST | rankingSubmitDTOs\<List\<RankingSubmitDTO\>\> | Body | 400 Bad Request | Error | Invalid ranking data (e.g., invalid rank, duplicate movies, wrong number of movies ranked) | VOTING |
| `/groups/{groupId}/users/{userId}/rankings` | POST | groupId\<long\>, userId\<long\> | Path | 404 Not Found | Error | User or Group with the given ID not found | VOTING |
| `/groups/{groupId}/rankings/result` | GET | groupId\<long\> | Path | 404 Not Found | Error | Group not found or no ranking result exists for the group yet | RESULTS |
| `/groups/{groupId}/movies/rankable` | GET | groupId\<long\> | Path | 404 Not Found | Error | Group not found or group has no movie pool | VOTING |
| `/groups/{groupId}/rankings/details` | GET | groupId\<long\> | Path | 404 Not Found | Error | Group not found | RESULTS |

**Note:** All ranking endpoints must check the group phase and return `409 Conflict` if called in the wrong phase.

#### Group Entity Update
Add a `phase` attribute to the group object:
```json
{
  ...
  "phase": "POOL" | "VOTING" | "RESULTS"
}
```
This field must be included in all relevant group-related responses.

```
