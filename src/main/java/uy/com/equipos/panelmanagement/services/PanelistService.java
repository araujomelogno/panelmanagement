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
import jakarta.persistence.criteria.Predicate;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistRepository;

@Service
public class PanelistService {

    private final PanelistRepository repository;

    public PanelistService(PanelistRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Panelist> get(Long id) {
        Optional<Panelist> panelistOptional = repository.findById(id);
        panelistOptional.ifPresent(panelist -> Hibernate.initialize(panelist.getPropertyValues()));
        return panelistOptional;
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
                               LocalDate dateOfBirth, String occupation, LocalDate lastContacted, LocalDate lastInterviewed) {
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
            if (dateOfBirth != null) {
                predicates.add(cb.equal(root.get("dateOfBirth"), dateOfBirth));
            }
            if (occupation != null && !occupation.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("occupation")), "%" + occupation.toLowerCase() + "%"));
            }
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

}
