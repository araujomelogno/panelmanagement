package uy.com.equipos.panelmanagement.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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

    public int count() {
        return (int) repository.count();
    }

}
