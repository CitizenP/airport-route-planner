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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void flywayMigrationsCreateSchemaAndLoadBuiltInDataset() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(airportRepository.count()).isEqualTo(161);
    }

    @Test
    void airportCanBePersistedAndRetrieved() {
        airportRepository.saveAndFlush(new Airport(
                "TST", "Test Airport", "Test City", "Test Country",
                10.0, 20.0, 1, 60));

        Airport airport = airportRepository.findById("TST").orElseThrow();

        assertThat(airport.getName()).isEqualTo("Test Airport");
        assertThat(airport.getUtcOffsetMinutes()).isEqualTo(60);
    }

    @Test
    void allSeededIataCodesAreUnique() {
        assertThat(airportRepository.findAll())
                .extracting(Airport::getIataCode)
                .hasSize(161)
                .doesNotHaveDuplicates();
    }

    @Test
    void allAirportsAreOrderedByIataCode() {
        assertThat(airportRepository.findAllByOrderByIataCodeAsc())
                .hasSize(161)
                .extracting(Airport::getIataCode)
                .isSorted();
    }

    @Test
    void representativeFixedUtcOffsetsAreLoaded() {
        assertUtcOffset("CMN", 0);
        assertUtcOffset("DEL", 330);
        assertUtcOffset("KBL", 270);
        assertUtcOffset("KTM", 345);
        assertUtcOffset("RGN", 390);
    }

    @Test
    void monacoHeliportUsesSimulationRunwayCapacity() {
        Airport monacoHeliport = airportRepository.findById("MCM").orElseThrow();

        assertThat(monacoHeliport.getNumberOfRunways()).isEqualTo(1);
    }

    @Test
    void getAllAirportsReturnsCompleteOrderedDataset() throws Exception {
        mockMvc.perform(get("/api/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(161))
                .andExpect(jsonPath("$[0].iataCode").value("ADD"))
                .andExpect(jsonPath("$[160].iataCode").value("ZRH"));
    }

    @Test
    void getAirportReturnsExpectedAirport() throws Exception {
        mockMvc.perform(get("/api/airports/DEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("DEL"))
                .andExpect(jsonPath("$.name").value("Indira Gandhi International Airport"))
                .andExpect(jsonPath("$.city").value("Delhi"))
                .andExpect(jsonPath("$.country").value("India"))
                .andExpect(jsonPath("$.latitude").value(28.5562))
                .andExpect(jsonPath("$.longitude").value(77.1))
                .andExpect(jsonPath("$.numberOfRunways").value(4))
                .andExpect(jsonPath("$.utcOffsetMinutes").value(330));
    }

    @Test
    void lowercaseIataLookupWorks() throws Exception {
        mockMvc.perform(get("/api/airports/del"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("DEL"));
    }

    @Test
    void unknownIataCodeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/airports/XXX"))
                .andExpect(status().isNotFound());
    }

    private void assertUtcOffset(String iataCode, int expectedOffsetMinutes) {
        Airport airport = airportRepository.findById(iataCode).orElseThrow();
        assertThat(airport.getUtcOffsetMinutes()).isEqualTo(expectedOffsetMinutes);
    }
}
