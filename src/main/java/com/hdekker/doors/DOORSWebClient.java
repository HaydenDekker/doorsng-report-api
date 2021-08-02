package com.hdekker.doors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DOORSWebClient {

	@Autowired
	Config config;
	
	WebClient wc;
	
	public DOORSWebClient(Config config){
		
		this.config = config;
		wc = WebClient.builder()
				.exchangeStrategies(
					ExchangeStrategies.builder()
		            .codecs(configurer -> configurer
		                    .defaultCodecs()
		                    .maxInMemorySize(16 * 1024 * 1024))
		                    .build()
		         )
				.baseUrl(config.getBaseURL())
				.build();
	}
	
	public WebClient getWebClientForBaseURL() {
		
		return wc;
		
	}
	
}
