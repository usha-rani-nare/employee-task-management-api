package com.usharani.taskmanager.controller;

import com.usharani.taskmanager.model.Task;
import com.usharani.taskmanager.model.TaskStatus;
import com.usharani.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        Task saved = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PutMapping("/{taskId}/assign/{employeeId}")
    public Task assignTask(@PathVariable Long taskId, @PathVariable Long employeeId) {
        return taskService.assignTask(taskId, employeeId);
    }

    // body looks like: { "status": "IN_PROGRESS" }
    @PatchMapping("/{id}/status")
    public Task updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        TaskStatus status = TaskStatus.valueOf(body.get("status").toUpperCase());
        return taskService.updateStatus(id, status);
    }

    @GetMapping("/employee/{employeeId}")
    public List<Task> getTasksByEmployee(@PathVariable Long employeeId) {
        return taskService.getTasksByEmployee(employeeId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
