package com.example.personservice.api;

import com.example.dto.person.CreatePersonRequest;
import com.example.dto.person.PersonResponse;
import com.example.dto.person.UpdatePersonRequest;
import com.example.personservice.service.PersonApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
public class PersonsApiController implements PersonsApi {

    private final PersonApplicationService service;

    public PersonsApiController(PersonApplicationService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<PersonResponse> createPerson(@Valid CreatePersonRequest createPersonRequest) {
        PersonResponse created = service.create(createPersonRequest);
        return ResponseEntity
                .created(URI.create("/v1/persons/" + created.getUserId()))
                .body(created);
    }

    @Override
    public ResponseEntity<PersonResponse> getPersonById(UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Override
    public ResponseEntity<PersonResponse> getPersonByEmail(String email) {
        return ResponseEntity.ok(service.getByEmail(email));
    }

    @Override
    public ResponseEntity<PersonResponse> updatePerson(UUID id, @Valid UpdatePersonRequest updatePersonRequest) {
        return ResponseEntity.ok(service.update(id, updatePersonRequest));
    }

    @Override
    public ResponseEntity<Void> deletePerson(UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}