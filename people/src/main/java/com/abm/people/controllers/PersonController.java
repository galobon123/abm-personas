package com.abm.people.controllers;

import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.abm.people.services.PersonService;

import jakarta.validation.Valid;

import com.abm.people.dtos.PersonDTOs.PersonRequest;
import com.abm.people.dtos.PersonDTOs.PersonResponse;

@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final PersonService service;
    public PersonController(PersonService service) {
        this.service = service;
    }

    @GetMapping
    public Page<PersonResponse> getPeoplePage(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public PersonResponse getPerson(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<PersonResponse> createPerson(@Valid @RequestBody PersonRequest data) {
        PersonResponse created = service.create(data);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public PersonResponse updatePerson(@PathVariable UUID id, @Valid @RequestBody PersonRequest data) {
        return service.update(id, data);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePerson(@PathVariable UUID id) {
        service.delete(id);
    }
}
