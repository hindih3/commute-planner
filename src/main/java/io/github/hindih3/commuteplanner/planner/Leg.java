package io.github.hindih3.commuteplanner.planner;

import java.time.LocalTime;

/**
 * One step of a plan: a segment plus when you start it and when you finish it.
 * For a bus or train, departs is when the vehicle leaves, so any gap after the
 * previous leg's arrives is time spent waiting at the stop.
 */
public record Leg(Segment segment, LocalTime departs, LocalTime arrives) {}
