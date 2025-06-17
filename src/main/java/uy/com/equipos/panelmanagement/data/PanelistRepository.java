package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PanelistRepository extends JpaRepository<Panelist, Long>, JpaSpecificationExecutor<Panelist> {

}
