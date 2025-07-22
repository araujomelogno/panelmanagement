package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConfigurationItemRepository
        extends
            JpaRepository<ConfigurationItem, Long>,
            JpaSpecificationExecutor<ConfigurationItem> {

    ConfigurationItem findByName(String name);

}
