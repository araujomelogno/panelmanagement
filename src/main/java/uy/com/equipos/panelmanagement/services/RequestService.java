package uy.com.equipos.panelmanagement.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.equipos.panelmanagement.data.Request;
import uy.com.equipos.panelmanagement.data.RequestRepository;

@Service
public class RequestService {

    private final RequestRepository repository;

    public RequestService(RequestRepository repository) {
        this.repository = repository;
    }

    public Optional<Request> get(Long id) {
        return repository.findById(id);
    }

    public Request save(Request entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Request> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Request> list(Pageable pageable, Specification<Request> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
