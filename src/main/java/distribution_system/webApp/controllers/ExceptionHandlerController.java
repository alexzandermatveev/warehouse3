package distribution_system.webApp.controllers;

import distribution_system.webApp.exceptions.CustomException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class ExceptionHandlerController {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> someProblem(CustomException exception){
        log.debug(exception.getMessage());
        return new ResponseEntity<>(exception.getMessage(),
                HttpStatus.NOT_FOUND);
    }
}
