package io.github.hindih3.commuteplanner.planner;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads the segment table and turns each row into a Segment record.
 *
 * Spring creates one of these at startup (because of @Repository) and hands
 * the constructor a ready-made JdbcClient, built from spring.datasource.*.
 */
@Repository
public class SegmentRepository {

    private final JdbcClient jdbc;

    public SegmentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** One commute's tree. Planner needs exactly one root, so never load both at once. */
    public List<Segment> findByCommute(Commute commute) {
        return jdbc.sql("""
            SELECT id, parent_id, mode, description, minutes, line, boarding_stop_id
            FROM segment
            WHERE commute = :commute
            ORDER BY id
            """)
                .param("commute", commute.name())
                // Called once per row: copy each column into the matching Segment field.
                .query((row, rowNum) -> new Segment(
                        row.getInt("id"),
                        row.getObject("parent_id", Integer.class),        // getInt would turn NULL into 0
                        Mode.valueOf(row.getString("mode")),              // "BUS" -> Mode.BUS
                        row.getString("description"),
                        row.getInt("minutes"),
                        row.getString("line"),
                        row.getObject("boarding_stop_id", Integer.class)  // NULL for walks
                ))
                .list();
    }
}
