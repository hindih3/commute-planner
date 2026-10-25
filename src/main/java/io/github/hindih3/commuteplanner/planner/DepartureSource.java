package io.github.hindih3.commuteplanner.planner;

import java.time.LocalTime;
import java.util.Optional;

@FunctionalInterface
public interface DepartureSource {
    Optional<LocalTime> next(String line, int stopId, LocalTime after);
}