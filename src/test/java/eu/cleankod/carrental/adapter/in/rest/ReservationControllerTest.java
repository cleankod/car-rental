package eu.cleankod.carrental.adapter.in.rest;

import eu.cleankod.carrental.domain.CarType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real Spring context end-to-end (HTTP -> controller -> use case -> real in-memory
 * repository) — no mocks. Fleet sizes are fixed to 1 unit per type so each nested test can reason about
 * capacity exhaustion without depending on the production defaults in application.yml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
        "car-rental.fleet.sedan-units=1",
        "car-rental.fleet.suv-units=1",
        "car-rental.fleet.van-units=1"
})
class ReservationControllerTest {

    private static final String PATH = "/api/v1/reservations";
    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    class Reserve {

        @Test
        void returnsCreatedWithTheReservationWhenACarIsAvailable() {
            // given
            ReservationRequest request = new ReservationRequest(CarType.SEDAN, START, 3);

            // when
            ResponseEntity<ReservationResponse> response =
                    restTemplate.postForEntity(PATH, request, ReservationResponse.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ReservationResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.carType()).isEqualTo(CarType.SEDAN);
            assertThat(body.days()).isEqualTo(3);
            assertThat(body.id()).isNotNull();
        }

        @Test
        void returnsConflictWhenNoUnitOfThatTypeRemainsAvailable() {
            // given: exhaust the single configured SUV unit
            ReservationRequest request = new ReservationRequest(CarType.SUV, START.plusDays(30), 2);
            ResponseEntity<ReservationResponse> firstResponse =
                    restTemplate.postForEntity(PATH, request, ReservationResponse.class);
            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // when
            ResponseEntity<ErrorResponse> secondResponse =
                    restTemplate.postForEntity(PATH, request, ErrorResponse.class);

            // then
            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(secondResponse.getBody()).isNotNull();
            assertThat(secondResponse.getBody().code()).isEqualTo("CAR_UNAVAILABLE");
        }

        @Test
        void returnsBadRequestWhenDaysIsNotPositive() {
            // given
            ReservationRequest request = new ReservationRequest(CarType.VAN, START, 0);

            // when
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(PATH, request, ErrorResponse.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        }

        @Test
        void returnsBadRequestWhenCarTypeIsMissing() {
            // given
            Map<String, Object> request = Map.of("start", START.toString(), "days", 2);

            // when
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(PATH, request, ErrorResponse.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        }

        @Test
        void returnsBadRequestWhenCarTypeIsNotARecognizedValue() {
            // given
            Map<String, Object> request = Map.of("carType", "HATCHBACK", "start", START.toString(), "days", 2);

            // when
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(PATH, request, ErrorResponse.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("MALFORMED_REQUEST");
        }
    }
}
