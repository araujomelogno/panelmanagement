package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PanelistRepository extends JpaRepository<Panelist, Long>, JpaSpecificationExecutor<Panelist>, PanelistRepositoryCustom {

    // Ya no se necesita findByIdWithSurveys, se usará la carga EAGER/LAZY definida en la entidad
    // o se creará un método específico para cargar participaciones si es necesario.
    // @Query("SELECT p FROM Panelist p LEFT JOIN FETCH p.participations WHERE p.id = :id")
    // Optional<Panelist> findByIdWithParticipations(@Param("id") Long id);

    Optional<Panelist> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Panelist> findByStatusAndRecruitmentRetries(Status status, Integer recruitmentRetries);
}
