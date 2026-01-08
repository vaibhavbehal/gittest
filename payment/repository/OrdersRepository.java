package com.example.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.payment.entity.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders,Integer> {
	Orders findByRazorpayOrderId(String razorpayId);
}
