package com.abm.people.dtos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public class PersonDTOs {
    public record PersonRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email String email,
    @Past LocalDate birthDate) {}

    public record PersonResponse(
    UUID id, String name, String lastName,
    String email, LocalDate birthDate, Instant createdAt) {}
}
