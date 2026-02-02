package com.example.personservice.mapper;

import com.example.dto.person.AddressResponse;
import com.example.dto.person.IndividualResponse;
import com.example.dto.person.PersonResponse;
import com.example.personservice.entity.AddressEntity;
import com.example.personservice.entity.CountryEntity;
import com.example.personservice.entity.IndividualEntity;
import com.example.personservice.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersonMapperTest {

    private PersonMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PersonMapper();
    }

    // ------------ полный объект: user + address (с country) + individual ------------

    @Test
    void toResponse_fullObject_mapsAllFields() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID indivId = UUID.randomUUID();

        CountryEntity country = new CountryEntity();
        country.setId(42);
        country.setName("Russia");

        AddressEntity address = new AddressEntity();
        address.setId(addressId);
        address.setCountry(country);
        address.setAddress("Lenina 1");
        address.setZipCode("123456");
        address.setCity("Moscow");
        address.setState("Moscow Oblast");

        IndividualEntity individual = new IndividualEntity();
        individual.setId(indivId);
        individual.setPassportNumber("1234 567890");
        individual.setPhoneNumber("+7 999 000 11 22");
        individual.setStatus("ACTIVE");

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFirstName("Ivan");
        user.setLastName("Petrov");
        user.setFilled(true);
        user.setAddress(address);
        user.setIndividual(individual);

        PersonResponse res = mapper.toResponse(user);

        // user
        assertEquals(userId, res.getUserId());
        assertEquals("test@example.com", res.getEmail());
        assertEquals("Ivan", res.getFirstName());
        assertEquals("Petrov", res.getLastName());
        assertTrue(res.getFilled());

        // address
        assertNotNull(res.getAddress());
        assertEquals(addressId, res.getAddress().getId());
        assertEquals("Lenina 1", res.getAddress().getAddress());
        assertEquals("123456", res.getAddress().getZipCode());
        assertEquals("Moscow", res.getAddress().getCity());
        assertEquals("Moscow Oblast", res.getAddress().getState());

        // individual
        assertNotNull(res.getIndividual());
        assertEquals(indivId, res.getIndividual().getId());
        assertEquals("1234 567890", res.getIndividual().getPassportNumber());
        assertEquals("+7 999 000 11 22", res.getIndividual().getPhoneNumber());
        assertEquals("ACTIVE", res.getIndividual().getStatus());
    }

    // ------------ countryId маппится когда country != null ------------

    @Test
    void toResponse_addressWithCountry_mapsCountryId() {
        CountryEntity country = new CountryEntity();
        country.setId(7);

        AddressEntity address = new AddressEntity();
        address.setId(UUID.randomUUID());
        address.setCountry(country);
        address.setCity("Test");

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("a@b.com");
        user.setFirstName("A");
        user.setLastName("B");
        user.setFilled(false);
        user.setAddress(address);
        user.setIndividual(null);

        PersonResponse res = mapper.toResponse(user);

        assertNotNull(res.getAddress());
        assertEquals(7, res.getAddress().getCountryId());
    }

    @Test
    void toResponse_nullAddress_returnsNullAddress() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("a@b.com");
        user.setFirstName("X");
        user.setLastName("Y");
        user.setFilled(false);
        user.setAddress(null);
        user.setIndividual(null);

        PersonResponse res = mapper.toResponse(user);

        assertNull(res.getAddress());
    }

    // ------------ null individual ------------

    @Test
    void toResponse_nullIndividual_returnsNullIndividual() {
        AddressEntity address = new AddressEntity();
        address.setId(UUID.randomUUID());
        address.setAddress("Street 2");
        address.setCountry(null);  // country может быть null

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("a@b.com");
        user.setFirstName("X");
        user.setLastName("Y");
        user.setFilled(false);
        user.setAddress(address);
        user.setIndividual(null);

        PersonResponse res = mapper.toResponse(user);

        assertNotNull(res.getAddress());
        assertNull(res.getIndividual());
    }

    // ------------ address без country ------------

    @Test
    void toResponse_addressWithoutCountry_noCountryIdInResponse() {
        AddressEntity address = new AddressEntity();
        address.setId(UUID.randomUUID());
        address.setAddress("Street 3");
        address.setCountry(null);
        address.setCity("SPb");

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("a@b.com");
        user.setFirstName("A");
        user.setLastName("B");
        user.setFilled(false);
        user.setAddress(address);
        user.setIndividual(null);

        PersonResponse res = mapper.toResponse(user);

        assertNotNull(res.getAddress());
        assertNull(res.getAddress().getCountryId());
        assertEquals("SPb", res.getAddress().getCity());
    }

    // ------------ filled == null ------------

    @Test
    void toResponse_filledNull_responseFilled_isNull() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("a@b.com");
        user.setFirstName("A");
        user.setLastName("B");
        user.setFilled(null);
        user.setAddress(null);
        user.setIndividual(null);

        PersonResponse res = mapper.toResponse(user);

        assertNull(res.getFilled());
    }
}