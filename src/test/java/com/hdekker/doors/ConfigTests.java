package com.hdekker.doors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;


@SpringBootTest
public class ConfigTests {

	@Autowired
	Config config;
	
	/**
	 * If these fail check app.props
	 * 
	 */
	@Test
	public void testDataPresent() {
		
		assertThat(config.getUserName().length(), greaterThan(1));
		assertThat(config.getPassWord().length(), greaterThan(1));
		assertThat(config.getBaseURL().length(), greaterThan(1));
		assertThat(config.getValueArtifactType().length(), greaterThan(1));
		assertThat(config.getValueRMProject().length(), greaterThan(1));
		
	}
	
}
