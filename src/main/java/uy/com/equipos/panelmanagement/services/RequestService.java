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

    public Page<Request> list(Pageable pageable,
                              String firstNameFilter, String lastNameFilter, String birhtdateFilter,
                              String sexFilter, String emailFilter, String phoneFilter) {

        Specification<Request> finalSpec = Specification.where(null); // Start with a base, no-op specification

        if (firstNameFilter != null && !firstNameFilter.trim().isEmpty()) {
            String lowerCaseFilter = firstNameFilter.trim().toLowerCase();
            finalSpec = finalSpec.and((root, query, cb) -> cb.like(cb.lower(root.get("firstName")), "%" + lowerCaseFilter + "%"));
        }
        if (lastNameFilter != null && !lastNameFilter.trim().isEmpty()) {
            String lowerCaseFilter = lastNameFilter.trim().toLowerCase();
            finalSpec = finalSpec.and((root, query, cb) -> cb.like(cb.lower(root.get("lastName")), "%" + lowerCaseFilter + "%"));
        }
        if (birhtdateFilter != null && !birhtdateFilter.trim().isEmpty()) {
            String lowerCaseFilter = birhtdateFilter.trim().toLowerCase();
            // Assuming 'birhtdate' is the correct field name in the Request entity
            finalSpec = finalSpec.and((root, query, cb) -> cb.like(cb.lower(root.get("birhtdate")), "%" + lowerCaseFilter + "%"));
        }
        if (sexFilter != null && !sexFilter.trim().isEmpty()) {
            String lowerCaseFilter = sexFilter.trim().toLowerCase();
            finalSpec = finalSpec.and((root, query, cb) -> cb.like(cb.lower(root.get("sex")), "%" + lowerCaseFilter + "%"));
        }
        if (emailFilter != null && !emailFilter.trim().isEmpty()) {
            String lowerCaseFilter = emailFilter.trim().toLowerCase();
            finalSpec = finalSpec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + lowerCaseFilter + "%"));
        }
        if (phoneFilter != null && !phoneFilter.trim().isEmpty()) {
            String lowerCaseFilter = phoneFilter.trim().toLowerCase();
            finalSpec = finalSpec.and((root, query, cb) -> cb.like(cb.lower(root.get("phone")), "%" + lowerCaseFilter + "%"));
        }

        return repository.findAll(finalSpec, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
