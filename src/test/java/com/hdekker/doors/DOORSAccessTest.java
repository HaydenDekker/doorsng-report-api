package com.hdekker.doors;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;


@SpringBootTest
public class DOORSAccessTest {

	@Autowired
	Config config;
	
	WebClient wc;
		
	@BeforeEach
	public void buildWebClient() {
		
		wc = WebClient.builder()
			.exchangeStrategies(
				ExchangeStrategies.builder()
	            .codecs(configurer -> configurer
	                    .defaultCodecs()
	                    .maxInMemorySize(16 * 1024 * 1024))
	                    .build())
			.baseUrl(config.getBaseURL())
			.defaultHeaders(header-> header.setBasicAuth(config.getUserName(), config.getPassWord()))
			.build();
	}
		

	
	Logger log = LoggerFactory.getLogger(DOORSAccessTest.class);
	
	/**
	 * Manually tested in authorised browser.
	 * 
	 * 
	 */
	@Test
	public void buildsSimpleTestURI() {
		
		DefaultUriBuilderFactory b = new DefaultUriBuilderFactory();
		URI path = b.builder()
			.path(Config.uri_Path_Resources)
				.queryParam(Config.attr_ProjectURI, config.getValueRMProject())
				.build(Map.of());
		
		assertThat(path.toString(), equalTo("/rm/publish/resources?" + Config.attr_ProjectURI + "=" + config.getValueRMProject()));
		
	}
	
	/***
	 * DOORS redirect when not authenticated
	 * 
	 * WWW-Authenticate=jauth realm="https://xxxxx.clm.ibmcloud.com/jts/", token_uri="https://xxxxx.clm.ibmcloud.com/jts/jauth-issue-token"
	 * 
	 */
	@Test
	public void canDetectDOORSAccessDOORSFailure() {
		
		String value = wc.get()
			.uri(builder-> builder.path(Config.uri_Path_Resources)
							.queryParam(Config.attr_ProjectURI, config.getValueRMProject())
							.build())
			.retrieve()
			.onStatus((s) -> s.equals(HttpStatus.UNAUTHORIZED), (resp)-> {
				
				List<String> authHeader = resp.headers().header("WWW-Authenticate");
				return Mono.error(new Error("Unauthorised connection. " + authHeader.get(0)));
			})
			.bodyToMono(String.class)
			.onErrorResume(Error.class, (e)-> Mono.just(e.getMessage()))
			.block();
		
		assertThat(value, containsString("clm.ibmcloud.com/jts/jauth-issue-token\""));
		
		log.info("Check for connection returned " + value);
	}
	
	/**
	 * You can go straight to
	 * the auth step and this is not needed
	 * 
	 */
	@Test
	public void canGetAuthForm() {
		
			String value = wc.get()
				.uri(builder-> builder.path(Config.path_Auth)
								.build())
				.retrieve()
					.onStatus((s) -> s.equals(HttpStatus.UNAUTHORIZED), (resp)-> {	
					log.info("UnAuthed hmm. " + resp.headers());
					return Mono.error(new Error());
					})
				 .bodyToMono(String.class)
				 .block();
			
			assertThat(value, not(Matchers.emptyOrNullString()));
	
	}
	
	MultiValueMap<String, ResponseCookie> cookieMap;
	
	@Test
	public void attemptLoginAsForm() {
		
			wc.post()
				.uri(builder-> builder.path(Config.path_Auth_Sec_Check)
								.build())
				.body(BodyInserters.fromFormData(Config.userVal, config.getUserName())
						.with(Config.passVal, config.getPassWord())
				)
				.exchangeToMono(resp->{

					Map<String,List<String>> cookiesAsString = resp.cookies().entrySet()
						.stream()
						.collect(Collectors.toMap(c->c.getKey(), c->c.getValue()
																		.stream()
																		.map(rc-> rc.getValue())
																		.collect(Collectors.toList())));
					
					ObjectMapper om = new ObjectMapper();
					try {
						log.info("Attempted login, header recieved are: " + om.writeValueAsString(resp.headers().asHttpHeaders().toSingleValueMap()));
						log.info("Attempted login, cookies recieved are: " + om.writeValueAsString(cookiesAsString));
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return Mono.just(cookiesAsString);
				}).block();	
	}
	
	@Test
	public void canLoginThenAccessSecureArea() {
		
		LinkedMultiValueMap<String, String> cookies = wc.post()
		.uri(builder-> builder.path(Config.path_Auth_Sec_Check)
						//.queryParam(Config.attr_ProjectURI, Config.value_RMProject)
						.build())
		.body(BodyInserters.fromFormData(Config.userVal, config.getUserName())
				.with(Config.passVal, config.getPassWord())
		)
		.exchangeToMono(resp->{

			Map<String,List<String>> cookiesAsString = resp.cookies().entrySet()
				.stream()
				.collect(Collectors.toMap(c->c.getKey(), c->c.getValue()
																.stream()
																.map(rc-> rc.getValue())
																.collect(Collectors.toList())));
			
			ObjectMapper om = new ObjectMapper();
			try {
				log.info("Attempted login, header recieved are: " + om.writeValueAsString(resp.headers().asHttpHeaders().toSingleValueMap()));
				log.info("Attempted login, cookies recieved are: " + om.writeValueAsString(cookiesAsString));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return Mono.just(new LinkedMultiValueMap<>(cookiesAsString));
		}).block();
		 
			String value = wc.get()
			.uri(builder-> builder.path(Config.uri_Path_Resources)
							.queryParam(Config.attr_ProjectURI, config.getValueRMProject())
							.build())
			.cookies(cook-> cook.addAll(cookies))
			.retrieve()
				.onStatus((s) -> s.equals(HttpStatus.UNAUTHORIZED), (resp)-> {	
				log.info("UnAuthed hmm. " + resp.headers());
				return Mono.error(new Error());
				})
			 .bodyToMono(String.class)
			 .block();
		
		assertThat(value, not(Matchers.emptyOrNullString()));
		
		log.info("That's it.");
	}
	
}
