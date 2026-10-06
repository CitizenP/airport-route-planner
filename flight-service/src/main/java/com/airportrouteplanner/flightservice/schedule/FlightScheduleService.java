package com.airportrouteplanner.flightservice.schedule;

import com.airportrouteplanner.flightservice.airport.AirportRunwayCapacityException;
import com.airportrouteplanner.flightservice.airport.AirportRunwayCapacityProvider;
import com.airportrouteplanner.flightservice.companyroute.CompanyRouteRepository;
import com.airportrouteplanner.flightservice.companyroute.FlightLeg;
import com.airportrouteplanner.flightservice.companyroute.FlightLegGenerator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FlightScheduleService {

    private final CompanyRouteRepository companyRouteRepository;
    private final FlightLegGenerator flightLegGenerator;
    private final AirportRunwayCapacityProvider runwayCapacityProvider;
    private final RunwayScheduler runwayScheduler;

    public FlightScheduleService(
            CompanyRouteRepository companyRouteRepository,
            FlightLegGenerator flightLegGenerator,
            AirportRunwayCapacityProvider runwayCapacityProvider,
            RunwayScheduler runwayScheduler) {
        this.companyRouteRepository = companyRouteRepository;
        this.flightLegGenerator = flightLegGenerator;
        this.runwayCapacityProvider = runwayCapacityProvider;
        this.runwayScheduler = runwayScheduler;
    }

    public List<AdjustedFlightLegResponse> getAdjustedDailySchedule() {
        List<FlightLeg> rawLegs = companyRouteRepository.findAllByOrderByCompanyCodeAscRouteNumberAsc().stream()
                .flatMap(route -> flightLegGenerator.generate(route).stream())
                .toList();
        Map<String, Integer> runwayCapacities = runwayCapacityProvider.getRunwayCapacities();

        try {
            return runwayScheduler.schedule(rawLegs, runwayCapacities).stream()
                    .map(AdjustedFlightLegResponse::from)
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw new AirportRunwayCapacityException(exception.getMessage(), exception);
        }
    }
}
