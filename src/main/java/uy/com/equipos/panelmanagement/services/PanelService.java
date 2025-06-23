package uy.com.equipos.panelmanagement.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.PanelRepository;

@Service
public class PanelService {

    private final PanelRepository repository;

    public PanelService(PanelRepository repository) {
        this.repository = repository;
    }

    public Optional<Panel> get(Long id) {
        return repository.findById(id);
    }

    public Panel save(Panel entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Panel> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Panel> list(Pageable pageable, Specification<Panel> filter) {
        return repository.findAll(filter, pageable);
    }

    public Page<Panel> list(Pageable pageable, String name, LocalDate created, Boolean active) {
        Specification<Panel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (created != null) {
                predicates.add(cb.equal(root.get("created"), created));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Panel> findAll() {
        return repository.findAll();
    }

}
