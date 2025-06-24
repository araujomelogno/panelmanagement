package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PanelRepository extends JpaRepository<Panel, Long>, JpaSpecificationExecutor<Panel> {

    @Query("SELECT p FROM Panel p LEFT JOIN FETCH p.panelists WHERE p.id = :id")
    Optional<Panel> findByIdWithPanelists(@Param("id") Long id);

}
