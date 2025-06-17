package uy.com.equipos.panelmanagement.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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

    public int count() {
        return (int) repository.count();
    }

}
