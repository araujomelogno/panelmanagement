package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph; // Added import
import java.util.Optional; // Added import

public interface PanelistPropertyRepository
        extends
            JpaRepository<PanelistProperty, Long>,
            JpaSpecificationExecutor<PanelistProperty> {

    @EntityGraph(attributePaths = {"codes"})
    Optional<PanelistProperty> findByIdFetchingCodes(Long id);
}
