package com.alok.aws.iotcore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(args = {
		"--awsAccessKey=some-key",
		"--awsSecretKey=some-secret"
})
class IotCoreApplicationTests {

	@Test
	void contextLoads() {
	}

}
