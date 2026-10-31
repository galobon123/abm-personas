package com.abm.people.repositories;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abm.people.models.Person;

public interface PersonRepository extends JpaRepository<Person, UUID>{
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    Page<Person> findByActive(boolean isActive, Pageable pageable);
    Optional<Person> findByIdAndActive(UUID id, boolean isActive);
}