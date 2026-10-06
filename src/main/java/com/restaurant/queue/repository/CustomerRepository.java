package com.restaurant.queue.repository;

import com.restaurant.queue.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE c.currentQueue = :queueType ORDER BY c.joinedAt")
    List<Customer> findByCurrentQueueOrderByJoinedAt(@Param("queueType") Customer.QueueType queueType);

    List<Customer> findByCurrentQueueAndPositionInQueueOrderByJoinedAt(Customer.QueueType queueType, Integer position);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.currentQueue = :queueType")
    long countByCurrentQueue(@Param("queueType") Customer.QueueType queueType);
}
