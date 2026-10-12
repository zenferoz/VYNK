package com.vynk.college.service;

import com.vynk.college.entity.College;
import com.vynk.college.repository.CollegeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CollegeService {

    private final CollegeRepository collegeRepository;

    public CollegeService(CollegeRepository collegeRepository) {
        this.collegeRepository = collegeRepository;
    }

    public College createCollege(College college) {
        return collegeRepository.save(college);
    }

    public List<College> getAllColleges() {
        return collegeRepository.findAll();
    }

    public List<College> getActiveColleges() {
        return collegeRepository.findByActiveTrue();
    }

    public Optional<College> getCollegeById(Long id) {
        return collegeRepository.findById(id);
    }
}