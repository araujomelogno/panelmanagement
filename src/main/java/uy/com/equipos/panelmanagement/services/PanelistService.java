package uy.com.equipos.panelmanagement.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistRepository;

@Service
public class PanelistService {

    private final PanelistRepository repository;

    public PanelistService(PanelistRepository repository) {
        this.repository = repository;
    }

    public Optional<Panelist> get(Long id) {
        return repository.findById(id);
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

    public int count() {
        return (int) repository.count();
    }

}
