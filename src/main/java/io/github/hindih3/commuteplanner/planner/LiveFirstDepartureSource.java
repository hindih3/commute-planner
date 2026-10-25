package io.github.hindih3.commuteplanner.planner;

import java.time.Clock;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * The DepartureSource Planner actually gets: live CTA predictions when you're
 * planning for right now, the schedule for everything else.
 *
 * There are two DepartureSource beans (this one and JdbcDepartureSource), so
 * Spring needs to be told which one to inject into Planner. @Primary says
 * "this one". Setting cta.live.enabled=false skips creating it entirely, and
 * Planner gets the schedule alone.
 */
@Primary
@Component
@ConditionalOnBooleanProperty(name = "cta.live.enabled", matchIfMissing = true)
public class LiveFirstDepartureSource implements DepartureSource {

    private static final Logger log = LoggerFactory.getLogger(LiveFirstDepartureSource.class);

    // CTA predicts about 30 minutes ahead; past this there's nothing live to ask about
    static final int LIVE_WINDOW_MINUTES = 45;
    // Train Tracker's line codes. Every other line is a bus route.
    private static final Set<String> TRAIN_LINES = Set.of("Red", "Blue", "Brn", "G", "Org", "P", "Pink", "Y");

    private final CtaClient cta;
    private final StopRepository stops;
    private final JdbcDepartureSource schedule;
    private final Clock clock;

    public LiveFirstDepartureSource(CtaClient cta, StopRepository stops, JdbcDepartureSource schedule, Clock clock) {
        this.cta = cta;
        this.stops = stops;
        this.schedule = schedule;
        this.clock = clock;
    }

    @Override
    public Optional<LocalTime> next(String line, int stopId, LocalTime after) {
        if (isAboutNow(after)) {
            Optional<LocalTime> live = livePrediction(line, stopId, after);
            if (live.isPresent()) {
                return live;
            }
        }
        // Planning for later, or CTA had nothing useful: fall back to the timetable
        return schedule.next(line, stopId, after);
    }

    // Predictions describe vehicles on the road right now, so they only help
    // when the departure we're looking for is in the next few minutes.
    private boolean isAboutNow(LocalTime after) {
        LocalTime now = LocalTime.now(clock);
        return !after.isBefore(now.minusMinutes(5)) && !after.isAfter(now.plusMinutes(LIVE_WINDOW_MINUTES));
    }

    private Optional<LocalTime> livePrediction(String line, int stopId, LocalTime after) {
        Optional<String> ctaStopId = stops.ctaStopId(stopId);
        if (ctaStopId.isEmpty()) {
            return Optional.empty();
        }
        try {
            List<LocalTime> predicted = TRAIN_LINES.contains(line)
                    ? cta.trainArrivals(line, ctaStopId.get())
                    : cta.busArrivals(line, ctaStopId.get());
            return predicted.stream()
                    .filter(t -> !t.isBefore(after))
                    .min(Comparator.naturalOrder());
        } catch (RestClientException e) {
            // CTA down, network out, bad response: the schedule is still a fine answer
            log.warn("No live CTA data for {} at stop {}, using the schedule: {}", line, stopId, e.getMessage());
            return Optional.empty();
        }
    }
}
