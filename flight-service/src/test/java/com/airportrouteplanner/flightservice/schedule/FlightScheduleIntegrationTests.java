package com.airportrouteplanner.flightservice.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airportrouteplanner.flightservice.airport.AirportRunwayCapacityProvider;
import com.airportrouteplanner.flightservice.companyroute.CompanyRouteRepository;
import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@Import(FlightScheduleIntegrationTests.CapacityProviderTestConfiguration.class)
class FlightScheduleIntegrationTests {

    @Autowired
    private CompanyRouteRepository companyRouteRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestCapacityProvider runwayCapacityProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void adjustedScheduleEndpointReturnsAllDerivedLegsWithoutChangingRoutes() throws Exception {
        long routesBeforeScheduling = companyRouteRepository.count();
        runwayCapacityProvider.setCapacities(highCapacityForEveryReferencedAirport());

        mockMvc.perform(get("/api/flight-legs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1232))
                .andExpect(jsonPath("$[0].scheduledDepartureUtc").exists())
                .andExpect(jsonPath("$[0].adjustedDepartureUtc").exists())
                .andExpect(jsonPath("$[0].adjustedDepartureDayOffset").value(0))
                .andExpect(jsonPath("$[0].delayMinutes").value(0));

        assertThat(companyRouteRepository.count()).isEqualTo(routesBeforeScheduling);
    }

    @Test
    void incompleteAirportDataReturnsClearBadGatewayResponse() throws Exception {
        runwayCapacityProvider.setCapacities(Map.of());

        mockMvc.perform(get("/api/flight-legs"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Airport runway capacity unavailable"))
                .andExpect(jsonPath("$.detail").value("missing runway capacity for origin airport RMO"));
    }

    @Test
    void derivedScheduleTypesAndTablesAreNotPersisted() throws Exception {
        assertThat(entityManager.getMetamodel().getEntities())
                .extracting(entityType -> entityType.getJavaType().getSimpleName())
                .containsExactly("CompanyRoute");
        assertTableDoesNotExist("FLIGHT_LEGS");
        assertTableDoesNotExist("ADJUSTED_FLIGHT_LEGS");
    }

    private Map<String, Integer> highCapacityForEveryReferencedAirport() {
        return companyRouteRepository.findAll().stream()
                .flatMap(route -> Stream.of(route.getBaseAirportCode(), route.getDestinationAirportCode()))
                .distinct()
                .collect(Collectors.toMap(Function.identity(), ignored -> 100));
    }

    private void assertTableDoesNotExist(String tableName) throws Exception {
        try (var connection = dataSource.getConnection();
                ResultSet tables = connection.getMetaData().getTables(
                        null, "PUBLIC", tableName, new String[] {"TABLE"})) {
            assertThat(tables.next()).isFalse();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CapacityProviderTestConfiguration {

        @Bean
        @Primary
        TestCapacityProvider testCapacityProvider() {
            return new TestCapacityProvider();
        }
    }

    static class TestCapacityProvider implements AirportRunwayCapacityProvider {

        private final AtomicReference<Map<String, Integer>> capacities = new AtomicReference<>(Map.of());

        void setCapacities(Map<String, Integer> capacities) {
            this.capacities.set(Map.copyOf(capacities));
        }

        @Override
        public Map<String, Integer> getRunwayCapacities() {
            return capacities.get();
        }
    }
}
