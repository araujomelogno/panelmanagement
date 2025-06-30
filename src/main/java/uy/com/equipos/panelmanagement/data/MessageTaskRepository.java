package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.MessageTaskStatus;

public interface MessageTaskRepository extends JpaRepository<MessageTask, Long>, JpaSpecificationExecutor<MessageTask> {

    List<MessageTask> findAllByJobTypeAndStatus(JobType jobType, MessageTaskStatus status);
}
