package io.github.hindih3.commuteplanner.planner;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Explores every path through the segment tree and returns the ones that
 * reach a leaf (the door) by the deadline, earliest arrival first.
 *
 */
@Component
public class Planner {

    private final DepartureSource departures;
    private final int boardingBufferMinutes;

    // @Value reads planner.boarding-buffer-minutes from application.properties.
    public Planner(DepartureSource departures,
                   @Value("${planner.boarding-buffer-minutes:2}") int boardingBufferMinutes) {
        this.departures = departures;
        this.boardingBufferMinutes = boardingBufferMinutes;
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
                         List<Leg> path, List<Plan> results) {
        // 1. Work out when this segment starts and ends.
        LocalTime departs = clock; // walks start right away
        if (seg.isTransit()) {
            // Only count a departure you'd be at the stop a few minutes early for:
            // buses come early, and a 0-minute connection is a gamble.
            LocalTime readyToBoard = clock.plusMinutes(boardingBufferMinutes);
            Optional<LocalTime> next = departures.next(seg.line(), seg.boardingStopId(), readyToBoard);
            if (next.isEmpty()) {
                return; // no more service today: dead branch
            }
            departs = next.get(); // the wait happens here
        }
        LocalTime arrives = departs.plusMinutes(seg.minutes()); // walk time, or ride time

        // 2. Prune: already too late, so nothing below this can make it.
        if (arrives.isAfter(deadline)) {
            return;
        }

        path.add(new Leg(seg, departs, arrives));

        // 3. Leaf = at the door. Otherwise try every child.
        List<Segment> children = childrenOf.getOrDefault(seg.id(), List.of());
        if (children.isEmpty()) {
            results.add(new Plan(List.copyOf(path), arrives));
        } else {
            for (Segment child : children) {
                explore(child, arrives, deadline, childrenOf, path, results);
            }
        }

        path.removeLast(); // backtrack so the sibling branches start clean
    }
}