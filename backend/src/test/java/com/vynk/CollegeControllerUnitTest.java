package com.vynk;

import com.vynk.college.controller.CollegeController;
import com.vynk.college.entity.College;
import com.vynk.college.service.CollegeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CollegeControllerUnitTest {

    private MockMvc mockMvc;
    private CollegeService collegeService;

    @BeforeEach
    void setUp() {
        collegeService = Mockito.mock(CollegeService.class);
        CollegeController controller = new CollegeController(collegeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnActiveColleges() throws Exception {
        College college = new College();
        college.setName("Anurag University");
        college.setEmailDomain("anurag.edu.in");
        college.setActive(true);

        when(collegeService.getActiveColleges()).thenReturn(List.of(college));

        mockMvc.perform(get("/api/colleges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Anurag University"))
                .andExpect(jsonPath("$[0].emailDomain").value("anurag.edu.in"));
    }
}
