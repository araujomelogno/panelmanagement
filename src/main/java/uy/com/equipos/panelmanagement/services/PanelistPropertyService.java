package uy.com.equipos.panelmanagement.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistPropertyRepository;

@Service
public class PanelistPropertyService {

    private final PanelistPropertyRepository repository;

    public PanelistPropertyService(PanelistPropertyRepository repository) {
        this.repository = repository;
    }

    public Optional<PanelistProperty> get(Long id) {
        return repository.findById(id);
    }

    public PanelistProperty save(PanelistProperty entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<PanelistProperty> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<PanelistProperty> list(Pageable pageable, Specification<PanelistProperty> filter) {
        return repository.findAll(filter, pageable);
    }

    public Page<PanelistProperty> list(Pageable pageable, String name, String type) {
        Specification<PanelistProperty> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (type != null && !type.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("type")), "%" + type.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
