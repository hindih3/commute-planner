package io.github.hindih3.commuteplanner.planner;

import java.time.LocalTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP entry point. GET /plans?start=07:30&deadline=08:30 returns every
 * route that makes the deadline, earliest arrival first, as JSON. Add
 * commute=TO_UNION for the trip home; the default is the trip to campus.
 */
@RestController
public class PlanController {

    private final SegmentRepository segments;
    private final Planner planner;

    public PlanController(SegmentRepository segments, Planner planner) {
        this.segments = segments;
        this.planner = planner;
    }

    @GetMapping("/plans")
    public List<Plan> plans(@RequestParam LocalTime start, @RequestParam LocalTime deadline,
                            // Spring turns "TO_UNION" into Commute.TO_UNION; anything else is a 400
                            @RequestParam(defaultValue = "TO_CAMPUS") Commute commute) {
        // The returned List<Plan> is turned into JSON by Jackson.
        return planner.plan(segments.findByCommute(commute), start, deadline);
    }
}
