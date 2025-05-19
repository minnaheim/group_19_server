package ch.uzh.ifi.hase.soprafs25.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AuthorizationUtil {
    public static String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Remove "Bearer " prefix
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
}