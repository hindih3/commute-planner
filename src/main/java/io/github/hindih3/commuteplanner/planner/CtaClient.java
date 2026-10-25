package io.github.hindih3.commuteplanner.planner;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to CTA's Bus Tracker and Train Tracker APIs. Both return predictions
 * only for vehicles already on their way, roughly the next 30 minutes.
 */
@Component
public class CtaClient {

    private static final Logger log = LoggerFactory.getLogger(CtaClient.class);
    private static final DateTimeFormatter BUS_TIME = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm");

    private final RestClient http;
    private final String busKey;
    private final String trainKey;

    // Spring Boot hands over a RestClient.Builder already set up with its Jackson JSON support.
    public CtaClient(RestClient.Builder builder,
                     @Value("${cta.bus-tracker-key}") String busKey,
                     @Value("${cta.train-tracker-key}") String trainKey) {
        this.http = builder.build();
        this.busKey = busKey;
        this.trainKey = trainKey;
    }

    /** Predicted arrivals of a bus route at a CTA bus stop. Empty when nothing is coming. */
    public List<LocalTime> busArrivals(String route, String ctaStopId) {
        BusResponse response = http.get()
                .uri("https://www.ctabustracker.com/bustime/api/v3/getpredictions?key={key}&rt={rt}&stpid={stop}&format=json",
                        busKey, route, ctaStopId)
                .retrieve()
                .body(BusResponse.class); // JSON -> the records below

        BusBody body = response == null ? null : response.body();
        if (body == null || body.prd() == null) {
            // CTA sends an error list instead. "No service scheduled" and friends are
            // normal; anything else (like a bad key) is worth a warning.
            if (body != null && body.error() != null) {
                body.error().stream()
                        .filter(e -> e.msg() != null && !e.msg().startsWith("No "))
                        .forEach(e -> log.warn("Bus Tracker error for {} at {}: {}", route, ctaStopId, e.msg()));
            }
            return List.of();
        }
        return body.prd().stream()
                .map(p -> LocalDateTime.parse(p.prdtm(), BUS_TIME).toLocalTime())
                .toList();
    }

    /** Predicted arrivals of a train line at a CTA platform (stop id). Empty when nothing is coming. */
    public List<LocalTime> trainArrivals(String line, String ctaStopId) {
        TrainResponse response = http.get()
                .uri("https://lapi.transitchicago.com/api/1.0/ttarrivals.aspx?key={key}&stpid={stop}&outputType=JSON",
                        trainKey, ctaStopId)
                .retrieve()
                .body(TrainResponse.class);

        TrainBody body = response == null ? null : response.ctatt();
        if (body == null || body.eta() == null) {
            if (body != null && !"0".equals(body.errCd())) {
                log.warn("Train Tracker error {} at {}: {}", body.errCd(), ctaStopId, body.errNm());
            }
            return List.of();
        }
        return body.eta().stream()
                .filter(eta -> line.equals(eta.rt()))
                // "16:25:37" -> 16:25: rounding down never has you arrive after the train
                .map(eta -> LocalDateTime.parse(eta.arrT()).toLocalTime().truncatedTo(ChronoUnit.MINUTES))
                .toList();
    }

    // Just the parts of CTA's JSON we read; ignoreUnknown skips every other field.

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BusResponse(@JsonProperty("bustime-response") BusBody body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BusBody(List<BusPrediction> prd, List<BusError> error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BusPrediction(String prdtm) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BusError(String msg) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrainResponse(TrainBody ctatt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrainBody(String errCd, String errNm, List<TrainEta> eta) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrainEta(String rt, String arrT) {}
}
