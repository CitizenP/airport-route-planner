package com.airportrouteplanner.flightservice.companyroute;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRouteRepository extends JpaRepository<CompanyRoute, Long> {

    List<CompanyRoute> findAllByOrderByCompanyCodeAscRouteNumberAsc();
}
