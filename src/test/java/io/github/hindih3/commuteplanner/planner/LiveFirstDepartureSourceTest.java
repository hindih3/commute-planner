package io.github.hindih3.commuteplanner.planner;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Plain unit test, no Spring: Mockito stands in for CTA, the stop table and the
// schedule, and the clock is frozen at 7:30 on a Monday.
class LiveFirstDepartureSourceTest {

    ZoneId zone = ZoneId.systemDefault();
    Clock sevenThirty = Clock.fixed(LocalDateTime.of(2026, 10, 26, 7, 30).atZone(zone).toInstant(), zone);

    CtaClient cta = mock(CtaClient.class);
    StopRepository stops = mock(StopRepository.class);
    JdbcDepartureSource schedule = mock(JdbcDepartureSource.class);

    LiveFirstDepartureSource source = new LiveFirstDepartureSource(cta, stops, schedule, sevenThirty);

    @Test
    void usesThePredictionWhenPlanningForNow() {
        when(stops.ctaStopId(1)).thenReturn(Optional.of("6361"));
        when(cta.busArrivals("157", "6361"))
                .thenReturn(List.of(LocalTime.of(7, 33), LocalTime.of(7, 41), LocalTime.of(7, 55)));

        // The 7:33 bus already left by the time we're ready at 7:37; the next one is predicted for 7:41
        assertThat(source.next("157", 1, LocalTime.of(7, 37))).contains(LocalTime.of(7, 41));
        verifyNoInteractions(schedule);
    }

    @Test
    void trainLinesAskTrainTracker() {
        when(stops.ctaStopId(3)).thenReturn(Optional.of("30085"));
        when(cta.trainArrivals("Blue", "30085")).thenReturn(List.of(LocalTime.of(7, 44)));

        assertThat(source.next("Blue", 3, LocalTime.of(7, 39))).contains(LocalTime.of(7, 44));
    }

    @Test
    void fallsBackToTheScheduleWhenNoPredictionIsLateEnough() {
        when(stops.ctaStopId(1)).thenReturn(Optional.of("6361"));
        when(cta.busArrivals("157", "6361")).thenReturn(List.of(LocalTime.of(7, 33)));
        when(schedule.next("157", 1, LocalTime.of(7, 37))).thenReturn(Optional.of(LocalTime.of(7, 43)));

        assertThat(source.next("157", 1, LocalTime.of(7, 37))).contains(LocalTime.of(7, 43));
    }

    @Test
    void planningForLaterUsesTheScheduleWithoutAskingCta() {
        when(schedule.next("157", 1, LocalTime.of(17, 0))).thenReturn(Optional.of(LocalTime.of(17, 4)));

        assertThat(source.next("157", 1, LocalTime.of(17, 0))).contains(LocalTime.of(17, 4));
        verifyNoInteractions(cta);
    }

    @Test
    void ctaFailureFallsBackToTheSchedule() {
        when(stops.ctaStopId(1)).thenReturn(Optional.of("6361"));
        when(cta.busArrivals("157", "6361")).thenThrow(new RestClientException("CTA is down"));
        when(schedule.next("157", 1, LocalTime.of(7, 37))).thenReturn(Optional.of(LocalTime.of(7, 43)));

        assertThat(source.next("157", 1, LocalTime.of(7, 37))).contains(LocalTime.of(7, 43));
    }
}
