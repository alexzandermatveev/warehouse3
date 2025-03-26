package distribution_system.webApp.controllers;

import distribution_system.webApp.exceptions.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> someProblem(CustomException exception){
        return new ResponseEntity<>(exception.getMessage(),
                HttpStatus.NOT_FOUND);
    }
}
