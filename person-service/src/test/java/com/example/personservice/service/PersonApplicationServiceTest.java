package com.example.personservice.service;

import com.example.dto.person.*;
import com.example.personservice.entity.*;
import com.example.personservice.exception.ConflictException;
import com.example.personservice.exception.NotFoundException;
import com.example.personservice.mapper.PersonMapper;
import com.example.personservice.repository.CountryRepository;
import com.example.personservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonApplicationServiceTest {

    @Mock private UserRepository    userRepository;
    @Mock private CountryRepository countryRepository;
    @Mock private PersonMapper      mapper;

    @InjectMocks
    private PersonApplicationService service;

    private static final UUID USER_ID = UUID.randomUUID();

    private UserEntity stubUser() {
        UserEntity u = new UserEntity();
        u.setId(USER_ID);
        u.setEmail("test@example.com");
        u.setFirstName("Ivan");
        u.setLastName("Petrov");
        u.setFilled(false);
        return u;
    }

    private PersonResponse stubResponse() {
        return new PersonResponse()
                .userId(USER_ID)
                .email("test@example.com")
                .firstName("Ivan")
                .lastName("Petrov")
                .filled(false);
    }

    // ------------ CREATE ------------

    @Test
    void create_happyPath_savesAndReturnsResponse() {
        CreatePersonRequest req = new CreatePersonRequest()
                .email("test@example.com")
                .firstName("Ivan")
                .lastName("Petrov");

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(UserEntity.class)))
                .thenReturn(stubResponse());

        PersonResponse res = service.create(req);

        assertEquals(USER_ID, res.getUserId());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void create_duplicateEmail_throwsConflict() {
        CreatePersonRequest req = new CreatePersonRequest()
                .email("dup@example.com")
                .firstName("A")
                .lastName("B");

        when(userRepository.findByEmailIgnoreCase("dup@example.com"))
                .thenReturn(Optional.of(stubUser()));

        assertThrows(ConflictException.class, () -> service.create(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_withAddress_setsAddressOnUser() {
        AddressRequest addrReq = new AddressRequest()
                .address("Lenina 1")
                .zipCode("123456")
                .city("Moscow")
                .state("Oblast");

        CreatePersonRequest req = new CreatePersonRequest()
                .email("addr@example.com")
                .firstName("A")
                .lastName("B")
                .address(addrReq);

        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(stubResponse());

        service.create(req);

        // Проверяем что save получил entity с address
        verify(userRepository).save(argThat(u ->
                u.getAddress() != null
                        && "Lenina 1".equals(u.getAddress().getAddress())
                        && "Moscow".equals(u.getAddress().getCity())
        ));
    }

    @Test
    void create_withAddressAndCountry_resolvesCountry() {
        CountryEntity country = new CountryEntity();
        country.setId(1);
        country.setName("Russia");

        AddressRequest addrReq = new AddressRequest()
                .countryId(1)
                .address("Street 1")
                .city("SPb");

        CreatePersonRequest req = new CreatePersonRequest()
                .email("c@example.com")
                .firstName("A")
                .lastName("B")
                .address(addrReq);

        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(countryRepository.findById(1)).thenReturn(Optional.of(country));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(stubResponse());

        service.create(req);

        verify(countryRepository).findById(1);
    }

    @Test
    void create_withAddressAndUnknownCountry_throwsNotFound() {
        AddressRequest addrReq = new AddressRequest().countryId(999).address("X");

        CreatePersonRequest req = new CreatePersonRequest()
                .email("x@example.com")
                .firstName("A")
                .lastName("B")
                .address(addrReq);

        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(countryRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(req));
    }

    @Test
    void create_withIndividual_setsIndividualOnUser() {
        IndividualRequest indReq = new IndividualRequest()
                .passportNumber("1234 567890")
                .phoneNumber("+7 999 000 11 22")
                .status("ACTIVE");

        CreatePersonRequest req = new CreatePersonRequest()
                .email("ind@example.com")
                .firstName("A")
                .lastName("B")
                .individual(indReq);

        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(stubResponse());

        service.create(req);

        verify(userRepository).save(argThat(u ->
                u.getIndividual() != null
                        && "1234 567890".equals(u.getIndividual().getPassportNumber())
                        && "ACTIVE".equals(u.getIndividual().getStatus())
        ));
    }

    // ------------ GET BY ID ------------

    @Test
    void getById_exists_returnsResponse() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));
        when(mapper.toResponse(any())).thenReturn(stubResponse());

        PersonResponse res = service.getById(USER_ID);

        assertEquals("test@example.com", res.getEmail());
    }

    @Test
    void getById_missing_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(USER_ID));
    }

    // ------------GET BY EMAIL ------------

    @Test
    void getByEmail_exists_returnsResponse() {
        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(stubUser()));
        when(mapper.toResponse(any())).thenReturn(stubResponse());

        PersonResponse res = service.getByEmail("test@example.com");

        assertEquals(USER_ID, res.getUserId());
    }

    @Test
    void getByEmail_missing_throwsNotFound() {
        when(userRepository.findByEmailIgnoreCase("nope@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByEmail("nope@example.com"));
    }

    // ------------ UPDATE ------------

    @Test
    void update_happyPath_updatesFields() {
        UserEntity existing = stubUser();
        existing.setAddress(null);
        existing.setIndividual(null);

        UpdatePersonRequest req = new UpdatePersonRequest()
                .firstName("Updated")
                .lastName("Name");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(stubResponse());

        service.update(USER_ID, req);

        assertEquals("Updated", existing.getFirstName());
        assertEquals("Name", existing.getLastName());
    }

    @Test
    void update_missing_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.update(USER_ID, new UpdatePersonRequest()));
    }

    @Test
    void update_createsAddressIfMissing() {
        UserEntity existing = stubUser();
        existing.setAddress(null);
        existing.setIndividual(null);

        UpdatePersonRequest req = new UpdatePersonRequest()
                .address(new AddressRequest().city("NewCity"));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(stubResponse());

        service.update(USER_ID, req);

        assertNotNull(existing.getAddress());
        assertEquals("NewCity", existing.getAddress().getCity());
    }

    @Test
    void update_createsIndividualIfMissing() {
        UserEntity existing = stubUser();
        existing.setAddress(null);
        existing.setIndividual(null);

        UpdatePersonRequest req = new UpdatePersonRequest()
                .individual(new IndividualRequest().phoneNumber("+1234"));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(stubResponse());

        service.update(USER_ID, req);

        assertNotNull(existing.getIndividual());
        assertEquals("+1234", existing.getIndividual().getPhoneNumber());
    }

    // ------------ DELETE ------------

    @Test
    void delete_exists_callsRepositoryDelete() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));

        service.delete(USER_ID);

        verify(userRepository).delete(any(UserEntity.class));
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(USER_ID));
    }
}