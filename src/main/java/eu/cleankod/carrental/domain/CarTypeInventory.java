package eu.cleankod.carrental.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The fixed number of units available for a {@link CarType}, and the rule for whether a candidate
 * reservation can be accepted given the reservations already accepted for that type. A candidate is
 * accepted iff the maximum number of reservations simultaneously active at any single instant — across
 * the existing reservations plus the candidate — never exceeds {@code totalUnits}.
 *
 * <p>This is computed with a sweep over each reservation's start (+1) and end-exclusive (-1), not by
 * counting how many existing reservations merely overlap the candidate: two existing reservations can
 * each overlap the candidate without overlapping each other (e.g. one ends before the other begins), in
 * which case a single vehicle can serve both sequentially, freeing capacity for the candidate. Counting
 * overlaps against the candidate alone would miss that and reject a booking a real fleet could still
 * accommodate. This sweep is the exact, provably optimal test for scheduling identical, interchangeable
 * units against a set of time intervals: interval graphs are perfect graphs, so the minimum number of
 * units needed (their chromatic number) equals the maximum number simultaneously active at any instant
 * (their clique number) — {@code O(n log n)}, not a heuristic, and not NP-hard bin-packing.
 *
 * <p>What this does <em>not</em> do: assign a specific vehicle to each reservation. It only proves that
 * a valid assignment could exist, not what it is — the domain has no per-vehicle identity, only a count.
 */
public record CarTypeInventory(CarType carType, int totalUnits) {

    public CarTypeInventory {
        Objects.requireNonNull(carType, "carType must not be null");
        if (totalUnits <= 0) {
            throw new InvalidFleetSizeException(carType, totalUnits);
        }
    }

    public boolean hasCapacityFor(Collection<Reservation> existingReservations, Reservation candidate) {
        List<Event> events = Stream.concat(existingReservations.stream(), Stream.of(candidate))
                .flatMap(reservation -> Stream.of(
                        new Event(reservation.period().start(), 1),
                        new Event(reservation.period().endExclusive(), -1)))
                .sorted(Comparator.comparing(Event::at).thenComparingInt(Event::delta))
                .toList();
        int occupied = 0;
        int maxOccupied = 0;
        for (Event event : events) {
            occupied += event.delta();
            maxOccupied = Math.max(maxOccupied, occupied);
        }
        return maxOccupied <= totalUnits;
    }

    /**
     * One endpoint of a reservation's period: {@code +1} at its start, {@code -1} at its end-exclusive.
     * Sorting by {@code at} then {@code delta} puts ends before starts at the same instant, so a
     * reservation ending exactly when another begins is never counted as simultaneous — matching
     * {@link RentalPeriod#overlaps}'s half-open semantics.
     */
    private record Event(LocalDateTime at, int delta) {
    }
}
