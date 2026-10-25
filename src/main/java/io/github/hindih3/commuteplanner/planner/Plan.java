package io.github.hindih3.commuteplanner.planner;

import java.time.LocalTime;
import java.util.List;

public record Plan(List<Segment> legs, LocalTime arrival) {}