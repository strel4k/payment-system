package com.example.personservice.api;

import com.example.dto.person.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PersonsApiIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("person")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.flyway.enabled",      () -> "true");
        r.add("spring.flyway.schemas",      () -> "person");
        r.add("spring.flyway.default-schema", () -> "person");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.jpa.properties.hibernate.default_schema", () -> "person");
        r.add("spring.jpa.properties.org.hibernate.envers.default_schema", () -> "person");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // ------------ helpers ------------

    private String uniqueEmail() {
        return "it_" + System.nanoTime() + "@mail.com";
    }

    // ------------ Создаёт пользователя и возвращает его userId ------------
    private UUID createUser(String email) throws Exception {
        var req = new CreatePersonRequest()
                .email(email)
                .firstName("Test")
                .lastName("User");

        var result = mvc.perform(post("/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                om.readTree(result.getResponse().getContentAsString())
                        .get("userId").asText()
        );
    }

    // ------------ 1. полный CRUD flow без address/individual ------------

    @Test
    void create_get_update_delete_flow() throws Exception {
        String email = uniqueEmail();

        // CREATE
        var create = new CreatePersonRequest()
                .email(email)
                .firstName("Alex")
                .lastName("Doe");

        var createRes = mvc.perform(post("/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Alex"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andReturn();

        UUID userId = UUID.fromString(
                om.readTree(createRes.getResponse().getContentAsString())
                        .get("userId").asText()
        );

        // GET BY ID
        mvc.perform(get("/v1/persons/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value(email));

        // UPDATE
        var upd = new UpdatePersonRequest()
                .firstName("Updated")
                .filled(true);

        mvc.perform(put("/v1/persons/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.filled").value(true));

        // DELETE
        mvc.perform(delete("/v1/persons/{id}", userId))
                .andExpect(status().isNoContent());

        // GET AFTER DELETE → 404
        mvc.perform(get("/v1/persons/{id}", userId))
                .andExpect(status().isNotFound());
    }


    // ------------ 2. создание с address и individual ------------

    @Test
    void create_withAddressAndIndividual_returnsNestedObjects() throws Exception {
        var req = new CreatePersonRequest()
                .email(uniqueEmail())
                .firstName("Nested")
                .lastName("Test")
                .address(new AddressRequest()
                        .address("Ulitsa 42")
                        .zipCode("654321")
                        .city("Novosibirsk")
                        .state("Novosibirskaya Oblast"))
                .individual(new IndividualRequest()
                        .passportNumber("9876 543210")
                        .phoneNumber("+7 800 123 4567")
                        .status("PENDING"));

        mvc.perform(post("/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.address.id").exists())
                .andExpect(jsonPath("$.address.address").value("Ulitsa 42"))
                .andExpect(jsonPath("$.address.zipCode").value("654321"))
                .andExpect(jsonPath("$.address.city").value("Novosibirsk"))
                .andExpect(jsonPath("$.address.state").value("Novosibirskaya Oblast"))
                .andExpect(jsonPath("$.individual.id").exists())
                .andExpect(jsonPath("$.individual.passportNumber").value("9876 543210"))
                .andExpect(jsonPath("$.individual.phoneNumber").value("+7 800 123 4567"))
                .andExpect(jsonPath("$.individual.status").value("PENDING"));
    }

    // ------------ 3. GET by email ------------

    @Test
    void getByEmail_exists_returnsUser() throws Exception {
        String email = uniqueEmail();
        createUser(email);

        mvc.perform(get("/v1/persons/by-email")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void getByEmail_missing_returns404() throws Exception {
        mvc.perform(get("/v1/persons/by-email")
                        .param("email", "nonexistent_" + System.nanoTime() + "@x.com"))
                .andExpect(status().isNotFound());
    }

    // ------------ 4. GET by ID — not found ------------

    @Test
    void getById_nonexistent_returns404() throws Exception {
        mvc.perform(get("/v1/persons/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ------------ 5. duplicate email → 409 ------------

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        createUser(email);

        var dup = new CreatePersonRequest()
                .email(email)
                .firstName("Dup")
                .lastName("User");

        mvc.perform(post("/v1/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    // ------------ 6. update nonexistent → 404 ------------

    @Test
    void update_nonexistent_returns404() throws Exception {
        var req = new UpdatePersonRequest().firstName("X");

        mvc.perform(put("/v1/persons/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ------------ 7. delete nonexistent → 404 ------------

    @Test
    void delete_nonexistent_returns404() throws Exception {
        mvc.perform(delete("/v1/persons/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ------------ 8. update добавляет address к существующему пользователю ------------

    @Test
    void update_addsAddressToExistingUser() throws Exception {
        UUID userId = createUser(uniqueEmail());

        var upd = new UpdatePersonRequest()
                .address(new AddressRequest()
                        .address("New Street 99")
                        .city("Kazan"));

        mvc.perform(put("/v1/persons/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.address").value("New Street 99"))
                .andExpect(jsonPath("$.address.city").value("Kazan"));
    }

    // ------------ 9. update добавляет individual к существующему пользователю ------------

    @Test
    void update_addsIndividualToExistingUser() throws Exception {
        UUID userId = createUser(uniqueEmail());

        var upd = new UpdatePersonRequest()
                .individual(new IndividualRequest()
                        .passportNumber("0000 000001")
                        .phoneNumber("+1 555 000 0000")
                        .status("VERIFIED"));

        mvc.perform(put("/v1/persons/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.individual.passportNumber").value("0000 000001"))
                .andExpect(jsonPath("$.individual.status").value("VERIFIED"));
    }
}