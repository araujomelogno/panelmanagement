package uy.com.equipos.panelmanagement.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import uy.com.equipos.panelmanagement.data.AppUser;
import uy.com.equipos.panelmanagement.data.AppUserRepository;

@Service
public class AppUserService {

    private final AppUserRepository repository;

    public AppUserService(AppUserRepository repository) {
        this.repository = repository;
    }

    public Optional<AppUser> get(Long id) {
        return repository.findById(id);
    }

    public AppUser save(AppUser entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<AppUser> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<AppUser> list(Pageable pageable, Specification<AppUser> filter) {
        return repository.findAll(filter, pageable);
    }

    public Page<AppUser> list(Pageable pageable, String name, String email) {
        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
