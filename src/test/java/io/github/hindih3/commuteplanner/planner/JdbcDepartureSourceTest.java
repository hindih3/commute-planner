package io.github.hindih3.commuteplanner.planner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

// Runs against the CTA weekday schedule loaded by V2__cta_weekday_schedule.sql.
// If that schedule is replaced, the expected times here change with it.
@SpringBootTest
class JdbcDepartureSourceTest {

    @Autowired
    JdbcDepartureSource departures;

    @Autowired
    SegmentRepository segments;

    @Test
    void findsTheNextDeparture() {
        // 157 at Clinton & Jackson leaves ... 7:35, 7:43, 7:53, 8:04 ...
        assertThat(departures.next("157", 1, LocalTime.of(7, 45))).contains(LocalTime.of(7, 53));
    }

    @Test
    void aBusLeavingRightNowCounts() {
        assertThat(departures.next("157", 1, LocalTime.of(7, 53))).contains(LocalTime.of(7, 53));
    }

    @Test
    void emptyAfterTheLastBus() {
        // The last weekday 157 leaves at 21:54
        assertThat(departures.next("157", 1, LocalTime.of(21, 55))).isEqualTo(Optional.empty());
    }

    @Test
    void sameLineDifferentStopHasDifferentTimes() {
        // The 8 stops at Halsted & Jackson (2) and UIC-Halsted (4) on different schedules
        assertThat(departures.next("8", 2, LocalTime.of(7, 51))).contains(LocalTime.of(7, 53));
        assertThat(departures.next("8", 4, LocalTime.of(7, 51))).contains(LocalTime.of(7, 54));
    }

    @Test
    void returnTripStopsHaveTheirOwnSchedule() {
        // 157 eastbound at Taylor & Halsted leaves ... 16:52, 17:05, 17:18 ...
        assertThat(departures.next("157", 5, LocalTime.of(17, 0))).contains(LocalTime.of(17, 5));
    }

    @Test
    void plannerRunsOnDatabaseData() {
        // Leaving at 7:30 you reach the 157 stop at 7:35, just as one leaves:
        // the fastest of all 5 routes, arriving 7:43
        Planner planner = new Planner(departures, 0); // no buffer: the raw schedule
        List<Plan> plans = planner.plan(segments.findByCommute(Commute.TO_CAMPUS), LocalTime.of(7, 30), LocalTime.of(8, 30));

        assertThat(plans).hasSize(5);
        assertThat(plans.getFirst().arrival()).isEqualTo(LocalTime.of(7, 43));
        assertThat(plans.getFirst().legs().getLast().segment().description()).isEqualTo("157 to building");
    }
}
