package uy.com.equipos.panelmanagement.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import uy.com.equipos.panelmanagement.data.Incentive;
import uy.com.equipos.panelmanagement.data.IncentiveRepository;

@Service
public class IncentiveService {

    private final IncentiveRepository repository;

    public IncentiveService(IncentiveRepository repository) {
        this.repository = repository;
    }

    public Optional<Incentive> get(Long id) {
        return repository.findById(id);
    }

    public Incentive save(Incentive entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Incentive> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Incentive> list(Pageable pageable, Specification<Incentive> filter) {
        return repository.findAll(filter, pageable);
    }

    public Page<Incentive> list(Pageable pageable, String name, String quantityAvailableStr) {
        Specification<Incentive> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (quantityAvailableStr != null && !quantityAvailableStr.isEmpty()) {
                try {
                    Integer quantity = Integer.parseInt(quantityAvailableStr);
                    predicates.add(cb.equal(root.get("quantityAvailable"), quantity));
                } catch (NumberFormatException e) {
                    // Si no es un número válido, no se filtra por esta cantidad.
                    // Se podría loguear el error: System.err.println("Invalid number format for quantityAvailable: " + quantityAvailableStr);
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
