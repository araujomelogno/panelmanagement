package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PanelistRepository extends JpaRepository<Panelist, Long>, JpaSpecificationExecutor<Panelist>, PanelistRepositoryCustom {

    @Query("SELECT p FROM Panelist p LEFT JOIN FETCH p.surveys WHERE p.id = :id")
    Optional<Panelist> findByIdWithSurveys(@Param("id") Long id);

}
