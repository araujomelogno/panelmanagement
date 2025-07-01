package uy.com.equipos.panelmanagement.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.MessageTask;
import uy.com.equipos.panelmanagement.data.MessageTaskRepository;
import uy.com.equipos.panelmanagement.data.MessageTaskStatus;

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

    public List<MessageTask> findAllByJobTypeAndStatus(JobType jobType, MessageTaskStatus status) {
        return repository.findAllByJobTypeAndStatus(jobType, status);
    }

    public List<MessageTask> findAll() {
        return repository.findAll();
    }

}
