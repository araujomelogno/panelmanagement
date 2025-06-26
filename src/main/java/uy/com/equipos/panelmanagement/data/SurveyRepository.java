package uy.com.equipos.panelmanagement.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyRepository extends JpaRepository<Survey, Long>, JpaSpecificationExecutor<Survey> {

    // Option 1: Using @Query with JOIN FETCH
    @Query("SELECT s FROM Survey s LEFT JOIN FETCH s.panelists WHERE s.id = :id")
    Optional<Survey> findByIdWithPanelists(@Param("id") Long id);

    // Option 2: Using @EntityGraph (alternative, choose one)
    // @EntityGraph(attributePaths = { "panelists" })
    // Optional<Survey> findById(Long id); // If using this, you'd rename the existing findById or rely on method override
}
