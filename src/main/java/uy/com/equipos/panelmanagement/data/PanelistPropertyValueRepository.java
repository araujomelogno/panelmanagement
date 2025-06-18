package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List; // Added for findByPanelist

public interface PanelistPropertyValueRepository
        extends JpaRepository<PanelistPropertyValue, Long>,
                JpaSpecificationExecutor<PanelistPropertyValue> {

    // Method to find all property values for a given panelist
    List<PanelistPropertyValue> findByPanelist(Panelist panelist);

    // Optional: Method to find a specific property value for a panelist and property
    // Optional<PanelistPropertyValue> findByPanelistAndPanelistProperty(Panelist panelist, PanelistProperty panelistProperty);
}
