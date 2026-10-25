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

    // No boarding buffer, so the times below are exact
    Planner planner = new Planner(fake, 0);

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
        assertThat(plans.getFirst().legs().getLast().segment().description()).isEqualTo("Walk to campus");
    }

    @Test
    void legsShowTheWaitForTheBus() {
        // At the stop 7:52, but the bus leaves 8:00: the 8-minute wait sits between the two legs.
        List<Leg> legs = planner.plan(tree, LocalTime.of(7, 45), LocalTime.of(9, 0)).getFirst().legs();
        Leg walk = legs.get(1);
        Leg bus = legs.get(2);

        assertThat(walk.arrives()).isEqualTo(LocalTime.of(7, 52));
        assertThat(bus.departs()).isEqualTo(LocalTime.of(8, 0));
        assertThat(bus.arrives()).isEqualTo(LocalTime.of(8, 5));
    }

    @Test
    void bufferSkipsABusYouWouldOnlyJustMake() {
        // 7:52 + 3 + 4 = 7:59 at the stop, bus at 8:00. Walking arrives 8:15.
        LocalTime start = LocalTime.of(7, 52);

        // Without a buffer, the 1-minute connection counts: bus arrives 8:05
        assertThat(planner.plan(tree, start, LocalTime.of(9, 0)).getFirst().arrival())
                .isEqualTo(LocalTime.of(8, 5));

        // With 2 minutes, you need to be there by 7:58, so the next bus is 8:20 (8:25) and walking wins
        List<Plan> buffered = new Planner(fake, 2).plan(tree, start, LocalTime.of(9, 0));
        assertThat(buffered.getFirst().legs().getLast().segment().description()).isEqualTo("Walk to campus");
        assertThat(buffered.getLast().arrival()).isEqualTo(LocalTime.of(8, 25));
    }

    @Test
    void pathsPastTheDeadlineAreDropped() {
        // Deadline 8:20: walking (8:18) survives, the bus (8:25) gets pruned.
        List<Plan> plans = planner.plan(tree, LocalTime.of(7, 55), LocalTime.of(8, 20));
        assertThat(plans).hasSize(1);
    }
}