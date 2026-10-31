package com.abm.people.exceptions;

import java.util.UUID;

public class PersonNotFoundException extends RuntimeException {
    public PersonNotFoundException(UUID id) {
        super("Person not found with ID: " + id);
    }
    
}
