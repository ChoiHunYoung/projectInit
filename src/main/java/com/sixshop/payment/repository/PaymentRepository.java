package com.sixshop.payment.repository;

import com.sixshop.payment.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
