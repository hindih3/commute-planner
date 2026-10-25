package io.github.hindih3.commuteplanner.planner;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Explores every path through the segment tree and returns the ones that
 * reach a leaf (the door) by the deadline, earliest arrival first.
 *
 * Plain Java on purpose: no Spring, no database. Whatever DepartureSource
 * you hand it (a fake in tests, Postgres or the CTA API later) is the only
 * thing that knows about schedules.
 */
public class Planner {

    private final DepartureSource departures;

    public Planner(DepartureSource departures) {
        this.departures = departures;
    }

    public List<Plan> plan(List<Segment> segments, LocalTime start, LocalTime deadline) {
        // Turn the flat rows (like they'll come out of Postgres) into a tree:
        // parentId -> list of children. The row with no parent is the root.
        Map<Integer, List<Segment>> childrenOf = new HashMap<>();
        Segment root = null;
        for (Segment s : segments) {
            if (s.parentId() == null) {
                root = s;
            } else {
                childrenOf.computeIfAbsent(s.parentId(), k -> new ArrayList<>()).add(s);
            }
        }
        if (root == null) {
            throw new IllegalArgumentException("No root segment (one segment needs parentId == null)");
        }

        List<Plan> results = new ArrayList<>();
        explore(root, start, deadline, childrenOf, new ArrayList<>(), results);
        results.sort(Comparator.comparing(Plan::arrival));
        return results;
    }

    private void explore(Segment seg, LocalTime clock, LocalTime deadline,
                         Map<Integer, List<Segment>> childrenOf,
                         List<Segment> path, List<Plan> results) {
        // 1. Advance the clock through this segment.
        if (seg.isTransit()) {
            Optional<LocalTime> departs = departures.next(seg.line(), seg.boardingStop(), clock);
            if (departs.isEmpty()) {
                return; // no more service today: dead branch
            }
            clock = departs.get(); // the wait happens here
        }
        clock = clock.plusMinutes(seg.minutes()); // walk time, or ride time

        // 2. Prune: already too late, so nothing below this can make it.
        if (clock.isAfter(deadline)) {
            return;
        }

        path.add(seg);

        // 3. Leaf = at the door. Otherwise try every child.
        List<Segment> children = childrenOf.getOrDefault(seg.id(), List.of());
        if (children.isEmpty()) {
            results.add(new Plan(List.copyOf(path), clock));
        } else {
            for (Segment child : children) {
                explore(child, clock, deadline, childrenOf, path, results);
            }
        }

        path.removeLast(); // backtrack so the sibling branches start clean
    }
}