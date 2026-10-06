package com.airportrouteplanner.flightservice.companyroute;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyRouteService {

    private final CompanyRouteRepository companyRouteRepository;
    private final FlightLegGenerator flightLegGenerator;

    public CompanyRouteService(
            CompanyRouteRepository companyRouteRepository,
            FlightLegGenerator flightLegGenerator) {
        this.companyRouteRepository = companyRouteRepository;
        this.flightLegGenerator = flightLegGenerator;
    }

    public List<CompanyRouteResponse> getAllCompanyRoutes() {
        return companyRouteRepository.findAllByOrderByCompanyCodeAscRouteNumberAsc().stream()
                .map(CompanyRouteResponse::from)
                .toList();
    }

    public Optional<CompanyRouteResponse> getCompanyRoute(Long id) {
        return companyRouteRepository.findById(id).map(CompanyRouteResponse::from);
    }

    public Optional<List<FlightLegResponse>> getFlightLegs(Long id) {
        return companyRouteRepository.findById(id)
                .map(flightLegGenerator::generate)
                .map(legs -> legs.stream().map(FlightLegResponse::from).toList());
    }
}
