package com.airportrouteplanner.fleetservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airportrouteplanner.fleetservice.aircrafttype.AircraftType;
import com.airportrouteplanner.fleetservice.aircrafttype.AircraftTypeRepository;
import com.airportrouteplanner.fleetservice.flightcompany.FlightCompany;
import com.airportrouteplanner.fleetservice.flightcompany.FlightCompanyRepository;
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
class FleetIntegrationTests {

    @Autowired
    private AircraftTypeRepository aircraftTypeRepository;

    @Autowired
    private FlightCompanyRepository flightCompanyRepository;

    @Autowired
    private Flyway flyway;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private AircraftType airbusA320;

    @BeforeEach
    void setUp() {
        flightCompanyRepository.deleteAll();
        aircraftTypeRepository.deleteAll();

        airbusA320 = aircraftTypeRepository.save(new AircraftType(
                "Airbus", "A320", 840.0, 6100.0, 24210.0, 3.1, 180, 1.72));
        aircraftTypeRepository.save(new AircraftType(
                "Boeing", "737-800", 842.0, 5765.0, 26020.0, 3.2, 189, 1.69));

        flightCompanyRepository.save(new FlightCompany(
                "TP", "TAP Air Portugal", "Portugal", "LIS"));
        flightCompanyRepository.save(new FlightCompany(
                "BA", "British Airways", "United Kingdom", "LHR"));

        aircraftTypeRepository.flush();
        flightCompanyRepository.flush();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void flywayMigrationCreatesJpaCompatibleSchema() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(aircraftTypeRepository.count()).isEqualTo(2);
        assertThat(flightCompanyRepository.count()).isEqualTo(2);
    }

    @Test
    void aircraftTypeCanBePersistedAndRetrieved() {
        AircraftType aircraftType = aircraftTypeRepository.findById(airbusA320.getId()).orElseThrow();

        assertThat(aircraftType.getManufacturer()).isEqualTo("Airbus");
        assertThat(aircraftType.getModel()).isEqualTo("A320");
        assertThat(aircraftType.getPassengerCapacity()).isEqualTo(180);
    }

    @Test
    void flightCompanyCanBePersistedAndRetrieved() {
        FlightCompany flightCompany = flightCompanyRepository.findById("TP").orElseThrow();

        assertThat(flightCompany.getName()).isEqualTo("TAP Air Portugal");
        assertThat(flightCompany.getBaseAirportCode()).isEqualTo("LIS");
    }

    @Test
    void aircraftTypesAreOrderedByManufacturerThenModel() {
        assertThat(aircraftTypeRepository.findAllByOrderByManufacturerAscModelAsc())
                .extracting(AircraftType::getManufacturer, AircraftType::getModel)
                .containsExactly(
                        tuple("Airbus", "A320"),
                        tuple("Boeing", "737-800"));
    }

    @Test
    void flightCompaniesAreOrderedByCode() {
        assertThat(flightCompanyRepository.findAllByOrderByCodeAsc())
                .extracting(FlightCompany::getCode)
                .containsExactly("BA", "TP");
    }

    @Test
    void getAllAircraftTypesReturnsOrderedData() throws Exception {
        mockMvc.perform(get("/api/aircraft-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].manufacturer").value("Airbus"))
                .andExpect(jsonPath("$[0].model").value("A320"))
                .andExpect(jsonPath("$[1].manufacturer").value("Boeing"));
    }

    @Test
    void getAircraftTypeReturnsExpectedData() throws Exception {
        mockMvc.perform(get("/api/aircraft-types/{id}", airbusA320.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(airbusA320.getId()))
                .andExpect(jsonPath("$.manufacturer").value("Airbus"))
                .andExpect(jsonPath("$.model").value("A320"))
                .andExpect(jsonPath("$.cruiseSpeedKmH").value(840.0))
                .andExpect(jsonPath("$.maximumRangeKm").value(6100.0))
                .andExpect(jsonPath("$.fuelCapacityLitres").value(24210.0))
                .andExpect(jsonPath("$.fuelConsumptionLitresPerKm").value(3.1))
                .andExpect(jsonPath("$.passengerCapacity").value(180))
                .andExpect(jsonPath("$.fuelConsumptionPerPassenger").value(1.72));
    }

    @Test
    void getAllFlightCompaniesReturnsOrderedData() throws Exception {
        mockMvc.perform(get("/api/flight-companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("BA"))
                .andExpect(jsonPath("$[1].code").value("TP"));
    }

    @Test
    void lowercaseCompanyCodeLookupReturnsExpectedData() throws Exception {
        mockMvc.perform(get("/api/flight-companies/tp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TP"))
                .andExpect(jsonPath("$.name").value("TAP Air Portugal"))
                .andExpect(jsonPath("$.country").value("Portugal"))
                .andExpect(jsonPath("$.baseAirportCode").value("LIS"));
    }

    @Test
    void unknownAircraftTypeAndCompanyReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/aircraft-types/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/flight-companies/ZZ"))
                .andExpect(status().isNotFound());
    }
}
