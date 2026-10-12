package com.vynk.college.repository;

import com.vynk.college.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollegeRepository extends JpaRepository<College, Long> {

    List<College> findByActiveTrue();

    boolean existsByEmailDomain(String emailDomain);
}