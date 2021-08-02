package com.hdekker.doors;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;


public interface DOORAuth {
	
	public static Function<WebClient, 
					Function<Tuple2<String,String>, LinkedMultiValueMap<String, String>>> login() {
		
		return (wc) -> (userPass) -> {
			
			LinkedMultiValueMap<String, String> cookies = wc.post()
					.uri(builder-> builder.path(Config.path_Auth_Sec_Check)
									.build())
					.body(BodyInserters.fromFormData(Config.userVal, userPass.getT1())
							.with(Config.passVal, userPass.getT2())
							)
					.exchangeToMono(resp->{
	
						Map<String,List<String>> cookiesAsString = resp.cookies().entrySet()
							.stream()
							.collect(Collectors.toMap(c->c.getKey(), c->c.getValue()
																			.stream()
																			.map(rc-> rc.getValue())
																			.collect(Collectors.toList())));
						
						return Mono.just(new LinkedMultiValueMap<>(cookiesAsString));
					}).block();
			
			return cookies;
		};
		
	}
	
}
