package io.github.hindih3.commuteplanner.planner;

public record Segment(
        int id,
        Integer parentId,     // null for root
        Mode mode,            // walk / bus / train
        String description,
        int minutes,          // functions as walk time or ride time for transit
        String line,          // "157", "Blue"; null for walks
        Integer boardingStopId   // null for walks
) {
    public boolean isTransit() {
        return mode != Mode.WALK;
    }
}