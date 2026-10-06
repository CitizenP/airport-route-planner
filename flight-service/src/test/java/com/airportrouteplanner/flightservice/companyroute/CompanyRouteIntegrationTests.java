package com.airportrouteplanner.flightservice.companyroute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.time.LocalTime;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class CompanyRouteIntegrationTests {

    @Autowired
    private CompanyRouteRepository companyRouteRepository;

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void flywayCreatesJpaCompatibleEmptySchema() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(companyRouteRepository.count()).isZero();
    }

    @Test
    void companyRouteCanBePersistedAndRetrieved() {
        CompanyRoute saved = companyRouteRepository.saveAndFlush(route(1, "tp", "lis", "opo", 9L, 8, 30));

        CompanyRoute retrieved = companyRouteRepository.findById(saved.getId()).orElseThrow();

        assertThat(retrieved.getCompanyCode()).isEqualTo("TP");
        assertThat(retrieved.getBaseAirportCode()).isEqualTo("LIS");
        assertThat(retrieved.getDestinationAirportCode()).isEqualTo("OPO");
        assertThat(retrieved.getAircraftTypeId()).isEqualTo(9L);
        assertThat(retrieved.getScheduledOutboundDepartureUtc()).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void companyRoutesAreOrderedByCompanyThenRouteNumber() {
        companyRouteRepository.save(route(2, "TP", "LIS", "JFK", 9L, 9, 0));
        companyRouteRepository.save(route(2, "BA", "LHR", "CDG", 10L, 10, 0));
        companyRouteRepository.save(route(1, "BA", "LHR", "OPO", 11L, 11, 0));
        companyRouteRepository.flush();

        assertThat(companyRouteRepository.findAllByOrderByCompanyCodeAscRouteNumberAsc())
                .extracting(CompanyRoute::getCompanyCode, CompanyRoute::getRouteNumber)
                .containsExactly(tuple("BA", 1), tuple("BA", 2), tuple("TP", 2));
    }

    @Test
    void flightLegIsNeitherJpaEntityNorDatabaseTable() throws Exception {
        assertThat(entityManager.getMetamodel().getEntities())
                .extracting(entityType -> entityType.getJavaType().getSimpleName())
                .contains("CompanyRoute")
                .doesNotContain("FlightLeg");

        try (var connection = dataSource.getConnection();
                ResultSet tables = connection.getMetaData().getTables(
                        null, "PUBLIC", "FLIGHT_LEGS", new String[] {"TABLE"})) {
            assertThat(tables.next()).isFalse();
        }
    }

    @Test
    void getAllCompanyRoutesIsEmptyBeforeSeedDataExists() throws Exception {
        mockMvc.perform(get("/api/company-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllCompanyRoutesReturnsOrderedData() throws Exception {
        companyRouteRepository.save(route(2, "TP", "LIS", "JFK", 9L, 9, 0));
        companyRouteRepository.save(route(1, "BA", "LHR", "OPO", 10L, 10, 0));
        companyRouteRepository.flush();

        mockMvc.perform(get("/api/company-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].companyCode").value("BA"))
                .andExpect(jsonPath("$[0].routeNumber").value(1))
                .andExpect(jsonPath("$[1].companyCode").value("TP"));
    }

    @Test
    void getCompanyRouteReturnsExpectedData() throws Exception {
        CompanyRoute saved = companyRouteRepository.saveAndFlush(route(
                3, "TP", "LIS", "JFK", 9L, 8, 30));

        mockMvc.perform(get("/api/company-routes/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.routeNumber").value(3))
                .andExpect(jsonPath("$.companyCode").value("TP"))
                .andExpect(jsonPath("$.baseAirportCode").value("LIS"))
                .andExpect(jsonPath("$.destinationAirportCode").value("JFK"))
                .andExpect(jsonPath("$.aircraftTypeId").value(9))
                .andExpect(jsonPath("$.scheduledOutboundDepartureUtc").value("08:30:00"));
    }

    @Test
    void getFlightLegsReturnsOutboundThenReturnWithDailyTimeWrap() throws Exception {
        CompanyRoute saved = companyRouteRepository.saveAndFlush(route(
                4, "QR", "DOH", "KBL", 10L, 18, 45));

        mockMvc.perform(get("/api/company-routes/{id}/flight-legs", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].companyRouteId").value(saved.getId()))
                .andExpect(jsonPath("$[0].direction").value("OUTBOUND"))
                .andExpect(jsonPath("$[0].originAirportCode").value("DOH"))
                .andExpect(jsonPath("$[0].destinationAirportCode").value("KBL"))
                .andExpect(jsonPath("$[0].scheduledDepartureUtc").value("18:45:00"))
                .andExpect(jsonPath("$[1].direction").value("RETURN"))
                .andExpect(jsonPath("$[1].originAirportCode").value("KBL"))
                .andExpect(jsonPath("$[1].destinationAirportCode").value("DOH"))
                .andExpect(jsonPath("$[1].scheduledDepartureUtc").value("06:45:00"));
    }

    @Test
    void unknownRouteReturnsNotFoundForRouteAndFlightLegEndpoints() throws Exception {
        mockMvc.perform(get("/api/company-routes/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/company-routes/999999/flight-legs"))
                .andExpect(status().isNotFound());
    }

    private CompanyRoute route(
            int routeNumber,
            String companyCode,
            String baseAirportCode,
            String destinationAirportCode,
            Long aircraftTypeId,
            int departureHour,
            int departureMinute) {
        return new CompanyRoute(
                routeNumber,
                companyCode,
                baseAirportCode,
                destinationAirportCode,
                aircraftTypeId,
                LocalTime.of(departureHour, departureMinute));
    }
}
