package ch.uzh.ifi.hase.soprafs25.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // Ensures this exception returns a 400 Bad Request status
public class InvalidRankingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRankingException(String message) {
        super(message);
    }
}
