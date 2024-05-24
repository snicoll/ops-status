package io.spring.sample.opsstatus.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.spring.sample.opsstatus.incident.Incident;
import io.spring.sample.opsstatus.incident.IncidentOrigin;
import io.spring.sample.opsstatus.incident.IncidentRepository;
import io.spring.sample.opsstatus.incident.IncidentStatus;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.context.WebApplicationContext;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(controllers = ApiController.class)
class ApiControllerTests {

	@MockBean
	private IncidentRepository incidentRepository;

	private final MockMvcTester mvc;

	public ApiControllerTests(@Autowired WebApplicationContext applicationContext) {
		this.mvc = MockMvcTester.from(applicationContext);
	}

	@Test
	void incidentsWhenNoIncident() {
		when(this.incidentRepository.streamAll()).thenReturn(Stream.empty());
		assertThat(mvc.perform(get("/api/incidents"))).hasStatusOk()
				.bodyJson().extractingPath("incidents").isEmpty();
	}

	@Test
	void incidentsWithIncidents() {
		List<Incident> incidents = List.of(
				createTestIncident("test", "a description", LocalDate.of(2024, 1, 2), IncidentOrigin.MAINTENANCE,
						IncidentStatus.RESOLVED),
				createTestIncident("another", "another description", LocalDate.of(2024, 1, 20), IncidentOrigin.ISSUE,
						IncidentStatus.IN_PROGRESS));
		when(this.incidentRepository.streamAll()).thenReturn(incidents.stream());
		MvcTestResult result = mvc.perform(get("/api/incidents"));
		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("incidents").asArray().hasSize(2);
		assertThat(result).bodyJson().extractingPath("incidents[0]").asMap().satisfies(jsonIncident(incidents.get(0)));
		assertThat(result).bodyJson().extractingPath("incidents[1]").asMap().satisfies(jsonIncident(incidents.get(1)));
	}

	@Test
	void incident() {
		Incident testIncident = createTestIncident("test", "a description", LocalDate.of(2024, 1, 2),
				IncidentOrigin.MAINTENANCE, IncidentStatus.RESOLVED);
		when(this.incidentRepository.findById(42L)).thenReturn(Optional.of(testIncident));
		assertThat(mvc.perform(get("/api/incidents/42"))).hasStatusOk()
			.bodyJson()
			.extractingPath("$")
			.asMap()
			.satisfies(jsonIncident(testIncident));
	}

	@Test
	void incidentWithNoSuchIncident() {
		when(this.incidentRepository.findById(42L)).thenReturn(Optional.empty());
		assertThat(mvc.perform(get("/api/incidents/42").accept(MediaType.APPLICATION_PROBLEM_JSON)))
				.hasStatus(HttpStatus.NOT_FOUND)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().withResourceLoadClass(getClass()).isStrictlyEqualTo("missing-incident-42.json");
	}

	private Consumer<Map<String, Object>> jsonIncident(Incident incident) {
		return map -> assertThat(map).contains(entry("title", incident.getTitle()),
				entry("description", incident.getDescription()),
				entry("happenedOn", incident.getHappenedOn().toString()),
				entry("origin", incident.getOrigin().toString()), entry("status", incident.getStatus().toString()));
	}

	private Incident createTestIncident(String title, String description, LocalDate happenedOn, IncidentOrigin origin,
			IncidentStatus status) {
		Incident incident = new Incident();
		incident.setTitle(title);
		incident.setDescription(description);
		incident.setHappenedOn(happenedOn);
		incident.setOrigin(origin);
		incident.setStatus(status);
		return incident;
	}

}
