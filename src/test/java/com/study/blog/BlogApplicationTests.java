package com.study.blog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.flyway.enabled=false")
class BlogApplicationTests {

	@Test
	void contextLoads() {
	}

}
