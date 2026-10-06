package com.restaurant.queue.repository;

import com.restaurant.queue.model.EmployeeQueue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeQueueRepository extends JpaRepository<EmployeeQueue, Long> {

    @EntityGraph(attributePaths = {"employee", "customer"})
    @Query("SELECT eq FROM EmployeeQueue eq WHERE eq.employee.id = :employeeId ORDER BY eq.position ASC")
    List<EmployeeQueue> findByEmployeeIdOrderByPositionAsc(@Param("employeeId") Long employeeId);

    @Query("SELECT eq FROM EmployeeQueue eq WHERE eq.employee.id = :employeeId AND eq.customer.id = :customerId")
    Optional<EmployeeQueue> findByEmployeeIdAndCustomerId(@Param("employeeId") Long employeeId, @Param("customerId") Long customerId);

    @Query("SELECT COUNT(eq) FROM EmployeeQueue eq WHERE eq.employee.id = :employeeId")
    long countByEmployeeId(@Param("employeeId") Long employeeId);
}
