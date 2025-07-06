package uy.com.equipos.panelmanagement.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.Task;
import uy.com.equipos.panelmanagement.data.TaskRepository;
import uy.com.equipos.panelmanagement.data.TaskStatus;

@Service
public class TaskService {

    private final TaskRepository repository;

    @Autowired
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public Task save(Task messageTask) {
        return repository.save(messageTask);
    }

    public List<Task> findAllByJobTypeAndStatus(JobType jobType, TaskStatus status) {
        return repository.findAllByJobTypeAndStatus(jobType, status);
    }

    public List<Task> findAll() {
        return repository.findAll();
    }

}
