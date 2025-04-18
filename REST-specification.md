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

### Movie
- **movieId**: long (e.g., 27205)
- **title**: string (e.g., "Inception")
- **genres**: List\<string\> (e.g., ["Action", "Science Fiction", "Adventure"])
- **year**: int (e.g., 2010)
- **actors**: List\<string\> (e.g., ["Leonardo DiCaprio", "Joseph Gordon-Levitt", "Ken Watanabe", "Tom Hardy", "Elliot Page"]) - first 5 actors, if listed in TMDb – can be sorted by popularity
- **directors**: List\<string\> (e.g., ["Christopher Nolan", "Emma Thomas"]) - first 2 directors, if listed in TMDb – can be sorted by popularity
- **originallanguage**: string (e.g., "English")
- **trailerURL**: string (e.g., "https://www.youtube.com/watch?v=mpj9dL7swwk")
- **posterURL**: string (e.g., "https://image.tmdb.org/t/p/w500/ljsZTbVsrQSqZgWeep2B1QiDKuh.jpg")
- **description**: string (e.g., "Cobb, a skilled thief who commits corporate espionage by infiltrating the subconscious of his targets is offered a chance to regain his old life as payment for a task considered to be impossible: \"inception\", the implantation of another person's idea into a target's subconscious.")

**Notes:**
- Lists can be empty (genres, actors, directors) if information is not available in TMDb.
- Description strings may have "weird" format (i.e. escaped quotes like \"inception\")

### Actor
- **actorId**: long (e.g., 6193)
- **name**: string (e.g., "Leonardo DiCaprio")

### Director
- **directorId**: long (e.g., 525)
- **name**: string (e.g., "Christopher Nolan")

### Genres
- **id**: integer (e.g., 28)
- **name**: string (e.g., "Action")

### User
- **userId**: number
- **username**: string
- **email**: string
- **password**: string
- **bio**: string
- **favoriteGenres**: string[]
- **favoriteMovie**: Movie
- **watchlist**: Movie[]
- **watchedMovies**: Movie[]

### RankingSubmitDTO (Used in Request Body)
- **movieId**: long (e.g., 27205) - *Required*
- **rank**: integer (e.g., 1) - *Required, Min: 1*

### RankingResultGetDTO (Used in Response Body)
- **calculationTimestamp**: string (ISO 8601 format, e.g., "2025-04-11T21:30:00.123Z")
- **winningMovie**: Movie(*) - Contains the full details of the winning movie.

## API Endpoints

### User Management

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/register` | POST | username \<string\>, password \<string\>, email \<string\> | Body | 201 Created | User(*) | Register a new user |
| `/register` | POST | username \<string\>, password \<string\>, email \<string\> | Body | 409 Conflict | Error: reason \<string\> | Registration failed due to existing username/email |
| `/register` | POST | username \<string\>, password \<string\>, email \<string\> | Body | 400 Bad Request | Error: reason \<string\> | Registration failed due to incomplete information or invalid input |
| `/login` | PUT | username \<string\>, password \<string\> | Body | 200 OK | User(*) | User successfully logged in |
| `/login` | PUT | username \<string\>, password \<string\> | Body | 404 Not Found | Error: reason \<string\> | Login failed due to incorrect credentials |
| `/login` | PUT | username \<string\>, password \<string\> | Body | 400 Bad Request | Error: reason \<string\> | Login failed due to missing required fields |
| `/logout/{userId}` | PUT | userId \<integer\> | Path | 200 OK | Success message \<string\> | User logs out |
| `/profile/{userId}` | GET | userId \<integer\> | Path | 200 OK | User(*) | Retrieve user profile |
| `/profile/{userId}` | GET | userId \<integer\> | Path | 404 Not Found | Error: reason \<string\> | User not found |
| `/profile/{userId}` | PUT | User | Body | 204 No Content | | Update user profile |
| `/profile/{userId}` | PUT | User | Body | 404 Not Found | Error: reason \<string\> | User not found |
| `/friends/{userId}` | GET | userId \<integer\> | Path | 200 OK | List\<User\> | Retrieve friends of user |
| `/friends/{userId}` | GET | userId \<integer\> | Path | 404 Not Found | Error: reason \<string\> | User not found |
| `/friends` | GET | Username \<string\> | Query | 200 OK | User | Retrieve user with provided username |
| `/friends` | GET | Username \<string\> | Query | 404 Not Found | Error: reason \<string\> | Username not found, spelling error or User not registered |
| `/friends/add/{userId}` | POST | User | Body | 200 OK | User(*) | Add user to friends |
| `/friends/remove/{userId}` | DELETE | User | Body | 204 No Content | | Delete user from friends |

### User Watchlist & Watched-Movies Management

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/watchlist/{userId}` | GET | userId \<integer\> | Path | 200 OK | List \<Movie\> | Retrieve user's watchlist |
| `/watchlist/{userId}` | GET | userId \<integer\> | Path | 400 Bad Request | Error: reason \<string\> | Invalid request (e.g., missing userId) |
| `/watchlist/{userId}` | GET | userId \<integer\> | Path | 404 Not Found | Error: reason \<string\> | Watchlist not found |
| `/watchlist/{userId}` | POST | userId \<integer\>, movieId \<integer\> | Body | 201 Created | Movie(*) | Add movie to watchlist |
| `/watchlist/{userId}` | POST | userId \<integer\>, movieId \<integer\> | Body | 400 Bad Request | Error: reason \<string\> | Add movie to watchlist failed due to invalid or missing parameters |
| `/watchlist/{userId}` | POST | userId \<integer\>, movieId \<integer\> | Body | 409 Conflict | Error: reason \<string\> | Add movie to watchlist failed because movie is already in watchlist |
| `/watchlist/{userId}` | DELETE | userId \<integer\>, movieId \<integer\> | Path | 200 OK | Success message \<string\> | Remove movie from watchlist |
| `/watchlist/{userId}` | DELETE | userId \<integer\>, movieId \<integer\> | Path | 400 Bad Request | Error: reason \<string\> | Remove movie from watchlist failed due to Invalid or missing parameters |
| `/watched/{userId}` | GET | userId \<integer\> | Path | 200 OK | List \<Movie\> | Retrieve user's watched movies |
| `/watched/{userId}` | GET | userId \<integer\> | Path | 400 Bad Request | Error: reason \<string\> | Retrieve user's watched movies failed due to invalid request (e.g., missing userId) |
| `/watched/{userId}` | GET | userId \<integer\> | Path | 404 Not Found | Error: reason \<string\> | No watched movies found |
| `/watched/{userId}` | POST | userId \<integer\>, movieId \<integer\> | Body | 201 Created | Movie (*) | Add movie to watched movies |
| `/watched/{userId}` | POST | userId \<integer\>, movieId \<integer\> | Body | 400 Bad Request | Error: reason \<string\> | Add movie to watched movies failed due to invalid or missing parameters |
| `/watched/{userId}` | POST | userId \<integer\>, movieId \<integer\> | Body | 409 Conflict | Error: reason \<string\> | Add movie to watched movies failed because movie already is in watched movies |
| `/watched/{userId}` | DELETE | userId \<integer\>, movieId \<integer\> | Path | 200 OK | Success message \<string\> | Remove movie from watched movies |
| `/watched/{userId}` | DELETE | userId \<integer\>, movieId \<integer\> | Path | 400 Bad Request | Error: reason \<string\> | Remove movie from watched movies failed due to invalid or missing parameters |

### User Preferences Management

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/genres` | GET | - | - | 200 OK | Array of genre objects | Retrieve all available genres from TMDb |
| `/users/{userId}/preferences/genres` | POST | userId <integer>, genreIds List<string> | Path, Body | 200 OK | { "genreIds": ["Action", "Adventure"] } | Save genre preferences for a user |
| `/users/{userId}/preferences/genres` | GET | userId <integer> | Path | 200 OK | { "genreIds": ["Action", "Adventure"] } | Get genre preferences for a user |
| `/users/{userId}/preferences/favorite-movie` | POST | userId <integer>, movieId <integer> | Path, Body | 200 OK | { "movieId": 123 } | Save favorite movie for a user |
| `/users/{userId}/preferences/favorite-movie` | GET | userId <integer> | Path | 200 OK | { "movie": { ...Movie fields... } } | Get favorite movie for a user |
| `/users/{userId}/preferences` | GET | userId <integer> | Path | 200 OK | { "favoriteGenres": ["Action", "Adventure"], "favoriteMovie": { ...Movie fields... } } | Get all preferences for a user |
| `/users/{userId}/preferences/genres` | POST | userId <integer>, genreIds List<string> | Path, Body | 400 Bad Request | Error: reason <string> | Invalid genre ID(s) provided | 
| `/users/{userId}/preferences/genres` | POST | userId <integer>, genreIds List<string> | Path, Body | 401 Unauthorized | Error: reason <string> | Invalid or missing authentication token | 
| `/users/{userId}/preferences/genres` | POST | userId <integer>, genreIds List<string> | Path, Body | 404 Not Found | Error: reason <string> | User not found | 
| `/users/{userId}/preferences/favorite-movie` | POST | userId <integer>, movieId <integer> | Path, Body | 400 Bad Request | Error: reason <string> | Invalid movie ID provided | 
| `/users/{userId}/preferences/favorite-movie` | POST | userId <integer>, movieId <integer> | Path, Body | 401 Unauthorized | Error: reason <string> | Invalid or missing authentication token | 
| `/users/{userId}/preferences/favorite-movie` | POST | userId <integer>, movieId <integer> | Path, Body | 404 Not Found | Error: reason <string> | User or movie not found | 

#### Example Responses


**POST /users/{userId}/preferences/genres**
```json
{
  "genreIds": ["Action", "Adventure"]
}
```

**GET /users/{userId}/preferences/genres**
```json
{
  "genreIds": ["Action", "Adventure"]
}
```

**POST /users/{userId}/preferences/favorite-movie**
```json
{
  "movieId": 123
}
```

**GET /users/{userId}/preferences/favorite-movie**
```json
{
  "movie": {
    "movieId": 123,
    "title": "Test Movie",
    "genres": ["Action", "Adventure"],
    "year": 2020,
    "actors": ["Actor 1", "Actor 2"],
    "directors": ["Director 1"],
    "originallanguage": "English",
    "trailerURL": "https://...",
    "posterURL": "https://...",
    "description": "A movie description."
  }
}
```

**GET /users/{userId}/preferences**
```json
{
  "favoriteGenres": ["Action", "Adventure"],
  "favoriteMovie": {
    "movieId": 123,
    "title": "Test Movie",
    "genres": ["Action", "Adventure"],
    "year": 2020,
    "actors": ["Actor 1", "Actor 2"],
    "directors": ["Director 1"],
    "originallanguage": "English",
    "trailerURL": "https://...",
    "posterURL": "https://...",
    "description": "A movie description."
  }
}
```

### Movie Management

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/movies` | GET | Title \<string\> (e.g., "Inception") or Genres List\<string\> (e.g., ["Action","Science Fiction","Adventure"]) or Century \<integer\> (e.g., 2010) or actorId \<integer\> (e.g., 6193) or directorId \<integer\> (e.g., 525) | Query | 200 OK | List \<Movie\> | Retrieve movies based on search filters |
| `/movies` | GET | Title \<string\> or Genres List\<string\> or Century \<integer\> or actorId \<integer\> or directorId \<integer\> | Query | 400 Bad Request | Error: reason \<string\> | Retrieve movies based on search filters failed due to invalid Query parameters |
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

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/groups` | POST | userId \<integer\>, groupName \<string\> | Body | 201 Created | Group(*) | Create a new group |
| `/groups` | POST | userId \<integer\>, groupName \<string\> | Body | 400 Bad Request | Error: reason \<string\> | Create a new group failed due to missing or invalid parameters |
| `/groups` | POST | userId \<integer\>, groupName \<string\> | Body | 409 Conflict | Error: reason \<string\> | Create a new group failed because group name already exists |
| `/groups` | DELETE | userId \<integer\>, groupName \<string\> | Body | 200 OK | Success message \<string\> | Group was deleted successfully |
| `/groups/{groupId}/pool` | POST | groupId \<integer\>, movieId \<integer\> | Path, Body | 200 OK | Success message \<string\> | Add movie to group pool |
| `/groups/{groupId}/pool` | POST | groupId \<integer\>, movieId \<integer\> | Body | 400 Bad Request | Error: reason \<string\> | Add movie to group pool failed dur to invalid or missing parameters |
| `/groups/{groupId}/pool` | POST | groupId \<integer\>, movieId \<integer\> | Body | 409 Conflict | Error: reason \<string\> | Add movie to group pool failed because movie is already in pool |
| `/groups/{groupId}/pool` | GET | groupId \<integer\> | Path | 200 OK | List\<Movie\> | Retrieve all movies in group pool |
| `/groups/{groupId}/pool` | GET | groupId \<integer\> | Path | 400 Bad Request | Error: reason \<string\> | Invalid or missing group ID |
| `/groups/{groupId}/invite` | POST | groupId \<integer\>, username \<string\> | Path, Body | 200 OK | Successful message: Invitation was sent \<string\> | Invite a user to a group |
| `/groups/{groupId}/invite` | POST | groupId \<integer\>, username \<string\> | Path, Body | 404 Not Found | Error: reason \<string\> | Username of invitee was not found |
| `/groups/{groupId}/invite/accept` | POST | groupId \<integer\>, userId \<integer\> | Path, Body | 200 OK | Successful message: Invitation was accepted \<string\> | Accept a group invitation |
| `/groups/{groupId}/invite/reject` | POST | groupId \<integer\>, userId \<integer\> | Path, Body | 200 OK | Successful message: Invitation was rejected \<string\> | Reject a group invitation |

### Voting System

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/groups/{groupId}/vote` | POST | groupId \<integer\>, userId \<integer\>, vote List\<Movie\> | Body | 200 OK | Success message \<string\> | Submit a vote |
| `/groups/{groupId}/vote` | POST | groupId \<integer\>, userId \<integer\>, vote List\<Movie\> | Body | 400 Bad Request | Error: reason \<string\> | Submit a voice failed due to invalid vote request |
| `/groups/{groupId}/vote` | POST | groupId \<integer\>, userId \<integer\>, vote List\<Movie\> | Body | 404 Not Found | Error: reason \<string\> | Group not found |
| `/groups/{groupId}/results` | GET | groupId \<integer\> | Path | 200 OK | List\<Movie\> | Retrieve voting results - list of movies with maximum number of votes |
| `/groups/{groupId}/results` | GET | groupId \<integer\> | Path | 400 Bad Request | Error: reason \<string\> | Retrieve voting results failed due to invalid group ID |

### Reviews & Ratings

| Endpoint | Method | Parameters | Parameter Type | Status Code | Response | Description |
|----------|--------|------------|---------------|-------------|----------|-------------|
| `/reviews` | GET | movieId \<integer\> (optional), userId \<integer\> (optional) | Path | 200 OK | List \<Review\> | Retrieve movie reviews |
| `/reviews` | GET | movieId \<integer\> (optional), userId \<integer\> (optional) | Path | 400 Bad Request | Error: reason \<string\> | Retrieve movie reviews failed due to invalid Path parameters |
| `/reviews` | POST | Review | Body | 201 Created | Review(*) | Submit a review |
| `/reviews` | POST | Review | Body | 400 Bad Request | Error: reason \<string\> | Submit a review failed due to invalid or missing parameters |
| `/reviews` | POST | Review | Body | 409 Conflict | Error: reason \<string\> | Submit a review failed because review already exists |
| `/movies/{movieId}/reviews` | PUT | Review | Body | 204 No Content | Success message \<string\> | Update a review |
| `/movies/{movieId}/reviews` | DELETE | reviewId \<integer\> | Path | 204 No Content | Success message \<string\> | Delete a review |

### Ranking System

| Endpoint                                             | Method | Parameters                    | Parameter Type | Status Code        | Response                        | Description |
|------------------------------------------------------|--------|-------------------------------|----------------|--------------------|----------------------------------|-------------|
| `/groups/{groupId}/users/{userId}/rankings`      | POST   | `groupId` (long), `userId` (long) | Path           | 204 No Content     | -                                | Submit a user's movie rankings for a specific group. |
|                                                      |        | Array<RankingSubmitDTO>       | Body           | 400 Bad Request    | Error(*)                         | Invalid ranking data (e.g., invalid rank, duplicate movies, wrong number of movies ranked). |
|                                                      |        |                               |                | 404 Not Found      | Error(*)                         | User or Group with the given ID not found. |
| `/groups/{groupId}/rankings/result`              | GET    | `groupId` (long)              | Path           | 200 OK             | RankingResultGetDTO(*)           | Retrieve the latest calculated ranking result for the group (the winning movie and its average rank). |
|                                                      |        |                               |                | 404 Not Found      | Error(*)                         | Group not found or no ranking result exists for the group yet. |
| `/groups/{groupId}/movies/rankable`              | GET    | `groupId` (long)              | Path           | 200 OK             | List<MovieGetDTO>(*)             | Retrieve the list of movies available for ranking in the group's pool. |
|                                                      |        |                               |                | 404 Not Found      | Error(*)                         | Group not found or group has no movie pool. |
| `/groups/{groupId}/rankings/details`             | GET    | `groupId` (long)              | Path           | 200 OK             | List<MovieAverageRankDTO>(*)     | Retrieve the detailed average ranking results for all movies in the group (each movie with its average rank, sorted by best rank). |
|                                                      |        |                               |                | 404 Not Found      | Error(*)                         | Group not found. |

### 5. Movie Search
