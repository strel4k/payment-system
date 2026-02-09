package com.example.individualsapi.client;

import com.example.dto.person.CreatePersonRequest;
import com.example.dto.person.PersonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonServiceClient {

    private final WebClient personServiceWebClient;
    private final PersonServiceProperties props;

    public Mono<PersonResponse> createPerson(CreatePersonRequest request) {
        String url = props.getBaseUrl() + "/v1/persons";

        log.info("Creating person in person-service for email: {}", request.getEmail());

        return personServiceWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PersonResponse.class)
                .doOnSuccess(response -> log.info("Person created successfully with userId: {}", response.getUserId()))
                .doOnError(error -> log.error("Failed to create person for email: {}", request.getEmail(), error));
    }

    public Mono<PersonResponse> getPersonByEmail(String email) {
        String url = props.getBaseUrl() + "/v1/persons/by-email?email=" + email;

        log.debug("Fetching person by email: {}", email);

        return personServiceWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(PersonResponse.class)
                .doOnSuccess(response -> log.debug("Person fetched successfully: {}", response.getUserId()))
                .doOnError(error -> log.error("Failed to fetch person by email: {}", email, error));
    }

    public Mono<Void> deletePerson(UUID userId) {
        String url = props.getBaseUrl() + "/v1/persons/" + userId;

        log.info("Deleting person from person-service with userId: {}", userId);

        return personServiceWebClient.delete()
                .uri(url)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Person deleted successfully with userId: {}", userId))
                .doOnError(error -> log.error("Failed to delete person with userId: {}", userId, error));
    }
}