package io.github.hindih3.commuteplanner.planner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

// Starts the whole app (database included), then asks Spring for the repository.
@SpringBootTest
class SegmentRepositoryTest {

    @Autowired
    SegmentRepository repository;

    @Test
    void loadsTheSeededTree() {
        List<Segment> segments = repository.findAll();
        assertThat(segments).hasSize(12);
    }

    @Test
    void rootHasNullParentNotZero() {
        Segment root = repository.findAll().getFirst();
        assertThat(root.description()).isEqualTo("Exit Union Station");
        assertThat(root.parentId()).isNull();
    }

    @Test
    void busRowIsMappedFieldByField() {
        Segment bus = repository.findAll().get(2); // id 3: "157 to building"
        assertThat(bus).isEqualTo(new Segment(3, 2, Mode.BUS, "157 to building", 8, "157", 1));
    }
}
