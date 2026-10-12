package com.vynk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"vynk.otp.pepper=test-explicit-pepper-for-spring-boot-tests-only"
})
class VynkBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
