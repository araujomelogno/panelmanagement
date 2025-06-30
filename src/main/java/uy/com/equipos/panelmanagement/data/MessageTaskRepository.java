package uy.com.equipos.panelmanagement.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MessageTaskRepository extends JpaRepository<MessageTask, Long>, JpaSpecificationExecutor<MessageTask> {

    List<MessageTask> findAllByJobTypeAndStatus(JobType jobType, MessageTaskStatus status);
}
