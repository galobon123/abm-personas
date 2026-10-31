package com.abm.people.exceptions;

public class EmailDuplicatedException extends RuntimeException {
    public EmailDuplicatedException() {
        super("Email already exists");
    }
    
}
