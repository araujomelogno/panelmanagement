package uy.com.equipos.panelmanagement.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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

    public int count() {
        return (int) repository.count();
    }

}
