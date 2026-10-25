package io.github.hindih3.commuteplanner.planner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import static org.assertj.core.api.Assertions.assertThat;

// Starts the whole app with a fake browser (MockMvcTester) instead of a real server,
// so these requests go through the controller, planner and database for real.
@SpringBootTest
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
                .extractingPath("$[0].arrival").isEqualTo("07:43:00"); // Jackson writes seconds too
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
