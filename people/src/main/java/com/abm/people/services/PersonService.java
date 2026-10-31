package com.abm.people.services;

import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import com.abm.people.repositories.PersonRepository;
import com.abm.people.exceptions.EmailDuplicatedException;
import com.abm.people.exceptions.PersonNotFoundException;
import com.abm.people.models.Person;
import com.abm.people.dtos.PersonDTOs.PersonResponse;
import com.abm.people.dtos.PersonDTOs.PersonRequest;



@Service
@Transactional(readOnly = true)
public class PersonService {

    private static final Logger log = LoggerFactory.getLogger(PersonService.class);

    private final PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    public Page<PersonResponse> list(Pageable pageable) {
        return repository.findByActive(true, pageable).map(this::toResponse);
    }

    public PersonResponse get(UUID id) {
        return toResponse(search(id));
    }

    @Transactional
    public PersonResponse create(PersonRequest data) {
        String email = normalize(data.email());
        if (repository.existsByEmail(email)) {
            throw new EmailDuplicatedException();
        }
        Person person = new Person();
        apply(person, data, email);
        person.setActive(true);
        try {
            person = repository.saveAndFlush(person);
        } catch (DataIntegrityViolationException e) {
            throw new EmailDuplicatedException();
        }
        log.info("Person created id={}", person.getId());
        return toResponse(person);
    }

    @Transactional
    public PersonResponse update(UUID id, PersonRequest data) {
        Person person = search(id);
        String email = normalize(data.email());
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new EmailDuplicatedException();
        }
        apply(person, data, email);
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new EmailDuplicatedException();
        }
        log.info("Person updated id={}", id);
        return toResponse(person);
    }

    @Transactional
    public void delete(UUID id) {
        Person person = search(id);
        person.setActive(false);
        log.info("Person deleted id={}", id);
    }

    private Person search(UUID id) {
        return repository.findByIdAndActive(id, true)
                .orElseThrow(() -> new PersonNotFoundException(id));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void apply(Person p, PersonRequest r, String email) {
        p.setName(r.name().trim());
        p.setLastName(r.lastName().trim());
        p.setEmail(email);
        p.setBirthDate(r.birthDate());
    }

    private PersonResponse toResponse(Person p) {
        return new PersonResponse(p.getId(), p.getName(), p.getLastName(),
                p.getEmail(), p.getBirthDate(), p.getCreatedAt());
    }
}
