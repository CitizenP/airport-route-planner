package com.airportrouteplanner.fleetservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airportrouteplanner.fleetservice.aircrafttype.AircraftType;
import com.airportrouteplanner.fleetservice.aircrafttype.AircraftTypeRepository;
import com.airportrouteplanner.fleetservice.flightcompany.FlightCompany;
import com.airportrouteplanner.fleetservice.flightcompany.FlightCompanyRepository;
import java.util.stream.LongStream;
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void flywayLoadsJpaCompatibleReferenceData() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(aircraftTypeRepository.count()).isEqualTo(23);
        assertThat(flightCompanyRepository.count()).isEqualTo(77);
    }

    @Test
    void aircraftReferenceDataHasExpectedIdentifiersAndUniqueModels() {
        Long[] expectedIds = LongStream.rangeClosed(1, 23).boxed().toArray(Long[]::new);

        assertThat(aircraftTypeRepository.findAll())
                .extracting(AircraftType::getId)
                .hasSize(23)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrder(expectedIds);
        assertThat(aircraftTypeRepository.findAll())
                .extracting(aircraftType -> aircraftType.getManufacturer() + "\u0000" + aircraftType.getModel())
                .hasSize(23)
                .doesNotHaveDuplicates();
    }

    @Test
    void aircraftTypeNineMatchesVettedReferenceData() {
        AircraftType aircraftType = aircraftTypeRepository.findById(9L).orElseThrow();

        assertThat(aircraftType.getManufacturer()).isEqualTo("Airbus");
        assertThat(aircraftType.getModel()).isEqualTo("A350-900");
        assertThat(aircraftType.getCruiseSpeedKmH()).isEqualTo(903.0);
        assertThat(aircraftType.getMaximumRangeKm()).isEqualTo(15372.0);
        assertThat(aircraftType.getFuelCapacityLitres()).isEqualTo(141000.0);
        assertThat(aircraftType.getFuelConsumptionLitresPerKm()).isEqualTo(8.1);
        assertThat(aircraftType.getPassengerCapacity()).isEqualTo(440);
        assertThat(aircraftType.getFuelConsumptionPerPassenger()).isEqualTo(2.49);
    }

    @Test
    void generatedAircraftTypeIdDoesNotCollideWithReferenceIds() {
        AircraftType aircraftType = aircraftTypeRepository.saveAndFlush(new AircraftType(
                "Test Manufacturer", "Test Model", 800.0, 5000.0, 20000.0, 3.0, 150, 2.0));

        assertThat(aircraftType.getId()).isGreaterThan(23L);
        assertThat(aircraftTypeRepository.findById(aircraftType.getId())).contains(aircraftType);
    }

    @Test
    void aircraftTypesAreOrderedByManufacturerThenModel() {
        assertThat(aircraftTypeRepository.findAllByOrderByManufacturerAscModelAsc())
                .hasSize(23)
                .extracting(aircraftType -> aircraftType.getManufacturer() + "\u0000" + aircraftType.getModel())
                .isSorted();
    }

    @Test
    void getAllAircraftTypesReturnsCompleteOrderedData() throws Exception {
        mockMvc.perform(get("/api/aircraft-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(23))
                .andExpect(jsonPath("$[0].manufacturer").value("Airbus"))
                .andExpect(jsonPath("$[0].model").value("A220-100"));
    }

    @Test
    void getAircraftTypeReturnsExpectedReferenceData() throws Exception {
        mockMvc.perform(get("/api/aircraft-types/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.manufacturer").value("Airbus"))
                .andExpect(jsonPath("$.model").value("A350-900"))
                .andExpect(jsonPath("$.cruiseSpeedKmH").value(903.0))
                .andExpect(jsonPath("$.maximumRangeKm").value(15372.0))
                .andExpect(jsonPath("$.fuelCapacityLitres").value(141000.0))
                .andExpect(jsonPath("$.fuelConsumptionLitresPerKm").value(8.1))
                .andExpect(jsonPath("$.passengerCapacity").value(440))
                .andExpect(jsonPath("$.fuelConsumptionPerPassenger").value(2.49));
    }

    @Test
    void aircraftTypeUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/aircraft-types/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void flightCompanyReferenceDataHasUniqueCodesAndRepresentativeRecords() {
        assertThat(flightCompanyRepository.findAll())
                .extracting(FlightCompany::getCode)
                .hasSize(77)
                .doesNotHaveDuplicates();

        assertFlightCompany("TP", "TAP Portugal", "Portugal", "LIS");
        assertFlightCompany("QR", "Qatar Airways", "Qatar", "DOH");
        assertFlightCompany("5F", "FlyOne", "Moldova", "RMO");
    }

    @Test
    void flightCompaniesAreOrderedByCode() {
        assertThat(flightCompanyRepository.findAllByOrderByCodeAsc())
                .hasSize(77)
                .extracting(FlightCompany::getCode)
                .isSorted();
    }

    @Test
    void getAllFlightCompaniesReturnsCompleteOrderedData() throws Exception {
        mockMvc.perform(get("/api/flight-companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(77))
                .andExpect(jsonPath("$[0].code").value("5F"));
    }

    @Test
    void getFlightCompanyReturnsExpectedReferenceData() throws Exception {
        mockMvc.perform(get("/api/flight-companies/TP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TP"))
                .andExpect(jsonPath("$.name").value("TAP Portugal"))
                .andExpect(jsonPath("$.country").value("Portugal"))
                .andExpect(jsonPath("$.baseAirportCode").value("LIS"));
    }

    @Test
    void lowercaseAlphanumericCompanyCodeLookupReturnsExpectedData() throws Exception {
        mockMvc.perform(get("/api/flight-companies/5f"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("5F"))
                .andExpect(jsonPath("$.name").value("FlyOne"))
                .andExpect(jsonPath("$.country").value("Moldova"))
                .andExpect(jsonPath("$.baseAirportCode").value("RMO"));
    }

    @Test
    void lowercaseAlphabeticCompanyCodeLookupReturnsExpectedData() throws Exception {
        mockMvc.perform(get("/api/flight-companies/tp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TP"));
    }

    @Test
    void flightCompanyUnknownCodeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/flight-companies/ZZ"))
                .andExpect(status().isNotFound());
    }

    private void assertFlightCompany(String code, String name, String country, String baseAirportCode) {
        FlightCompany company = flightCompanyRepository.findById(code).orElseThrow();
        assertThat(company.getName()).isEqualTo(name);
        assertThat(company.getCountry()).isEqualTo(country);
        assertThat(company.getBaseAirportCode()).isEqualTo(baseAirportCode);
    }
}
