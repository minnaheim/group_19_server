package ch.uzh.ifi.hase.soprafs24.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SearchValidationException extends RuntimeException {
    public SearchValidationException(String message) {
        super(message);
    }
}