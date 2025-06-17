package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {

}
