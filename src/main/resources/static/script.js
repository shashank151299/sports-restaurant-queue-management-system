const API_BASE = '/api';

async function fetchAPI(endpoint, options = {}) {
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

async function loadStats() {
    try {
        const stats = await fetchAPI('/stats');
        document.getElementById('waitlistCount').textContent = `${stats.waitlistQueueCount} / ${stats.waitlistQueueLimit}`;
        document.getElementById('mainQueueCount').textContent = `${stats.mainQueueCount} / ${stats.mainQueueLimit}`;
        document.getElementById('servedCount').textContent = stats.servedCustomersCount;
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

async function loadWaitlistQueue() {
    try {
        const queue = await fetchAPI('/queues/waitlist');
        const container = document.getElementById('waitlistQueueList');
        container.innerHTML = queue.map(customer => `
            <div class="queue-item">
                <span class="position">#${customer.positionInQueue}</span>
                <h4>${customer.name}</h4>
                <p>Email: ${customer.email}</p>
                <p>Phone: ${customer.phone}</p>
                <p>Joined: ${new Date(customer.joinedAt).toLocaleString()}</p>
                <button onclick="moveToMain(${customer.id})" style="margin-top: 10px; padding: 5px 10px; cursor: pointer;">Move to Main Queue</button>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading waitlist queue:', error);
    }
}

async function loadMainQueue() {
    try {
        const queue = await fetchAPI('/queues/main');
        const container = document.getElementById('mainQueueList');
        container.innerHTML = queue.map(customer => `
            <div class="queue-item">
                <span class="position">#${customer.positionInQueue}</span>
                <h4>${customer.name}</h4>
                <p>Email: ${customer.email}</p>
                <p>Phone: ${customer.phone}</p>
                <p>Joined: ${new Date(customer.joinedAt).toLocaleString()}</p>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading main queue:', error);
    }
}

async function loadEmployees() {
    try {
        const employees = await fetchAPI('/employees');
        // Employees are loaded for reference but no dropdown needed
    } catch (error) {
        console.error('Error loading employees:', error);
    }
}

async function loadEmployeeQueue(employeeId) {
    // No longer needed - employee queue is automatic
}

async function addCustomer(event) {
    event.preventDefault();
    const customerData = {
        name: document.getElementById('customerName').value,
        email: document.getElementById('customerEmail').value,
        phone: document.getElementById('customerPhone').value,
        password: document.getElementById('customerPassword').value
    };

    try {
        await fetchAPI('/customers', {
            method: 'POST',
            body: JSON.stringify(customerData)
        });
        alert('Customer added to waitlist successfully!');
        document.getElementById('customerForm').reset();
        loadAllData();
    } catch (error) {
        alert('Error adding customer: ' + error.message);
    }
}

async function addEmployee(event) {
    event.preventDefault();
    const employeeData = {
        username: document.getElementById('employeeUsername').value,
        password: document.getElementById('employeePassword').value,
        name: document.getElementById('employeeName').value,
        queueLimit: parseInt(document.getElementById('employeeQueueLimit').value)
    };

    try {
        await fetchAPI('/employees', {
            method: 'POST',
            body: JSON.stringify(employeeData)
        });
        alert('Employee added successfully!');
        document.getElementById('employeeForm').reset();
        loadAllData();
    } catch (error) {
        alert('Error adding employee: ' + error.message);
    }
}

async function moveToMain(customerId) {
    try {
        await fetchAPI(`/queues/main/${customerId}`, {
            method: 'POST'
        });
        alert('Customer moved out of the waitlist. Available employees receive customers automatically; customers stay in the main queue when all employee queues are full.');
        loadAllData();
    } catch (error) {
        alert('Error moving customer: ' + error.message);
    }
}

async function markAsServed(employeeId, customerId) {
    try {
        await fetchAPI(`/queues/employee/${employeeId}/serve/${customerId}`, {
            method: 'POST'
        });
        alert('Customer marked as served!');
        loadAllData();
    } catch (error) {
        alert('Error marking customer as served: ' + error.message);
    }
}

async function loadServedCustomers() {
    try {
        const customers = await fetchAPI('/queues/served');
        const container = document.getElementById('servedCustomersList');
        container.innerHTML = customers.map(customer => `
            <div class="queue-item">
                <h4>${customer.name}</h4>
                <p>Email: ${customer.email}</p>
                <p>Phone: ${customer.phone}</p>
                <p>Served: ${new Date(customer.joinedAt).toLocaleString()}</p>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading served customers:', error);
    }
}

async function loadEmployeeServiceStatus() {
    try {
        const allQueues = await fetchAPI('/queues/employee/all');
        const container = document.getElementById('employeeServiceStatus');
        
        let html = '';
        for (const [employeeName, queue] of Object.entries(allQueues)) {
            if (queue.length === 0) {
                html += `
                    <div class="employee-service-card">
                        <h3>${employeeName}</h3>
                        <p class="customer-count">0 customers being served</p>
                        <p style="color: #999; font-style: italic;">No customers in queue</p>
                    </div>
                `;
            } else {
                html += `
                    <div class="employee-service-card">
                        <h3>${employeeName}</h3>
                        <p class="customer-count">${queue.length} customer(s) being served</p>
                        ${queue.map(eq => `
                            <div class="employee-service-customer">
                                <h4>${eq.customer.name}</h4>
                                <p>Email: ${eq.customer.email}</p>
                                <p>Phone: ${eq.customer.phone}</p>
                                <p>Assigned: ${new Date(eq.assignedAt).toLocaleString()}</p>
                                <button onclick="markAsServed(${eq.employeeId}, ${eq.customer.id})" class="served-btn">Served</button>
                            </div>
                        `).join('')}
                    </div>
                `;
            }
        }
        
        container.innerHTML = html;
    } catch (error) {
        console.error('Error loading employee service status:', error);
        document.getElementById('employeeServiceStatus').textContent = 'Unable to load employee queues. Please refresh the page.';
    }
}

function loadAllData() {
    loadStats();
    loadWaitlistQueue();
    loadMainQueue();
    loadEmployees();
    loadEmployeeServiceStatus();
    loadServedCustomers();
}

document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('customerForm').addEventListener('submit', addCustomer);
    document.getElementById('employeeForm').addEventListener('submit', addEmployee);
    loadAllData();
    setInterval(loadAllData, 5000);
});
