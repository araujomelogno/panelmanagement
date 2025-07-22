package uy.com.equipos.panelmanagement.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.equipos.panelmanagement.data.ConfigurationItem;
import uy.com.equipos.panelmanagement.data.ConfigurationItemRepository;

@Service
public class ConfigurationItemService {

    private final ConfigurationItemRepository repository;

    public ConfigurationItemService(ConfigurationItemRepository repository) {
        this.repository = repository;
    }

    public Optional<ConfigurationItem> get(Long id) {
        return repository.findById(id);
    }

    public Optional<ConfigurationItem> getByName(String name) {
        return repository.findByName(name);
    }

    public ConfigurationItem update(ConfigurationItem entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ConfigurationItem> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<ConfigurationItem> list(Pageable pageable, Specification<ConfigurationItem> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
