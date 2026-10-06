package com.airportrouteplanner.flightservice.companyroute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
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
    private FlightLegGenerator flightLegGenerator;

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
    void flywayLoadsJpaCompatibleReferenceData() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(companyRouteRepository.count()).isEqualTo(616);
    }

    @Test
    void referenceDataHasExpectedIdsCompaniesRoutesAndExternalIdentifiers() {
        List<CompanyRoute> routes = companyRouteRepository.findAll();
        Long[] expectedIds = LongStream.rangeClosed(1, 616).boxed().toArray(Long[]::new);

        assertThat(routes)
                .extracting(CompanyRoute::getId)
                .hasSize(616)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrder(expectedIds);
        assertThat(routes)
                .extracting(route -> route.getCompanyCode() + "\u0000" + route.getRouteNumber())
                .hasSize(616)
                .doesNotHaveDuplicates();
        Map<String, Long> routesPerCompany = routes.stream()
                .collect(Collectors.groupingBy(CompanyRoute::getCompanyCode, Collectors.counting()));
        assertThat(routesPerCompany).hasSize(77);
        assertThat(routesPerCompany.values()).allMatch(count -> count == 8L);
        assertThat(routes).allMatch(route -> route.getRouteNumber() >= 1 && route.getRouteNumber() <= 8);
        assertThat(routes).allMatch(route -> route.getAircraftTypeId() >= 1 && route.getAircraftTypeId() <= 23);
        assertThat(routes).allMatch(route -> !route.getBaseAirportCode().equals(route.getDestinationAirportCode()));

        Set<String> airportCodes = routes.stream()
                .flatMap(route -> Set.of(route.getBaseAirportCode(), route.getDestinationAirportCode()).stream())
                .collect(Collectors.toSet());
        assertThat(airportCodes).hasSize(161);

        Map<String, List<CompanyRoute>> routesByCompany = routes.stream()
                .collect(Collectors.groupingBy(CompanyRoute::getCompanyCode));
        assertThat(routesByCompany.values()).allSatisfy(companyRoutes -> assertThat(companyRoutes)
                .extracting(CompanyRoute::getDestinationAirportCode)
                .doesNotHaveDuplicates());
    }

    @Test
    void repositoryReturnsAllReferenceRoutesInCompanyAndRouteNumberOrder() {
        List<CompanyRoute> routes = companyRouteRepository.findAllByOrderByCompanyCodeAscRouteNumberAsc();

        assertThat(routes)
                .hasSize(616)
                .isSortedAccordingTo(Comparator.comparing(CompanyRoute::getCompanyCode)
                        .thenComparingInt(CompanyRoute::getRouteNumber));
    }

    @Test
    void representativeRoutesMatchVettedReferenceData() {
        assertRoute(1L, "A3", 1, "ATH", "SKP", 15L, LocalTime.MIDNIGHT);
        assertRoute(80L, "AF", 8, "CDG", "TBS", 20L, LocalTime.MIDNIGHT);
        assertRoute(616L, "W6", 8, "BUD", "TBS", 9L, LocalTime.of(22, 20));
        assertThat(companyRouteRepository.findById(151L).orElseThrow().getScheduledOutboundDepartureUtc())
                .isEqualTo(LocalTime.MIDNIGHT);
        assertThat(companyRouteRepository.findById(222L).orElseThrow().getScheduledOutboundDepartureUtc())
                .isEqualTo(LocalTime.MIDNIGHT);
        assertThat(companyRouteRepository.findById(293L).orElseThrow().getScheduledOutboundDepartureUtc())
                .isEqualTo(LocalTime.MIDNIGHT);
    }

    @Test
    void correctedSeedMigrationContainsNoTwentyFourHourLiteral() throws Exception {
        String migration = new ClassPathResource("db/migration/V2__seed_company_routes.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(migration).doesNotContain("24:00:00");
    }

    @Test
    void generatedCompanyRouteIdDoesNotCollideWithReferenceIds() {
        CompanyRoute route = companyRouteRepository.saveAndFlush(new CompanyRoute(
                1, "TEST", "LIS", "OPO", 9L, LocalTime.of(8, 30)));

        assertThat(route.getId()).isGreaterThan(616L);
        assertThat(companyRouteRepository.findById(route.getId())).contains(route);
    }

    @Test
    void everyReferenceRouteDerivesOutboundThenReturnWithoutPersistence() {
        List<CompanyRoute> routes = companyRouteRepository.findAll();

        assertThat(routes).hasSize(616).allSatisfy(route -> {
            List<FlightLeg> legs = flightLegGenerator.generate(route);
            assertThat(legs).hasSize(2);
            assertThat(legs).extracting(FlightLeg::direction)
                    .containsExactly(FlightDirection.OUTBOUND, FlightDirection.RETURN);

            FlightLeg outbound = legs.get(0);
            assertThat(outbound.originAirportCode()).isEqualTo(route.getBaseAirportCode());
            assertThat(outbound.destinationAirportCode()).isEqualTo(route.getDestinationAirportCode());
            assertThat(outbound.scheduledDepartureUtc()).isEqualTo(route.getScheduledOutboundDepartureUtc());

            FlightLeg returnLeg = legs.get(1);
            assertThat(returnLeg.originAirportCode()).isEqualTo(route.getDestinationAirportCode());
            assertThat(returnLeg.destinationAirportCode()).isEqualTo(route.getBaseAirportCode());
            assertThat(returnLeg.scheduledDepartureUtc())
                    .isEqualTo(route.getScheduledOutboundDepartureUtc().plusHours(12));
        });

        assertThat(flightLegGenerator.generate(companyRouteRepository.findById(1L).orElseThrow()))
                .extracting(FlightLeg::scheduledDepartureUtc)
                .containsExactly(LocalTime.MIDNIGHT, LocalTime.NOON);
        FlightLeg route616Return = flightLegGenerator
                .generate(companyRouteRepository.findById(616L).orElseThrow())
                .get(1);
        assertThat(route616Return.originAirportCode()).isEqualTo("TBS");
        assertThat(route616Return.destinationAirportCode()).isEqualTo("BUD");
        assertThat(route616Return.scheduledDepartureUtc()).isEqualTo(LocalTime.of(10, 20));
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
    void getAllCompanyRoutesReturnsCompleteOrderedReferenceData() throws Exception {
        mockMvc.perform(get("/api/company-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(616))
                .andExpect(jsonPath("$[0].companyCode").value("5F"))
                .andExpect(jsonPath("$[0].routeNumber").value(1));
    }

    @Test
    void getCompanyRouteReturnsExpectedSeededRoute() throws Exception {
        mockMvc.perform(get("/api/company-routes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.routeNumber").value(1))
                .andExpect(jsonPath("$.companyCode").value("A3"))
                .andExpect(jsonPath("$.baseAirportCode").value("ATH"))
                .andExpect(jsonPath("$.destinationAirportCode").value("SKP"))
                .andExpect(jsonPath("$.aircraftTypeId").value(15))
                .andExpect(jsonPath("$.scheduledOutboundDepartureUtc").value("00:00:00"));
    }

    @Test
    void correctedMidnightRoutesUseNativeLocalTimeApiRepresentation() throws Exception {
        for (long routeId : List.of(80L, 151L, 222L, 293L)) {
            mockMvc.perform(get("/api/company-routes/{id}", routeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduledOutboundDepartureUtc").value("00:00:00"));
        }
    }

    @Test
    void getFlightLegsReturnsOutboundThenWrappedReturnForSeededRoute() throws Exception {
        mockMvc.perform(get("/api/company-routes/616/flight-legs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].companyRouteId").value(616))
                .andExpect(jsonPath("$[0].direction").value("OUTBOUND"))
                .andExpect(jsonPath("$[0].originAirportCode").value("BUD"))
                .andExpect(jsonPath("$[0].destinationAirportCode").value("TBS"))
                .andExpect(jsonPath("$[0].scheduledDepartureUtc").value("22:20:00"))
                .andExpect(jsonPath("$[1].direction").value("RETURN"))
                .andExpect(jsonPath("$[1].originAirportCode").value("TBS"))
                .andExpect(jsonPath("$[1].destinationAirportCode").value("BUD"))
                .andExpect(jsonPath("$[1].scheduledDepartureUtc").value("10:20:00"));
    }

    @Test
    void unknownRouteReturnsNotFoundForRouteAndFlightLegEndpoints() throws Exception {
        mockMvc.perform(get("/api/company-routes/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/company-routes/999999/flight-legs"))
                .andExpect(status().isNotFound());
    }

    private void assertRoute(
            Long id,
            String companyCode,
            int routeNumber,
            String baseAirportCode,
            String destinationAirportCode,
            Long aircraftTypeId,
            LocalTime scheduledDepartureUtc) {
        CompanyRoute route = companyRouteRepository.findById(id).orElseThrow();
        assertThat(route.getCompanyCode()).isEqualTo(companyCode);
        assertThat(route.getRouteNumber()).isEqualTo(routeNumber);
        assertThat(route.getBaseAirportCode()).isEqualTo(baseAirportCode);
        assertThat(route.getDestinationAirportCode()).isEqualTo(destinationAirportCode);
        assertThat(route.getAircraftTypeId()).isEqualTo(aircraftTypeId);
        assertThat(route.getScheduledOutboundDepartureUtc()).isEqualTo(scheduledDepartureUtc);
    }
}
