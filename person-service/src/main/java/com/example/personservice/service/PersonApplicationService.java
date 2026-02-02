package com.example.personservice.service;

import com.example.dto.person.CreatePersonRequest;
import com.example.dto.person.PersonResponse;
import com.example.dto.person.UpdatePersonRequest;
import com.example.dto.person.AddressRequest;
import com.example.dto.person.IndividualRequest;
import com.example.personservice.entity.AddressEntity;
import com.example.personservice.entity.CountryEntity;
import com.example.personservice.entity.IndividualEntity;
import com.example.personservice.entity.UserEntity;
import com.example.personservice.exception.ConflictException;
import com.example.personservice.exception.NotFoundException;
import com.example.personservice.mapper.PersonMapper;
import com.example.personservice.repository.CountryRepository;
import com.example.personservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PersonApplicationService {

    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final PersonMapper mapper;

    public PersonApplicationService(UserRepository userRepository,
                                    CountryRepository countryRepository,
                                    PersonMapper mapper) {
        this.userRepository = userRepository;
        this.countryRepository = countryRepository;
        this.mapper = mapper;
    }

    @Transactional
    public PersonResponse create(CreatePersonRequest req) {
        userRepository.findByEmailIgnoreCase(req.getEmail()).ifPresent(u -> {
            throw new ConflictException("User with email already exists: " + req.getEmail());
        });

        var user = new UserEntity();
        user.setEmail(req.getEmail());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());

        // address (optional)
        if (req.getAddress() != null) {
            user.setAddress(mapAddress(req.getAddress(), new AddressEntity()));
        }

        // individual (optional)
        if (req.getIndividual() != null) {
            var individual = mapIndividual(req.getIndividual(), new IndividualEntity());
            individual.setUser(user);
            user.setIndividual(individual);
        }

        var saved = userRepository.save(user);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PersonResponse getById(UUID id) {
        var u = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        return mapper.toResponse(u);
    }

    @Transactional(readOnly = true)
    public PersonResponse getByEmail(String email) {
        var u = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User not found by email: " + email));
        return mapper.toResponse(u);
    }

    @Transactional
    public PersonResponse update(UUID id, UpdatePersonRequest req) {
        var u = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        if (req.getFirstName() != null) u.setFirstName(req.getFirstName());
        if (req.getLastName() != null) u.setLastName(req.getLastName());
        if (req.getFilled() != null) u.setFilled(req.getFilled());

        if (req.getAddress() != null) {
            var a = Optional.ofNullable(u.getAddress()).orElseGet(() -> {
                var created = new AddressEntity();
                u.setAddress(created);
                return created;
            });
            mapAddress(req.getAddress(), a);
        }

        if (req.getIndividual() != null) {
            var i = Optional.ofNullable(u.getIndividual()).orElseGet(() -> {
                var created = new IndividualEntity();
                created.setUser(u);
                u.setIndividual(created);
                return created;
            });
            mapIndividual(req.getIndividual(), i);
        }

        var saved = userRepository.save(u);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        var u = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        userRepository.delete(u);
    }

    private AddressEntity mapAddress(AddressRequest req, AddressEntity target) {
        if (req.getCountryId() != null) {
            CountryEntity country = countryRepository.findById(req.getCountryId())
                    .orElseThrow(() -> new NotFoundException("Country not found: " + req.getCountryId()));
            target.setCountry(country);
        } else {
            target.setCountry(null);
        }

        target.setAddress(req.getAddress());
        target.setZipCode(req.getZipCode());
        target.setCity(req.getCity());
        target.setState(req.getState());
        return target;
    }

    private IndividualEntity mapIndividual(IndividualRequest req, IndividualEntity target) {
        target.setPassportNumber(req.getPassportNumber());
        target.setPhoneNumber(req.getPhoneNumber());
        target.setStatus(req.getStatus());
        return target;
    }
}