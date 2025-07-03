package uy.com.equipos.panelmanagement.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistRepository;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PanelistService {

    private final PanelistRepository repository;

    public PanelistService(PanelistRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Panelist> get(Long id) {
        Optional<Panelist> panelistOptional = repository.findById(id);
        panelistOptional.ifPresent(panelist -> {
            Hibernate.initialize(panelist.getPropertyValues());
            Hibernate.initialize(panelist.getParticipations()); // Actualizado a participations
        });
        return panelistOptional;
    }

    // El método findByIdWithSurveys ya no es necesario o debe ser reemplazado
    // por una lógica que cargue las participaciones si se requiere.
    // Por ahora, lo comentamos o eliminamos.
    // @Transactional(readOnly = true)
    // public Optional<Panelist> findByIdWithParticipations(Long id) {
    //    return repository.findByIdWithParticipations(id);
    // }

    // Renamed from findByIdWithSurveys to better reflect its action due to get() initializing participations
    @Transactional(readOnly = true)
    public Optional<Panelist> findByIdWithParticipations(Long id) {
        // The existing get() method already initializes participations.
        return get(id);
    }

    public Panelist save(Panelist entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Panelist> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Panelist> list(Pageable pageable, Specification<Panelist> filter) {
        return repository.findAll(filter, pageable);
    }

    public Page<Panelist> list(Pageable pageable, String firstName, String lastName, String email, String phone,
                               LocalDate lastContacted, LocalDate lastInterviewed) {
        Specification<Panelist> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (firstName != null && !firstName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
            }
            if (lastName != null && !lastName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (phone != null && !phone.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("phone")), "%" + phone.toLowerCase() + "%"));
            }
            // Removed dateOfBirth filter logic
            // Removed occupation filter logic
            if (lastContacted != null) {
                predicates.add(cb.equal(root.get("lastContacted"), lastContacted));
            }
            if (lastInterviewed != null) {
                predicates.add(cb.equal(root.get("lastInterviewed"), lastInterviewed));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    @Transactional(readOnly = true)
    public List<Panelist> findPanelistsByCriteria(Map<PanelistProperty, Object> filterCriteria) {
        if (filterCriteria == null || filterCriteria.isEmpty()) {
            return repository.findAll(); // Or an empty list, as per requirements
        }
        // Remove entries with null or blank string values before passing to repository
        Map<PanelistProperty, Object> finalCriteria = filterCriteria.entrySet().stream()
            .filter(entry -> entry.getValue() != null)
            .filter(entry -> !(entry.getValue() instanceof String) || !((String) entry.getValue()).isBlank())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (finalCriteria.isEmpty()) {
             return repository.findAll(); // Or an empty list
        }
        return repository.findByCriteria(finalCriteria);
    }

    @Transactional
    public void removePanelFromPanelist(Long panelistId, Long panelId) {
        Optional<Panelist> panelistOpt = repository.findById(panelistId);
        if (panelistOpt.isPresent()) {
            Panelist panelist = panelistOpt.get();
            // Initialize panels collection if it's LAZY and not already fetched.
            // Since it's EAGER in Panelist.java, this explicit initialization might not be strictly necessary
            // but doesn't harm and ensures the collection is loaded if fetching strategy changes.
            Hibernate.initialize(panelist.getPanels());

            Optional<Panel> panelToRemoveOpt = panelist.getPanels().stream()
                    .filter(panel -> panel.getId().equals(panelId))
                    .findFirst();

            if (panelToRemoveOpt.isPresent()) {
                panelist.getPanels().remove(panelToRemoveOpt.get());
                // No explicit call to panelRepository is needed here for the Panel side
                // as Panelist is the owning side. Changes to panelist.getPanels()
                // will be persisted when the transaction commits after this method.
                // repository.save(panelist); // This save is implicit due to @Transactional
                                         // if the entity is managed and dirty.
                                         // However, to be absolutely explicit, especially if not relying
                                         // solely on dirty checking or if outside a transaction elsewhere,
                                         // an explicit save is safer. Given @Transactional, it should persist.
            } else {
                // Optionally, throw an exception or log if the panel is not associated
                // System.out.println("Panel with ID " + panelId + " not found in panelist's panels.");
                 throw new EntityNotFoundException("Panel with ID " + panelId + " not found in panelist's associated panels.");
            }
        } else {
            // Optionally, throw an exception or log if the panelist is not found
            // System.out.println("Panelist with ID " + panelistId + " not found.");
            throw new EntityNotFoundException("Panelist with ID " + panelistId + " not found.");
        }
    }
}
