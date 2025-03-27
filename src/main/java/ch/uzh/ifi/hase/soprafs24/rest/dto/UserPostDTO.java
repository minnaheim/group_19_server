package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserPostDTO {

  private int id;
  private String username;
  private String email;
  private String password;
  
  // private String bio;
  // private List<String> preferences;
  // private List<Movie> watchlist;
  // private List<Movie> watchedMovies;

  public int getId() {
    return id;
}

  public void setId(int id) {
      this.id = id;
  }

  public String getUsername() {
      return username;
  }

  public void setUsername(String username) {
      this.username = username;
  }

  public String getEmail() {
      return email;
  }

  public void setEmail(String email) {
      this.email = email;
  }

  public String getPassword() {
      return password;
  }

  public void setPassword(String password) {
      this.password = password;
  }

}