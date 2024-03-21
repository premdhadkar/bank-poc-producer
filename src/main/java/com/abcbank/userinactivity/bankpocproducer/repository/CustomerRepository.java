package com.abcbank.userinactivity.bankpocproducer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abcbank.userinactivity.bankpocproducer.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

}
