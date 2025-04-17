package ch.uzh.ifi.hase.soprafs25.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GroupNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GroupNotFoundException(String message) {
        super(message);
    }
}
