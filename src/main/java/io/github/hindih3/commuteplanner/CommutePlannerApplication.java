package io.github.hindih3.commuteplanner;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CommutePlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommutePlannerApplication.class, args);
	}

	// "What time is it?" as a bean, so tests can swap in a fixed clock.
	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}
