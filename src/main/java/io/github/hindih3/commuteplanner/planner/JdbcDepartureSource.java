package io.github.hindih3.commuteplanner.planner;

import java.time.LocalTime;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The real DepartureSource: looks up scheduled times in the departure table.
 *
 * Weekday schedules only, on purpose: the app is only used on weekdays, so
 * day_type is fixed in the query instead of being passed through Planner.
 */
@Repository
public class JdbcDepartureSource implements DepartureSource {

    private final JdbcClient jdbc;

    public JdbcDepartureSource(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LocalTime> next(String line, int stopId, LocalTime after) {
        return jdbc.sql("""
            SELECT departs_at FROM departure
            WHERE line = :line AND stop_id = :stopId
              AND day_type = 'WEEKDAY' AND departs_at >= :after
            ORDER BY departs_at
            LIMIT 1
            """)
                // Fill the :placeholders. The driver sends these separately from
                // the SQL text, so a value can never be run as SQL.
                .param("line", line)
                .param("stopId", stopId)
                .param("after", after)
                .query(LocalTime.class)
                .optional(); // no row (no more service today) -> Optional.empty()
    }
}
