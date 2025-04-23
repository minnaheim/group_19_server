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

### UserPreferencesGenresDTO Object

A `UserPreferencesGenresDTO object` is a JSON object with the following structure:

```json
{
  "genreIds": [string]
}
```

### UserPreferencesFavoriteMovieDTO Object

A `UserPreferencesFavoriteMovieDTO object` is a JSON object with the following structure:

```json
{
  "movieId": integer
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
| `/register` | POST | userPostDTO\<UserPostDTO\> | Body | 201 Created | User | Register a new user |
| `/register` | POST | userPostDTO\<UserPostDTO\> | Body | 400 Bad Request | Error: reason\<string\> | Invalid input or missing fields |
| `/register` | POST | userPostDTO\<UserPostDTO\> | Body | 409 Conflict | Error: reason\<string\> | Username or email already exists |
| `/login` | POST | userPostDTO\<UserPostDTO\> | Body | 200 OK | User | User successfully logged in |
| `/login` | POST | userPostDTO\<UserPostDTO\> | Body | 400 Bad Request | Error: reason\<string\> | Invalid input or missing fields |
| `/login` | POST | userPostDTO\<UserPostDTO\> | Body | 404 Not Found | Error: reason\<string\> | User not found |
| `/login` | POST | userPostDTO\<UserPostDTO\> | Body | 401 Unauthorized | Error: reason\<string\> | Invalid password |
| `/logout` | POST | token\<string\> | Query | 200 OK | - | User logs out |
| `/logout` | POST | token\<string\> | Query | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/session` | GET | token\<string\> | Query | 200 OK | User | Validate session |
| `/session` | GET | token\<string\> | Query | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/session` | GET | token\<string\> | Query | 404 Not Found | Error: reason\<string\> | User not found |
| `/check/username` | GET | username\<string\> | Query | 200 OK | boolean | Check username availability |
| `/check/username` | GET | username\<string\> | Query | 400 Bad Request | Error: reason\<string\> | Invalid or missing username |
| `/check/email` | GET | email\<string\> | Query | 200 OK | boolean | Check email availability |
| `/check/email` | GET | email\<string\> | Query | 400 Bad Request | Error: reason\<string\> | Invalid or missing email |
| `/users/{userId}/profile` | GET | userId\<long\> | Path | 200 OK | User | Retrieve user profile |
| `/users/{userId}/profile` | GET | userId\<long\> | Path | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/profile` | PUT | userId\<long\>, userPostDTO\<UserPostDTO\>, token\<string\> or Authorization\<string\> | Path, Body, Query/Header | 200 OK | User | Update user profile |
| `/users/{userId}/profile` | PUT | userId\<long\>, userPostDTO\<UserPostDTO\>, token\<string\> or Authorization\<string\> | Path, Body, Query/Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/profile` | PUT | userId\<long\>, userPostDTO\<UserPostDTO\>, token\<string\> or Authorization\<string\> | Path, Body, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
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
| `/friends/remove/{friendId}` | DELETE | Authorization\<string\>, friendId\<long\> | Header, Path | 403 Forbidden | Error: reason\<string\> | This user is not your friend |

### User Watchlist & Watched-Movies Management

| Endpoint | Method | Parameters | Parameter Location | Status Code | Response | Description |
|----------|--------|------------|-------------------|-------------|----------|-------------|
| `/users/{userId}/watchlist` | GET | userId\<long\> | Path | 200 OK | List\<Movie\> | Get all movies in a user's watchlist |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 201 Created | List\<Movie\> | Add movie to user's watchlist |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 409 Conflict | Error: reason\<string\> | Movie is already in your watchlist |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 404 Not Found | Error: reason\<string\> | User or Movie not found |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/watchlist/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify this watchlist |
| `/users/{userId}/watchlist/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 200 OK | List\<Movie\> | Remove movie from user's watchlist |
| `/users/{userId}/watchlist/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 404 Not Found | Error: reason\<string\> | User, Movie, or entry not found |
| `/users/{userId}/watchlist/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/watchlist/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify this watchlist |
| `/users/{userId}/watched` | GET | userId\<long\> | Path | 200 OK | List\<Movie\> | Get all movies in a user's watched list |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 201 Created | List\<Movie\> | Add movie to user's watched list |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 409 Conflict | Error: reason\<string\> | Movie is already in your watched movies list |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 404 Not Found | Error: reason\<string\> | User or Movie not found |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/watched/{movieId}` | POST | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify this watched list |
| `/users/{userId}/watched/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 200 OK | List\<Movie\> | Remove movie from user's watched list |
| `/users/{userId}/watched/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 404 Not Found | Error: reason\<string\> | User, Movie, or entry not found |
| `/users/{userId}/watched/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/watched/{movieId}` | DELETE | userId\<long\>, movieId\<long\>, token\<string\> or Authorization\<string\> | Path, Query/Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify this watched list |

### User Preferences Management

| Endpoint | Method | Parameters | Parameter Location | Status Code | Response | Description |
|----------|--------|------------|-------------------|-------------|----------|-------------|
| `/users/{userId}/preferences/genres` | POST | userId\<long\>, UserPreferencesGenresDTO\<object\>, Authorization\<string\> | Path, Body, Header | 200 OK | UserPreferencesGenresDTO | Save genre preferences for a user |
| `/users/{userId}/preferences/genres` | POST | userId\<long\>, UserPreferencesGenresDTO\<object\>, Authorization\<string\> | Path, Body, Header | 400 Bad Request | Error: reason\<string\> | Invalid genre (not in TMDb list) |
| `/users/{userId}/preferences/genres` | POST | userId\<long\>, UserPreferencesGenresDTO\<object\>, Authorization\<string\> | Path, Body, Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/preferences/genres` | POST | userId\<long\>, UserPreferencesGenresDTO\<object\>, Authorization\<string\> | Path, Body, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/preferences/genres` | POST | userId\<long\>, UserPreferencesGenresDTO\<object\>, Authorization\<string\> | Path, Body, Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify these preferences |
| `/users/{userId}/preferences/genres` | GET | userId\<long\> | Path | 200 OK | { "genreIds": ["Action", "Adventure"] } | Get genre preferences for a user |
| `/users/{userId}/preferences/favorite-movie` | POST | userId\<long\>, UserPreferencesFavoriteMovieDTO\<object\>, Authorization\<string\> | Path, Body, Header | 200 OK | UserPreferencesFavoriteMovieDTO | Save favorite movie for a user |
| `/users/{userId}/preferences/favorite-movie` | POST | userId\<long\>, UserPreferencesFavoriteMovieDTO\<object\>, Authorization\<string\> | Path, Body, Header | 404 Not Found | Error: reason\<string\> | User not found |
| `/users/{userId}/preferences/favorite-movie` | POST | userId\<long\>, UserPreferencesFavoriteMovieDTO\<object\>, Authorization\<string\> | Path, Body, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/users/{userId}/preferences/favorite-movie` | POST | userId\<long\>, UserPreferencesFavoriteMovieDTO\<object\>, Authorization\<string\> | Path, Body, Header | 403 Forbidden | Error: reason\<string\> | User is not authorized to modify these preferences |
| `/users/{userId}/preferences/favorite-movie` | GET | userId\<long\> | Path | 200 OK | { "movie": { ...Movie fields... } } | Get favorite movie for a user |
| `/users/{userId}/preferences` | GET | userId\<long\> | Path | 200 OK | { "favoriteGenres": ["Action", "Adventure"], "favoriteMovie": { ...Movie fields... } } | Get all preferences for a user |

