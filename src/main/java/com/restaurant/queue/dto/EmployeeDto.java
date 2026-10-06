package com.restaurant.queue.dto;

public class EmployeeDto {
    private String username;
    private String password;
    private String name;
    private Integer queueLimit;

    public EmployeeDto() {
    }

    public EmployeeDto(String username, String password, String name, Integer queueLimit) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.queueLimit = queueLimit;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQueueLimit() {
        return queueLimit;
    }

    public void setQueueLimit(Integer queueLimit) {
        this.queueLimit = queueLimit;
    }
}
