package io.github.hindih3.commuteplanner.planner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import static org.assertj.core.api.Assertions.assertThat;

// Starts the whole app with a fake browser (MockMvcTester) instead of a real server,
// so these requests go through the controller, planner and database for real.
// Live CTA data is off so results don't depend on where real buses are right now.
@SpringBootTest(properties = "cta.live.enabled=false")
@AutoConfigureMockMvc
class PlanControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void returnsPlansAsJson() {
        assertThat(mvc.get().uri("/plans?start=07:30&deadline=08:30"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()").isEqualTo(5);

        assertThat(mvc.get().uri("/plans?start=07:30&deadline=08:30"))
                .bodyJson()
                .extractingPath("$[0].arrival").isEqualTo("07:51:00"); // Jackson writes seconds too
    }

    @Test
    void boardingBufferSkipsTheBusYouWouldOnlyJustMake() {
        // At the 157 stop at 7:35. The 7:35 bus is too tight with the 2-minute buffer,
        // so the plan waits for the 7:43 one
        assertThat(mvc.get().uri("/plans?start=07:30&deadline=08:30"))
                .bodyJson()
                .extractingPath("$[0].legs[1].arrives").isEqualTo("07:35:00");

        assertThat(mvc.get().uri("/plans?start=07:30&deadline=08:30"))
                .bodyJson()
                .extractingPath("$[0].legs[2].departs").isEqualTo("07:43:00");
    }

    @Test
    void missingParameterIsABadRequest() {
        assertThat(mvc.get().uri("/plans?start=07:30")).hasStatus4xxClientError();
    }

    @Test
    void unreadableTimeIsABadRequest() {
        assertThat(mvc.get().uri("/plans?start=soon&deadline=08:30")).hasStatus4xxClientError();
    }
}
