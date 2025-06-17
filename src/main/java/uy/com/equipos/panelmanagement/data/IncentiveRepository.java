package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncentiveRepository extends JpaRepository<Incentive, Long>, JpaSpecificationExecutor<Incentive> {

}
