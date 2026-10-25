package io.github.hindih3.commuteplanner.planner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

// Runs against the seed data in V1__init.sql.
@SpringBootTest
class JdbcDepartureSourceTest {

    @Autowired
    JdbcDepartureSource departures;

    @Autowired
    SegmentRepository segments;

    @Test
    void findsTheNextDeparture() {
        // 157 at Clinton leaves 7:40, 8:00, 8:20
        assertThat(departures.next("157", 1, LocalTime.of(7, 45))).contains(LocalTime.of(8, 0));
    }

    @Test
    void aBusLeavingRightNowCounts() {
        assertThat(departures.next("157", 1, LocalTime.of(8, 0))).contains(LocalTime.of(8, 0));
    }

    @Test
    void emptyAfterTheLastBus() {
        assertThat(departures.next("157", 1, LocalTime.of(8, 21))).isEqualTo(Optional.empty());
    }

    @Test
    void sameLineDifferentStopHasDifferentTimes() {
        // The 8 stops at Halsted & Jackson (2) and UIC-Halsted (4) on different schedules
        assertThat(departures.next("8", 2, LocalTime.of(7, 51))).contains(LocalTime.of(8, 2));
        assertThat(departures.next("8", 4, LocalTime.of(7, 51))).contains(LocalTime.of(7, 55));
    }

    @Test
    void plannerRunsOnDatabaseData() {
        // Leaving at 7:30, the 157 (catching the 7:40) is the fastest of all 5 routes
        Planner planner = new Planner(departures);
        List<Plan> plans = planner.plan(segments.findAll(), LocalTime.of(7, 30), LocalTime.of(8, 30));

        assertThat(plans).hasSize(5);
        assertThat(plans.getFirst().arrival()).isEqualTo(LocalTime.of(7, 48));
        assertThat(plans.getFirst().legs().getLast().description()).isEqualTo("157 to building");
    }
}
