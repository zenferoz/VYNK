package com.vynk.college.config;

import com.vynk.college.entity.College;
import com.vynk.college.repository.CollegeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CollegeDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CollegeDataInitializer.class);

    private final CollegeRepository collegeRepository;

    public CollegeDataInitializer(CollegeRepository collegeRepository) {
        this.collegeRepository = collegeRepository;
    }

    @Override
    public void run(String... args) {
        seedCollegeIfMissing("Anurag University", "anurag.edu.in");
        seedCollegeIfMissing("SRM Institute of Science and Technology", "srmist.edu.in");
        seedCollegeIfMissing("Vellore Institute of Technology", "vitstudent.ac.in");
        seedCollegeIfMissing("Example University", "example.edu");
    }

    private void seedCollegeIfMissing(String name, String domain) {
        if (!collegeRepository.existsByEmailDomain(domain)) {
            College college = new College();
            college.setName(name);
            college.setEmailDomain(domain);
            college.setActive(true);
            collegeRepository.save(college);
            log.info("Initialized default college: {} ({})", name, domain);
        }
    }
}
