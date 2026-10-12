package com.vynk.college.controller;

import com.vynk.college.dto.CollegeResponse;
import com.vynk.college.service.CollegeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/colleges")
public class CollegeController {

    private final CollegeService collegeService;

    public CollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @GetMapping
    public List<CollegeResponse> getAllColleges() {
        return collegeService.getActiveColleges().stream()
                .map(college -> new CollegeResponse(
                        college.getId(),
                        college.getName(),
                        college.getEmailDomain(),
                        college.isActive()
                ))
                .toList();
    }
}