package eu.cleankod.carrental.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RentalPeriodTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Nested
    class Constructor {

        @Test
        void rejectsZeroDays() {
            // given / when
            var thrown = catchThrowable(() -> new RentalPeriod(START, 0));

            // then
            assertThat(thrown).isInstanceOf(InvalidRentalPeriodException.class)
                    .hasMessage("days must be positive for period starting at " + START + ", was: 0");
        }

        @Test
        void rejectsNegativeDays() {
            // given / when
            var thrown = catchThrowable(() -> new RentalPeriod(START, -1));

            // then
            assertThat(thrown).isInstanceOf(InvalidRentalPeriodException.class)
                    .hasMessage("days must be positive for period starting at " + START + ", was: -1");
        }

        @Test
        void rejectsNullStart() {
            // given / when
            var thrown = catchThrowable(() -> new RentalPeriod(null, 1));

            // then: a required field being null is a precondition, not a business-rule violation —
            // Objects.requireNonNull, not a custom domain exception
            assertThat(thrown).isInstanceOf(NullPointerException.class)
                    .hasMessage("start must not be null");
        }
    }

    @Nested
    class EndExclusive {

        @Test
        void isStartPlusDays() {
            // given
            var period = new RentalPeriod(START, 3);

            // when
            var end = period.endExclusive();

            // then
            assertThat(end).isEqualTo(START.plusDays(3));
        }
    }

    @Nested
    class Overlaps {

        @Test
        void isTrueWhenPeriodsGenuinelyOverlap() {
            // given
            var first = new RentalPeriod(START, 5);
            var second = new RentalPeriod(START.plusDays(2), 5);

            // when / then
            assertThat(first.overlaps(second)).isTrue();
            assertThat(second.overlaps(first)).isTrue();
        }

        @Test
        void isFalseWhenPeriodsAreClearlySeparate() {
            // given
            var first = new RentalPeriod(START, 2);
            var second = new RentalPeriod(START.plusDays(10), 2);

            // when / then
            assertThat(first.overlaps(second)).isFalse();
            assertThat(second.overlaps(first)).isFalse();
        }

        @Test
        void isFalseWhenOnePeriodEndsExactlyWhenTheOtherStarts() {
            // given: back-to-back reservations of the same car are allowed, not treated as overlapping
            var first = new RentalPeriod(START, 3);
            var second = new RentalPeriod(first.endExclusive(), 2);

            // when / then
            assertThat(first.overlaps(second)).isFalse();
            assertThat(second.overlaps(first)).isFalse();
        }
    }
}
