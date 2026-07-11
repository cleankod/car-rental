package eu.cleankod.carrental.adapter.out.persistence;

import eu.cleankod.carrental.domain.CarType;
import eu.cleankod.carrental.domain.CarTypeInventory;
import eu.cleankod.carrental.domain.CarUnavailableException;
import eu.cleankod.carrental.domain.RentalPeriod;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the atomic-allocation requirement directly, by racing many threads for the same car type
 * rather than only inspecting the locking code — see
 * {@code docs/decisions/0002-use-per-car-type-locking-for-atomic-allocation.md}.
 */
class InMemoryCarInventoryRepositoryConcurrencyTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Nested
    class Reserve {

        @Test
        void allowsExactlyOneWinnerWhenConcurrentAttemptsRaceForTheLastUnit() throws Exception {
            // given
            int attempts = 20;
            InMemoryCarInventoryRepository repository =
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.SEDAN, 1));
            RentalPeriod period = new RentalPeriod(START, 3);

            // when
            RaceResult result = raceForTheSamePeriod(repository, CarType.SEDAN, period, attempts);

            // then
            assertThat(result.succeeded()).isEqualTo(1);
            assertThat(result.rejected()).isEqualTo(attempts - 1);
        }

        @Test
        void allowsExactlyTotalUnitsWinnersWhenMoreAttemptsThanCapacityRaceConcurrently() throws Exception {
            // given
            int totalUnits = 3;
            int attempts = 15;
            InMemoryCarInventoryRepository repository =
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.VAN, totalUnits));
            RentalPeriod period = new RentalPeriod(START, 3);

            // when
            RaceResult result = raceForTheSamePeriod(repository, CarType.VAN, period, attempts);

            // then
            assertThat(result.succeeded()).isEqualTo(totalUnits);
            assertThat(result.rejected()).isEqualTo(attempts - totalUnits);
        }

        private RaceResult raceForTheSamePeriod(
                InMemoryCarInventoryRepository repository, CarType carType, RentalPeriod period, int attempts)
                throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(attempts);
            CountDownLatch ready = new CountDownLatch(attempts);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger succeeded = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < attempts; i++) {
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        awaitUninterruptibly(start);
                        try {
                            repository.reserve(carType, period);
                            succeeded.incrementAndGet();
                        } catch (CarUnavailableException e) {
                            rejected.incrementAndGet();
                        }
                    }));
                }
                ready.await();
                start.countDown();
                for (Future<?> future : futures) {
                    future.get();
                }
            } finally {
                executor.shutdown();
            }
            return new RaceResult(succeeded.get(), rejected.get());
        }

        private void awaitUninterruptibly(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private record RaceResult(int succeeded, int rejected) {
        }
    }
}
