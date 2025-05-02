package ch.uzh.ifi.hase.soprafs25.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // More specific than @ControllerAdvice for REST APIs
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handler for UserNotFoundException (404)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        log.error("UserNotFoundException caught: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", "")); // Get request path

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Handler for GroupNotFoundException (404)
    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<Object> handleGroupNotFoundException(GroupNotFoundException ex, WebRequest request) {
        log.error("GroupNotFoundException caught: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", "")); // Get request path

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Handler for InvalidRankingException (400)
    @ExceptionHandler(InvalidRankingException.class)
    public ResponseEntity<Object> handleInvalidRankingException(InvalidRankingException ex, WebRequest request) {
        log.error("InvalidRankingException caught: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", "")); // Get request path

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Optional: Generic handler for other ResponseStatusExceptions (like those from Spring Security, etc.)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        log.error("ResponseStatusException caught: Status {}, Reason {}", ex.getStatus(), ex.getReason());
         Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", ex.getStatus().value());
        body.put("error", ex.getStatus().getReasonPhrase());
        body.put("message", ex.getReason()); // Use the reason from the exception
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, ex.getStatus());
    }

    // Optional: Fallback handler for unexpected exceptions (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUncaughtException(Exception ex, WebRequest request) {
        log.error("Unhandled exception caught: {}", ex.getMessage(), ex); // Log stack trace for unexpected errors
         Map<String, Object> body = new HashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please try again later."); // Generic message
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
