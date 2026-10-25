package io.github.hindih3.commuteplanner.planner;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PlannerTest {

    List<Segment> tree = List.of(
            new Segment(1, null, Mode.WALK, "Exit Union Station", 3, null, null),
            new Segment(2, 1,    Mode.WALK, "Walk to campus",     20, null, null),
            new Segment(3, 1,    Mode.WALK, "Walk to bus stop",   4, null, null),
            new Segment(4, 3,    Mode.BUS,  "157 to building",    5, "157", 1)
    );

    List<LocalTime> busTimes = List.of(LocalTime.of(8, 0), LocalTime.of(8, 20));

    // A fake departure source: a lambda, thanks to @FunctionalInterface
    DepartureSource fake = (line, stop, after) ->
            busTimes.stream().filter(t -> !t.isBefore(after)).findFirst();

    Planner planner = new Planner(fake);

    @Test
    void busWinsWhenYouCatchIt() {
        // 7:45 + 3 + 4 = 7:52 at stop, bus 8:00, +5 = 8:05. Walking arrives 8:08.
        List<Plan> plans = planner.plan(tree, LocalTime.of(7, 45), LocalTime.of(9, 0));
        assertThat(plans.getFirst().arrival()).isEqualTo(LocalTime.of(8, 5));
    }

    @Test
    void walkingWinsWhenYouJustMissTheBus() {
        // 7:55 → at stop 8:02, missed it, next bus 8:20 → 8:25. Walking: 8:18.
        List<Plan> plans = planner.plan(tree, LocalTime.of(7, 55), LocalTime.of(9, 0));
        assertThat(plans.getFirst().legs().getLast().description()).isEqualTo("Walk to campus");
    }

    @Test
    void pathsPastTheDeadlineAreDropped() {
        // Deadline 8:20: walking (8:18) survives, the bus (8:25) gets pruned.
        List<Plan> plans = planner.plan(tree, LocalTime.of(7, 55), LocalTime.of(8, 20));
        assertThat(plans).hasSize(1);
    }
}