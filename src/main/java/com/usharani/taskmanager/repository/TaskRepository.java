package com.usharani.taskmanager.repository;

import com.usharani.taskmanager.model.Task;
import com.usharani.taskmanager.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedToId(Long employeeId);
    List<Task> findByStatus(TaskStatus status);
}
