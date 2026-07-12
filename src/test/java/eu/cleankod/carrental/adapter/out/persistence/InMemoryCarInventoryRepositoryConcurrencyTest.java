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
            // given: each thread requests its own distinct period (a different `days` value), all
            // sharing the same start so every pair genuinely overlaps — not literally the same request
            // repeated
            int attempts = 20;
            InMemoryCarInventoryRepository repository =
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.SEDAN, 1));

            // when
            RaceResult result = raceForOverlappingPeriods(repository, CarType.SEDAN, START, attempts);

            // then
            assertThat(result.succeeded()).isEqualTo(1);
            assertThat(result.rejected()).isEqualTo(attempts - 1);
        }

        @Test
        void allowsExactlyTotalUnitsWinnersWhenMoreAttemptsThanCapacityRaceConcurrently() throws Exception {
            // given: each thread requests its own distinct, mutually-overlapping period
            int totalUnits = 3;
            int attempts = 15;
            InMemoryCarInventoryRepository repository =
                    new InMemoryCarInventoryRepository(new CarTypeInventory(CarType.VAN, totalUnits));

            // when
            RaceResult result = raceForOverlappingPeriods(repository, CarType.VAN, START, attempts);

            // then
            assertThat(result.succeeded()).isEqualTo(totalUnits);
            assertThat(result.rejected()).isEqualTo(attempts - totalUnits);
        }

        /**
         * Races {@code attempts} threads, each requesting its own distinct {@link RentalPeriod} — the
         * same {@code start}, a different positive {@code days} per thread — so every pair of periods
         * genuinely overlaps (a shared start always overlaps another positive-duration period starting
         * at that same instant) without every thread submitting a literally identical request.
         */
        private RaceResult raceForOverlappingPeriods(
                InMemoryCarInventoryRepository repository, CarType carType, LocalDateTime start, int attempts)
                throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(attempts);
            CountDownLatch ready = new CountDownLatch(attempts);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger succeeded = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < attempts; i++) {
                    RentalPeriod period = new RentalPeriod(start, i + 1);
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        awaitUninterruptibly(go);
                        try {
                            repository.reserve(carType, period);
                            succeeded.incrementAndGet();
                        } catch (CarUnavailableException e) {
                            rejected.incrementAndGet();
                        }
                    }));
                }
                ready.await();
                go.countDown();
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
