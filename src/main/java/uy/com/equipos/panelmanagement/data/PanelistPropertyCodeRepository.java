package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List; // Add this import

public interface PanelistPropertyCodeRepository
        extends
            JpaRepository<PanelistPropertyCode, Long>,
            JpaSpecificationExecutor<PanelistPropertyCode> {

    // Add this method
    List<PanelistPropertyCode> findByPanelistProperty(PanelistProperty panelistProperty);
}
