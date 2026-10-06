package com.restaurant.queue.controller;

import com.restaurant.queue.dto.CustomerDto;
import com.restaurant.queue.dto.EmployeeDto;
import com.restaurant.queue.model.Customer;
import com.restaurant.queue.model.Employee;
import com.restaurant.queue.model.EmployeeQueue;
import com.restaurant.queue.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @PostMapping("/customers")
    public ResponseEntity<?> addCustomer(@RequestBody CustomerDto customerDto) {
        try {
            Customer customer = queueService.addCustomer(customerDto);
            return ResponseEntity.ok(customer);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/employees")
    public ResponseEntity<?> addEmployee(@RequestBody EmployeeDto employeeDto) {
        try {
            Employee employee = queueService.addEmployee(employeeDto);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/queues/main/{customerId}")
    public ResponseEntity<?> moveToMainQueue(@PathVariable Long customerId) {
        try {
            queueService.moveToMainQueue(customerId);
            return ResponseEntity.ok(Map.of("message", "Customer moved to main queue"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/queues/employee/{employeeId}/pull")
    public ResponseEntity<?> pullCustomerToEmployeeQueue(@PathVariable Long employeeId) {
        try {
            queueService.pullCustomerToEmployeeQueue(employeeId);
            return ResponseEntity.ok(Map.of("message", "Customer pulled to employee queue"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/queues/assign-shortest")
    public ResponseEntity<?> assignCustomerToShortestQueue() {
        try {
            Employee employee = queueService.assignCustomerToShortestQueue();
            return ResponseEntity.ok(Map.of(
                "message", "Customer assigned to employee with shortest queue",
                "employeeName", employee.getName(),
                "employeeId", employee.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/queues/waitlist")
    public ResponseEntity<List<Customer>> getWaitlistQueue() {
        return ResponseEntity.ok(queueService.getWaitlistQueue());
    }

    @GetMapping("/queues/main")
    public ResponseEntity<List<Customer>> getMainQueue() {
        return ResponseEntity.ok(queueService.getMainQueue());
    }

    @GetMapping("/queues/employee/{employeeId}")
    public ResponseEntity<List<EmployeeQueue>> getEmployeeQueue(@PathVariable Long employeeId) {
        return ResponseEntity.ok(queueService.getEmployeeQueue(employeeId));
    }

    @GetMapping("/queues/employee/all")
    public ResponseEntity<Map<String, Object>> getAllEmployeeQueues() {
        return ResponseEntity.ok(queueService.getAllEmployeeQueues());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(queueService.getAllEmployees());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "waitlistQueueCount", queueService.getWaitlistQueueCount(),
            "waitlistQueueLimit", 1000,
            "mainQueueCount", queueService.getMainQueueCount(),
            "mainQueueLimit", 1000,
            "servedCustomersCount", queueService.getServedCustomersCount()
        ));
    }

    @PostMapping("/queues/employee/{employeeId}/serve/{customerId}")
    public ResponseEntity<?> markCustomerAsServed(@PathVariable Long employeeId, @PathVariable Long customerId) {
        try {
            queueService.markCustomerAsServed(employeeId, customerId);
            return ResponseEntity.ok(Map.of("message", "Customer marked as served"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/queues/served")
    public ResponseEntity<List<Customer>> getServedCustomers() {
        return ResponseEntity.ok(queueService.getServedCustomers());
    }
}
