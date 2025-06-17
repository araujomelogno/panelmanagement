package uy.com.equipos.panelmanagement.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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

    public int count() {
        return (int) repository.count();
    }

}