### Movie Management

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/movies` | GET | Title \<string\> (e.g., "Inception") or Genres \<List\<string\>\> (e.g., ["Action","Science Fiction","Adventure"]) or Century \<integer\> (e.g., 2010) or actorId \<integer\> (e.g., 6193) or directorId \<integer\> (e.g., 525) | Query | 200 OK | List \<Movie\> | Retrieve movies based on search filters |
| `/movies` | GET | Title \<string\> or Genres \<List\<string\>\> or Century \<integer\> or actorId \<integer\> or directorId \<integer\> | Query | 400 Bad Request | Error: reason \<string\> | Retrieve movies based on search filters failed due to invalid Query parameters |
| `/movies/actors` | GET | Name \<string\> (e.g., "Leonardo DiCaprio") | Query | 200 OK | List \<Actor\> | Retrieve actors based on search string |
| `/movies/actors` | GET | Name \<string\> (e.g., "Leonardo DiCaprio") | Query | 400 Bad Request | Error: reason \<string\> | Retrieve actors based on search strings failed due to invalid Query parameters |
| `/movies/directors` | GET | Name \<string\> (e.g., "Christopher Nolan") | Query | 200 OK | List \<Director\> | Retrieve directors based on search string |
| `/moives/directors` | GET | name \<string\> (e.g., "Christopher Nolan") | Query | 400 Bad Request | Error: reason \<string\> | Retrieve directors based on search strings failed due to invalid Query parameters |
| `/movies/{movieId}` | GET | movieId \<integer\> | Path | 200 OK | Movie(*) | Retrieve movie details |
| `/movies/{movieId}` | GET | movieId \<integer\> | Path | 404 Not Found | Error: reason \<string\> | Movie was not found |
| `/movies/suggestions/{userId}` | GET | userId \<integer\> | Path | 200 OK | List \<Movie\> | Retrieve personalized movie suggestions |
| `/movies/suggestions/{userId}` | GET | userId \<integer\> | Path | 400 Bad Request | Error: reason \<string\> | Retrieve of suggestion failed due to invalid Path parameters |
| `/movies/genres` | GET | - | - | 200 OK | List\<Genres\> | Retrieve all genres |

### Group Management

| Endpoint | Method | Parameters | Parameter Location | Status Code | Response | Description |
|----------|--------|------------|-------------------|-------------|----------|-------------|
| `/groups` | GET | Authorization\<string\> | Header | 200 OK | List\<Group\> | Get groups for user |
| `/groups` | POST | Authorization\<string\>, groupPostDTO\<GroupPostDTO\> | Header, Body | 201 Created | Group | Create group |
| `/groups/{groupId}` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 204 No Content | - | Delete group |
| `/groups/{groupId}` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 404 Not Found | Error: reason\<string\> | Group not found |
| `/groups/{groupId}` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/{groupId}` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 403 Forbidden | Error: reason\<string\> | Only the group creator can delete the group |
| `/groups/{groupId}/members` | GET | groupId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<User\> | Get group members |
| `/groups/{groupId}/pool` | GET | groupId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<Movie\> | Get movie pool for group |
| `/groups/{groupId}/pool/{movieId}` | POST | groupId\<long\>, movieId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<Movie\> | Add movie to group pool |
| `/groups/{groupId}/members` | GET | groupId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List<User> | Get group members |
| `/groups/{groupId}/pool` | GET | groupId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<Movie\> | Get movie pool for group |
| `/groups/{groupId}/pool/{movieId}` | POST | groupId\<long\>, movieId\<long\>, Authorization\<string\> | Path, Header | 200 OK | List\<Movie\> | Add movie to group pool |
| `/groups/{groupId}/pool/{movieId}` | DELETE | groupId\<long\>, movieId\<long\>, Authorization\<string\> | Path, Header | 204 No Content | - | Remove movie from group pool |
| `/groups/{groupId}/leave` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 204 No Content | - | Leave group |
| `/groups/{groupId}/leave` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 404 Not Found | Error: reason\<string\> | Group or User not found |
| `/groups/{groupId}/leave` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 401 Unauthorized | Error: reason\<string\> | Invalid or missing token |
| `/groups/{groupId}/leave` | DELETE | groupId\<long\>, Authorization\<string\> | Path, Header | 403 Forbidden | Error: reason\<string\> | User is not a member of this group |

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
|----------|--------|------------|---------------|-------------|----------|-------------|---------------|
| `/groups/{groupId}/vote` | POST | groupId\<integer\>, userId\<integer\>, vote\<List\<Movie\>\> | Body | 200 OK | Success message\<string\> | Submit a vote | VOTING |
| `/groups/{groupId}/vote` | POST | groupId\<integer\>, userId\<integer\>, vote\<List\<Movie\>\> | Body | 400 Bad Request | Error: reason\<string\> | Submit a vote failed due to invalid vote request | VOTING |
| `/groups/{groupId}/vote` | POST | groupId\<integer\>, userId\<integer\>, vote\<List\<Movie\>\> | Body | 404 Not Found | Error: reason\<string\> | Group not found | VOTING |
| `/groups/{groupId}/results` | GET | groupId\<integer\> | Path | 200 OK | List\<Movie\> | Retrieve voting results - list of movies with maximum number of votes | RESULTS |
| `/groups/{groupId}/results` | GET | groupId\<integer\> | Path | 400 Bad Request | Error: reason\<string\> | Retrieve voting results failed due to invalid group ID | RESULTS |

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
