package ch.uzh.ifi.hase.soprafs25.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AuthorizationUtilTest {

    @Test
    void extractToken_validHeader_returnsToken() {
        // Arrange
        String authHeader = "Bearer myToken123";

        // Act
        String token = AuthorizationUtil.extractToken(authHeader);

        // Assert
        assertEquals("myToken123", token);
    }

    @Test
    void extractToken_nullHeader_throwsException() {
        // Arrange
        String authHeader = null;

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> AuthorizationUtil.extractToken(authHeader)
        );

        assertEquals("Invalid Authorization header", exception.getReason());    }

    @Test
    void extractToken_headerWithoutBearer_throwsException() {
        // Arrange
        String authHeader = "Token myToken123";

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> AuthorizationUtil.extractToken(authHeader)
        );

        assertEquals("Invalid Authorization header", exception.getReason());    }

    @Test
    void extractToken_emptyHeader_throwsException() {
        // Arrange
        String authHeader = "";

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> AuthorizationUtil.extractToken(authHeader)
        );

        assertEquals("Invalid Authorization header", exception.getReason());    }

    @Test
    void extractToken_headerWithOnlyBearer_returnsEmptyString() {
        // Arrange
        String authHeader = "Bearer ";

        // Act
        String token = AuthorizationUtil.extractToken(authHeader);

        // Assert
        assertEquals("", token);
    }
}