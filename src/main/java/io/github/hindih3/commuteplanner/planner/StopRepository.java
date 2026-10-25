package io.github.hindih3.commuteplanner.planner;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StopRepository {

    private final JdbcClient jdbc;

    public StopRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** CTA's id for one of our stops, which is what CTA's APIs ask for. */
    public Optional<String> ctaStopId(int stopId) {
        return jdbc.sql("SELECT cta_stop_id FROM stop WHERE id = :id AND cta_stop_id IS NOT NULL")
                .param("id", stopId)
                .query(String.class)
                .optional();
    }
}
