package io.github.hindih3.commuteplanner.planner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Starts only CtaClient plus a fake CTA server (MockRestServiceServer): no real
// HTTP calls. The JSON below is trimmed from real Bus/Train Tracker responses.
@RestClientTest(components = CtaClient.class,
        properties = {"cta.bus-tracker-key=test-bus-key", "cta.train-tracker-key=test-train-key"})
class CtaClientTest {

    @Autowired
    CtaClient cta;

    @Autowired
    MockRestServiceServer server;

    @Test
    void busPredictionsBecomeTimes() {
        server.expect(requestTo("https://www.ctabustracker.com/bustime/api/v3/getpredictions"
                        + "?key=test-bus-key&rt=8&stpid=5799&format=json"))
                .andRespond(withSuccess("""
                        {"bustime-response": {"prd": [
                          {"typ": "A", "stpid": "5799", "rt": "8", "rtdir": "Southbound", "prdtm": "20261025 16:22", "prdctdn": "2"},
                          {"typ": "A", "stpid": "5799", "rt": "8", "rtdir": "Southbound", "prdtm": "20261025 16:37", "prdctdn": "17"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(cta.busArrivals("8", "5799")).containsExactly(LocalTime.of(16, 22), LocalTime.of(16, 37));
    }

    @Test
    void noServiceIsAnEmptyList() {
        server.expect(requestTo("https://www.ctabustracker.com/bustime/api/v3/getpredictions"
                        + "?key=test-bus-key&rt=157&stpid=6361&format=json"))
                .andRespond(withSuccess("""
                        {"bustime-response": {"error": [{"rt": "157", "stpid": "6361", "msg": "No service scheduled"}]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(cta.busArrivals("157", "6361")).isEmpty();
    }

    @Test
    void trainPredictionsBecomeTimesRoundedDown() {
        server.expect(requestTo("https://lapi.transitchicago.com/api/1.0/ttarrivals.aspx"
                        + "?key=test-train-key&stpid=30085&outputType=JSON"))
                .andRespond(withSuccess("""
                        {"ctatt": {"tmst": "2026-10-25T16:19:45", "errCd": "0", "errNm": null, "eta": [
                          {"stpId": "30085", "rt": "Blue", "destNm": "Forest Park", "arrT": "2026-10-25T16:25:37", "isSch": "0"},
                          {"stpId": "30085", "rt": "Blue", "destNm": "Forest Park", "arrT": "2026-10-25T16:38:36", "isSch": "0"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(cta.trainArrivals("Blue", "30085")).containsExactly(LocalTime.of(16, 25), LocalTime.of(16, 38));
    }
}
