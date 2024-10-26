package io.spring.sample.opsstatus.dashboard;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.spring.sample.opsstatus.availability.InfrastructureComponent;
import io.spring.sample.opsstatus.availability.InfrastructureComponentRepository;
import io.spring.sample.opsstatus.faker.DemoDataGenerator;
import io.spring.sample.opsstatus.incident.Incident;
import io.spring.sample.opsstatus.incident.IncidentRepository;
import io.spring.sample.opsstatus.incident.IncidentStatus;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = DashboardController.class)
class DashboardControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private IncidentRepository incidentRepository;

	@MockitoBean
	private InfrastructureComponentRepository infrastructureComponentRepository;

	@Test
	void getPopulatesModel() {
		Incident inProgress = createTestIncident("Oops", "something failed", IncidentStatus.IN_PROGRESS);
		when(incidentRepository.findAllByStatusOrderByHappenedOnDesc(IncidentStatus.IN_PROGRESS))
			.thenReturn(List.of(inProgress));
		when(incidentRepository.findAllByStatusOrderByHappenedOnDesc(IncidentStatus.SCHEDULED))
			.thenReturn(Collections.emptyList());
		Page<Incident> resolvedIncidents = new PageImpl<>(
				List.of(createTestIncident("Oopsie", "something went wrong", IncidentStatus.RESOLVED)));
		when(incidentRepository.findByStatusOrderByHappenedOnDesc(IncidentStatus.RESOLVED, Pageable.ofSize(5)))
			.thenReturn(resolvedIncidents);
		Set<InfrastructureComponent> infrastructureComponents = new DemoDataGenerator().infrastructureComponents(10);
		when(infrastructureComponentRepository.findAll(Sort.by("name"))).thenReturn(infrastructureComponents);
		assertThat(mvc.get().uri("/")).hasViewName("dashboard")
			.model()
			.contains(entry("inProgress", List.of(inProgress)), entry("scheduled", List.of()),
					entry("resolved", resolvedIncidents), entry("components", infrastructureComponents));
	}

	private Incident createTestIncident(String title, String description, IncidentStatus status) {
		Incident incident = new Incident();
		incident.setTitle(title);
		incident.setDescription(description);
		incident.setHappenedOn(LocalDate.now());
		incident.setStatus(status);
		return incident;
	}

}
