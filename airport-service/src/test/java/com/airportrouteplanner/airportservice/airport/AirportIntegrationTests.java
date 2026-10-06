package com.airportrouteplanner.airportservice.airport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AirportIntegrationTests {

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private Flyway flyway;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        airportRepository.deleteAll();
        airportRepository.save(new Airport(
                "OPO", "Francisco Sá Carneiro Airport", "Porto", "Portugal",
                41.2481, -8.6814, 1, 0));
        airportRepository.save(new Airport(
                "DEL", "Indira Gandhi International Airport", "Delhi", "India",
                28.5562, 77.1000, 4, 330));
        airportRepository.save(new Airport(
                "AKL", "Auckland Airport", "Auckland", "New Zealand",
                -37.0082, 174.7850, 2, 720));
        airportRepository.flush();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void flywayMigrationCreatesJpaCompatibleSchema() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(airportRepository.count()).isEqualTo(3);
    }

    @Test
    void airportCanBePersistedAndRetrieved() {
        Airport airport = airportRepository.findById("OPO").orElseThrow();

        assertThat(airport.getName()).isEqualTo("Francisco Sá Carneiro Airport");
        assertThat(airport.getUtcOffsetMinutes()).isZero();
    }

    @Test
    void allAirportsAreOrderedByIataCode() {
        assertThat(airportRepository.findAllByOrderByIataCodeAsc())
                .extracting(Airport::getIataCode)
                .containsExactly("AKL", "DEL", "OPO");
    }

    @Test
    void getAllAirportsReturnsOrderedAirportData() throws Exception {
        mockMvc.perform(get("/api/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].iataCode").value("AKL"))
                .andExpect(jsonPath("$[1].iataCode").value("DEL"))
                .andExpect(jsonPath("$[1].utcOffsetMinutes").value(330))
                .andExpect(jsonPath("$[2].iataCode").value("OPO"))
                .andExpect(jsonPath("$[2].city").value("Porto"));
    }

    @Test
    void getAirportReturnsExpectedAirport() throws Exception {
        mockMvc.perform(get("/api/airports/OPO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("OPO"))
                .andExpect(jsonPath("$.name").value("Francisco Sá Carneiro Airport"))
                .andExpect(jsonPath("$.country").value("Portugal"))
                .andExpect(jsonPath("$.latitude").value(41.2481))
                .andExpect(jsonPath("$.longitude").value(-8.6814))
                .andExpect(jsonPath("$.numberOfRunways").value(1))
                .andExpect(jsonPath("$.utcOffsetMinutes").value(0));
    }

    @Test
    void lowercaseIataLookupWorks() throws Exception {
        mockMvc.perform(get("/api/airports/opo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("OPO"));
    }

    @Test
    void unknownIataCodeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/airports/XXX"))
                .andExpect(status().isNotFound());
    }
}
