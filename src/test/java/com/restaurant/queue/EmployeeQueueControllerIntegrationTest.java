package com.restaurant.queue;

import com.restaurant.queue.dto.CustomerDto;
import com.restaurant.queue.dto.EmployeeDto;
import com.restaurant.queue.model.Customer;
import com.restaurant.queue.model.Employee;
import com.restaurant.queue.repository.CustomerRepository;
import com.restaurant.queue.repository.EmployeeQueueRepository;
import com.restaurant.queue.repository.EmployeeRepository;
import com.restaurant.queue.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeQueueControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueService queueService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeQueueRepository employeeQueueRepository;

    @BeforeEach
    void clearDatabase() {
        employeeQueueRepository.deleteAll();
        customerRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    void movingOneCustomerDoesNotPromoteTheRestOfTheWaitlist() throws Exception {
        Employee firstEmployee = queueService.addEmployee(
            new EmployeeDto("first", "employee-password", "First Employee", 5)
        );
        Employee secondEmployee = queueService.addEmployee(
            new EmployeeDto("second", "employee-password", "Second Employee", 5)
        );

        Customer movedCustomer = queueService.addCustomer(
            new CustomerDto("Moved Customer", "moved@example.com", "151", "customer-password")
        );
        queueService.addCustomer(
            new CustomerDto("Next Customer", "next@example.com", "152", "customer-password")
        );
        queueService.addCustomer(
            new CustomerDto("Last Customer", "last@example.com", "153", "customer-password")
        );
        queueService.addCustomer(
            new CustomerDto("Fourth Customer", "fourth@example.com", "154", "customer-password")
        );
        queueService.addCustomer(
            new CustomerDto("Fifth Customer", "fifth@example.com", "155", "customer-password")
        );

        queueService.moveToMainQueue(movedCustomer.getId());

        org.junit.jupiter.api.Assertions.assertEquals(4, queueService.getWaitlistQueueCount());
        org.junit.jupiter.api.Assertions.assertEquals(0, queueService.getMainQueueCount());
        org.junit.jupiter.api.Assertions.assertEquals(1, employeeQueueRepository.countByEmployeeId(firstEmployee.getId())
            + employeeQueueRepository.countByEmployeeId(secondEmployee.getId()));

        Long assignedEmployeeId = employeeQueueRepository.findByEmployeeIdOrderByPositionAsc(firstEmployee.getId())
            .stream()
            .anyMatch(queue -> queue.getCustomer().getId().equals(movedCustomer.getId()))
            ? firstEmployee.getId()
            : secondEmployee.getId();

        String employeeQueues = mockMvc.perform(get("/api/queues/employee/all"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(employeeQueues.contains("Moved Customer"));
        org.junit.jupiter.api.Assertions.assertFalse(employeeQueues.contains("Next Customer"));
        org.junit.jupiter.api.Assertions.assertFalse(employeeQueues.contains("Last Customer"));
        org.junit.jupiter.api.Assertions.assertFalse(employeeQueues.contains("customer-password"));

        mockMvc.perform(post("/api/queues/employee/{employeeId}/serve/{customerId}",
                assignedEmployeeId, movedCustomer.getId()))
            .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(1, queueService.getServedCustomersCount());
        org.junit.jupiter.api.Assertions.assertEquals(0, queueService.getMainQueueCount());
        org.junit.jupiter.api.Assertions.assertEquals(0, employeeQueueRepository.countByEmployeeId(firstEmployee.getId())
            + employeeQueueRepository.countByEmployeeId(secondEmployee.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(4, queueService.getWaitlistQueueCount());
    }

    @Test
    void servingCustomerDispatchesAnAlreadyQueuedCustomerButDoesNotPullWaitlist() {
        Employee employee = queueService.addEmployee(
            new EmployeeDto("single-capacity", "employee-password", "Single Capacity", 1)
        );
        Customer firstCustomer = queueService.addCustomer(
            new CustomerDto("First Customer", "first@example.com", "611", "customer-password")
        );
        Customer secondCustomer = queueService.addCustomer(
            new CustomerDto("Second Customer", "second@example.com", "612", "customer-password")
        );
        Customer thirdCustomer = queueService.addCustomer(
            new CustomerDto("Third Customer", "third@example.com", "613", "customer-password")
        );

        queueService.moveToMainQueue(firstCustomer.getId());
        queueService.moveToMainQueue(secondCustomer.getId());

        org.junit.jupiter.api.Assertions.assertEquals(1, queueService.getWaitlistQueueCount());
        org.junit.jupiter.api.Assertions.assertEquals(1, queueService.getMainQueueCount());

        queueService.markCustomerAsServed(employee.getId(), firstCustomer.getId());

        org.junit.jupiter.api.Assertions.assertEquals(1, queueService.getWaitlistQueueCount());
        org.junit.jupiter.api.Assertions.assertEquals(0, queueService.getMainQueueCount());
        org.junit.jupiter.api.Assertions.assertEquals(1, employeeQueueRepository.countByEmployeeId(employee.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(
            secondCustomer.getId(),
            employeeQueueRepository.findByEmployeeIdOrderByPositionAsc(employee.getId()).get(0).getCustomer().getId()
        );
        org.junit.jupiter.api.Assertions.assertEquals(Customer.QueueType.WAITLIST, customerRepository.findById(thirdCustomer.getId())
            .orElseThrow()
            .getCurrentQueue());
    }
}
