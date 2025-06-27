package uy.com.equipos.panelmanagement.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyRepository extends JpaRepository<Survey, Long>, JpaSpecificationExecutor<Survey> {

    // Ya no se necesita findByIdWithPanelists, se usará la carga EAGER/LAZY definida en la entidad
    // o se creará un método específico para cargar participaciones si es necesario.
    // Option 1: Using @Query with JOIN FETCH
    // @Query("SELECT s FROM Survey s LEFT JOIN FETCH s.participations WHERE s.id = :id")
    // Optional<Survey> findByIdWithParticipations(@Param("id") Long id);

    // Option 2: Using @EntityGraph (alternative, choose one)
    // @EntityGraph(attributePaths = { "participations" })
    // Optional<Survey> findById(Long id);
}
