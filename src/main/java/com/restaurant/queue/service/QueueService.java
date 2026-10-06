package com.restaurant.queue.service;

import com.restaurant.queue.dto.CustomerDto;
import com.restaurant.queue.dto.EmployeeDto;
import com.restaurant.queue.model.Customer;
import com.restaurant.queue.model.Employee;
import com.restaurant.queue.model.EmployeeQueue;
import com.restaurant.queue.repository.CustomerRepository;
import com.restaurant.queue.repository.EmployeeQueueRepository;
import com.restaurant.queue.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class QueueService {

    private static final int WAITLIST_QUEUE_LIMIT = 1000;
    private static final int MAIN_QUEUE_LIMIT = 1000;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeQueueRepository employeeQueueRepository;

    private final Set<String> waitlistQueueEmails = new HashSet<>();
    private final Set<String> waitlistQueuePhones = new HashSet<>();
    private final Set<String> mainQueueEmails = new HashSet<>();
    private final Set<String> mainQueuePhones = new HashSet<>();

    @Transactional
    public Customer addCustomer(CustomerDto customerDto) {
        if (customerRepository.existsByEmail(customerDto.getEmail())) {
            throw new RuntimeException("Customer with this email already exists");
        }
        if (customerRepository.existsByPhone(customerDto.getPhone())) {
            throw new RuntimeException("Customer with this phone already exists");
        }

        long waitlistCount = customerRepository.countByCurrentQueue(Customer.QueueType.WAITLIST);
        if (waitlistCount >= WAITLIST_QUEUE_LIMIT) {
            throw new RuntimeException("Waitlist is full (limit: " + WAITLIST_QUEUE_LIMIT + ")");
        }

        if (waitlistQueueEmails.contains(customerDto.getEmail()) || waitlistQueuePhones.contains(customerDto.getPhone())) {
            throw new RuntimeException("Customer already in waitlist");
        }

        Customer customer = new Customer(
            customerDto.getName(),
            customerDto.getEmail(),
            customerDto.getPhone(),
            customerDto.getPassword()
        );
        customer.setCurrentQueue(Customer.QueueType.WAITLIST);

        waitlistQueueEmails.add(customerDto.getEmail());
        waitlistQueuePhones.add(customerDto.getPhone());

        customer = customerRepository.save(customer);
        updateQueuePositions();
        return customer;
    }

    @Transactional
    public Employee addEmployee(EmployeeDto employeeDto) {
        if (employeeRepository.existsByUsername(employeeDto.getUsername())) {
            throw new RuntimeException("Employee with this username already exists");
        }

        Employee employee = new Employee(
            employeeDto.getUsername(),
            employeeDto.getPassword(),
            employeeDto.getName(),
            employeeDto.getQueueLimit()
        );

        return employeeRepository.save(employee);
    }

    @Transactional
    public void moveToMainQueue(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getCurrentQueue() != Customer.QueueType.WAITLIST) {
            throw new RuntimeException("Customer is not in waitlist");
        }

        long mainQueueCount = customerRepository.countByCurrentQueue(Customer.QueueType.MAIN_QUEUE);
        if (mainQueueCount >= MAIN_QUEUE_LIMIT) {
            throw new RuntimeException("Main queue is full (limit: " + MAIN_QUEUE_LIMIT + ")");
        }

        if (mainQueueEmails.contains(customer.getEmail()) || mainQueuePhones.contains(customer.getPhone())) {
            throw new RuntimeException("Customer already in main queue");
        }

        waitlistQueueEmails.remove(customer.getEmail());
        waitlistQueuePhones.remove(customer.getPhone());

        customer.setCurrentQueue(Customer.QueueType.MAIN_QUEUE);
        customer.setJoinedAt(java.time.LocalDateTime.now());
        customerRepository.save(customer);

        mainQueueEmails.add(customer.getEmail());
        mainQueuePhones.add(customer.getPhone());

        updateQueuePositions();
        dispatchAvailableCustomers();
    }

    @Transactional
    public void pullCustomerToEmployeeQueue(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found"));

        long currentQueueSize = employeeQueueRepository.countByEmployeeId(employeeId);
        if (currentQueueSize >= employee.getQueueLimit()) {
            throw new RuntimeException("Employee queue is full (limit: " + employee.getQueueLimit() + ")");
        }

        List<Customer> mainQueue = customerRepository.findByCurrentQueueOrderByJoinedAt(Customer.QueueType.MAIN_QUEUE);
        if (mainQueue.isEmpty()) {
            throw new RuntimeException("No customers in main queue");
        }

        Customer customer = mainQueue.get(0);

        // Check if customer is already in any employee queue
        for (Employee emp : employeeRepository.findAll()) {
            if (employeeQueueRepository.findByEmployeeIdAndCustomerId(emp.getId(), customer.getId()).isPresent()) {
                throw new RuntimeException("Customer is already in another employee's queue");
            }
        }

        mainQueueEmails.remove(customer.getEmail());
        mainQueuePhones.remove(customer.getPhone());

        customer.setCurrentQueue(Customer.QueueType.EMPLOYEE_QUEUE);
        customerRepository.save(customer);

        int newPosition = (int) currentQueueSize + 1;
        EmployeeQueue employeeQueue = new EmployeeQueue(employee, customer, newPosition);
        employeeQueueRepository.save(employeeQueue);

        updateQueuePositions();

        dispatchAvailableCustomers();
    }

    @Transactional
    public Employee assignCustomerToShortestQueue() {
        List<Customer> mainQueue = customerRepository.findByCurrentQueueOrderByJoinedAt(Customer.QueueType.MAIN_QUEUE);
        if (mainQueue.isEmpty()) {
            throw new RuntimeException("No customers in main queue");
        }

        Customer customer = mainQueue.get(0);

        // Check if customer is already in any employee queue
        for (Employee emp : employeeRepository.findAll()) {
            if (employeeQueueRepository.findByEmployeeIdAndCustomerId(emp.getId(), customer.getId()).isPresent()) {
                throw new RuntimeException("Customer is already in another employee's queue");
            }
        }

        List<Employee> employees = employeeRepository.findAll();
        if (employees.isEmpty()) {
            throw new RuntimeException("No employees available");
        }

        Employee shortestQueueEmployee = null;
        long minQueueSize = Long.MAX_VALUE;

        for (Employee employee : employees) {
            long currentQueueSize = employeeQueueRepository.countByEmployeeId(employee.getId());
            if (currentQueueSize < employee.getQueueLimit() && currentQueueSize < minQueueSize) {
                minQueueSize = currentQueueSize;
                shortestQueueEmployee = employee;
            }
        }

        if (shortestQueueEmployee == null) {
            throw new RuntimeException("All employee queues are full");
        }

        if (employeeQueueRepository.findByEmployeeIdAndCustomerId(shortestQueueEmployee.getId(), customer.getId()).isPresent()) {
            throw new RuntimeException("Customer already in this employee's queue");
        }

        mainQueueEmails.remove(customer.getEmail());
        mainQueuePhones.remove(customer.getPhone());

        customer.setCurrentQueue(Customer.QueueType.EMPLOYEE_QUEUE);
        customerRepository.save(customer);

        int newPosition = (int) minQueueSize + 1;
        EmployeeQueue employeeQueue = new EmployeeQueue(shortestQueueEmployee, customer, newPosition);
        employeeQueueRepository.save(employeeQueue);

        updateQueuePositions();

        dispatchAvailableCustomers();

        return shortestQueueEmployee;
    }

    @Transactional
    public void updateQueuePositions() {
        List<Customer> waitlistQueue = customerRepository.findByCurrentQueueOrderByJoinedAt(Customer.QueueType.WAITLIST);
        for (int i = 0; i < waitlistQueue.size(); i++) {
            waitlistQueue.get(i).setPositionInQueue(i + 1);
        }
        customerRepository.saveAll(waitlistQueue);

        List<Customer> mainQueue = customerRepository.findByCurrentQueueOrderByJoinedAt(Customer.QueueType.MAIN_QUEUE);
        for (int i = 0; i < mainQueue.size(); i++) {
            mainQueue.get(i).setPositionInQueue(i + 1);
        }
        customerRepository.saveAll(mainQueue);
    }

    public List<Customer> getWaitlistQueue() {
        return customerRepository.findByCurrentQueueOrderByJoinedAt(Customer.QueueType.WAITLIST);
    }

    public List<Customer> getMainQueue() {
        return customerRepository.findByCurrentQueueOrderByJoinedAt(Customer.QueueType.MAIN_QUEUE);
    }

    public List<EmployeeQueue> getEmployeeQueue(Long employeeId) {
        return employeeQueueRepository.findByEmployeeIdOrderByPositionAsc(employeeId);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Customer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public Optional<Employee> getEmployeeByUsername(String username) {
        return employeeRepository.findByUsername(username);
    }

    public int getWaitlistQueueCount() {
        return (int) customerRepository.countByCurrentQueue(Customer.QueueType.WAITLIST);
    }

    public int getMainQueueCount() {
        return (int) customerRepository.countByCurrentQueue(Customer.QueueType.MAIN_QUEUE);
    }

    @Transactional
    public void markCustomerAsServed(Long employeeId, Long customerId) {
        EmployeeQueue employeeQueue = employeeQueueRepository.findByEmployeeIdAndCustomerId(employeeId, customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found in employee queue"));

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setCurrentQueue(Customer.QueueType.SERVED);
        customerRepository.save(customer);

        employeeQueueRepository.delete(employeeQueue);

        updateQueuePositions();

        dispatchAvailableCustomers();
    }

    @Transactional
    private void dispatchAvailableCustomers() {
        while (true) {
            List<Customer> mainQueue = customerRepository.findByCurrentQueueOrderByJoinedAt(Customer.QueueType.MAIN_QUEUE);
            if (mainQueue.isEmpty()) {
                break;
            }

            Customer customer = mainQueue.get(0);
            Employee availableEmployee = null;
            long shortestQueueSize = Long.MAX_VALUE;

            for (Employee employee : employeeRepository.findAll()) {
                long queueSize = employeeQueueRepository.countByEmployeeId(employee.getId());
                if (queueSize < employee.getQueueLimit() && queueSize < shortestQueueSize) {
                    availableEmployee = employee;
                    shortestQueueSize = queueSize;
                }
            }

            if (availableEmployee == null) {
                break;
            }

            mainQueueEmails.remove(customer.getEmail());
            mainQueuePhones.remove(customer.getPhone());
            customer.setCurrentQueue(Customer.QueueType.EMPLOYEE_QUEUE);
            customerRepository.save(customer);
            employeeQueueRepository.save(new EmployeeQueue(
                availableEmployee,
                customer,
                (int) shortestQueueSize + 1
            ));
            updateQueuePositions();
        }
    }

    public List<Customer> getServedCustomers() {
        return customerRepository.findByCurrentQueueOrderByJoinedAt(Customer.QueueType.SERVED);
    }

    public int getServedCustomersCount() {
        return (int) customerRepository.countByCurrentQueue(Customer.QueueType.SERVED);
    }

    public Map<String, Object> getAllEmployeeQueues() {
        List<Employee> employees = employeeRepository.findAll();
        Map<String, Object> result = new java.util.HashMap<>();
        
        for (Employee employee : employees) {
            List<EmployeeQueue> queue = employeeQueueRepository.findByEmployeeIdOrderByPositionAsc(employee.getId());
            result.put(employee.getName(), queue);
        }
        
        return result;
    }
}
