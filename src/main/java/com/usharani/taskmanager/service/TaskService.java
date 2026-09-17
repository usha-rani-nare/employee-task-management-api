package com.usharani.taskmanager.service;

import com.usharani.taskmanager.exception.ResourceNotFoundException;
import com.usharani.taskmanager.model.Employee;
import com.usharani.taskmanager.model.Task;
import com.usharani.taskmanager.model.TaskStatus;
import com.usharani.taskmanager.repository.EmployeeRepository;
import com.usharani.taskmanager.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    public Task assignTask(Long taskId, Long employeeId) {
        Task task = getTaskById(taskId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        task.setAssignedTo(employee);
        return taskRepository.save(task);
    }

    public Task updateStatus(Long taskId, TaskStatus status) {
        Task task = getTaskById(taskId);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    public List<Task> getTasksByEmployee(Long employeeId) {
        return taskRepository.findByAssignedToId(employeeId);
    }

    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }
}
