package com.abm.people.exceptions;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.abm.people.dtos.FieldErrorDTOs.FieldErrorDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PersonNotFoundException.class)
    public ProblemDetail handlePersonNotFound(PersonNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EmailDuplicatedException.class)
    public ProblemDetail handleEmailDuplicate(EmailDuplicatedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");

        problem.setProperty("errors", e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorDTO(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList()));
        return problem;
    }
}
