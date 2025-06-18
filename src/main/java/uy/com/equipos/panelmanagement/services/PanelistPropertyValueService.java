package uy.com.equipos.panelmanagement.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Added for transactional methods
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistPropertyValue;
import uy.com.equipos.panelmanagement.data.PanelistPropertyValueRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PanelistPropertyValueService {

    private final PanelistPropertyValueRepository repository;

    public PanelistPropertyValueService(PanelistPropertyValueRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PanelistPropertyValue> findByPanelist(Panelist panelist) {
        return repository.findByPanelist(panelist);
    }

    @Transactional(readOnly = true)
    public Optional<PanelistPropertyValue> findByPanelistAndPanelistProperty(Panelist panelist, PanelistProperty panelistProperty) {
        // This would require a custom query in the repository, e.g.:
        // return repository.findByPanelistAndPanelistProperty(panelist, panelistProperty);
        // For now, implementing it by filtering the list from findByPanelist:
        return findByPanelist(panelist).stream()
                .filter(pv -> pv.getPanelistProperty().equals(panelistProperty))
                .findFirst();
    }

    @Transactional
    public PanelistPropertyValue save(PanelistPropertyValue entity) {
        return repository.save(entity);
    }

    @Transactional
    public List<PanelistPropertyValue> saveAll(List<PanelistPropertyValue> entities) {
        return repository.saveAll(entities);
    }

    @Transactional
    public void delete(PanelistPropertyValue entity) {
        repository.delete(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void deleteAll(List<PanelistPropertyValue> entities) {
        repository.deleteAll(entities);
    }
}
