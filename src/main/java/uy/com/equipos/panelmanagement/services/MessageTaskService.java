package uy.com.equipos.panelmanagement.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.com.equipos.panelmanagement.data.MessageTask;
import uy.com.equipos.panelmanagement.data.MessageTaskRepository;

@Service
public class MessageTaskService {

    private final MessageTaskRepository repository;

    @Autowired
    public MessageTaskService(MessageTaskRepository repository) {
        this.repository = repository;
    }

    public MessageTask save(MessageTask messageTask) {
        return repository.save(messageTask);
    }

    // Optional: Add other common service methods like list, get, delete if needed for future use.
    // public Optional<MessageTask> get(Long id) {
    //     return repository.findById(id);
    // }

    // public void delete(Long id) {
    //     repository.deleteById(id);
    // }

    // public Page<MessageTask> list(Pageable pageable) {
    //     return repository.findAll(pageable);
    // }

    // public Page<MessageTask> list(Pageable pageable, Specification<MessageTask> filter) {
    //     return repository.findAll(filter, pageable);
    // }
}
