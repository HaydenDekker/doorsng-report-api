package com.hdekker.doors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

@SpringBootTest
public class DOORSDataExtractTests {

	@Autowired
	Config config;
	
	@Autowired
	DOORSWebClient dwc;
	
	Logger log = LoggerFactory.getLogger(DOORSDataExtractTests.class);
	
	/**
	 * Polling DOORS at 31-07-2021
	 * can see 11011 RSA System Requirement artifacts
	 * 
	 * 
	 */
	@Test
	public void canExtractSystemRequirement() {
		
		WebClient wc = dwc.getWebClientForBaseURL();
		LinkedMultiValueMap<String, String> authCookies = DOORAuth.login()
																	.apply(wc)
																	.apply(Tuples.of(config.getUserName(), config.getPassWord()));
		
		
		String value = wc.get()
			.uri(builder-> builder.path(Config.uri_Path_Resources)
							.queryParam("size", "10")
							.queryParam(Config.attr_ProjectURI, config.getValueRMProject())
							.queryParam(Config.attr_TypeName, config.getValueArtifactType())
							
							.build())
			.cookies(cook-> cook.addAll(authCookies))
			.retrieve()
				.onStatus((s) -> s.equals(HttpStatus.UNAUTHORIZED), (resp)-> {	
				//log.info("UnAuthed hmm. " + resp.headers());
				return Mono.error(new Error());
				})
			 .bodyToMono(String.class)
			 .block();
		
		try {
			FileOutputStream os = new FileOutputStream(config.getTempFolderForRawXML());
			os.write(value.getBytes());
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Document doc = Jsoup.parse(value, "", Parser.xmlParser());
		Elements artifacts = doc.select(DOORSExtractor.artifacts);
		assertThat(Double.valueOf(artifacts.size()), closeTo(10.00, 1.0));

	}
	
	@Test
	public void querySelectorsOnRSASystemsArtifacts() {
		
		File f = new File(config.getTempFolderForRawXML());
		Document doc = null;;
		try {
			doc = Jsoup.parse(f, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Elements artifactsEls = doc.select(DOORSExtractor.artifacts);
		List<Map<String, String>> artifactsList = artifacts.apply(artifactsEls);
		ObjectMapper om = new ObjectMapper();
		try {
			String v = om.writeValueAsString(artifactsList);
			log.info("Extracting artifacts resulted in the following map," + v);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertThat(Double.valueOf(Double.valueOf(artifactsList.size())), closeTo(10.00, 1.0));

	}
	
	Function<Elements, List<Map<String,String>>> artifacts = (els) ->{
		
		return els.stream()
			.map(el->{
			
				Elements customAttrs = el.select(DOORSExtractor.artifactCustomAttributes);
				return customAttrs.stream()
							.map(DOORSExtractor.customAttrToKeyVal)
							.collect(Collectors.toMap(t-> t.getT1(), t->t.getT2(), (prev,nxt)-> prev + "," + nxt));
			})
			.collect(Collectors.toList());
		
	};
	
	// mock xml.
	String testAttrInt = "<attribute:customAttribute attribute:datatype=\"http://www.w3.org/2001/XMLSchema#int\" attribute:isEnumeration=\"true\" attribute:isMultiValued=\"true\" attribute:itemId=\"_Q8LyYYuuEeiC2dwB1IQqlw\" attribute:literalId=\"_KJYB0YuuEeiC2dwB1IQqlw#3c19dddf-177a-46d3-8a36-1b1e501409fd\" attribute:literalName=\"Below\" attribute:name=\"VV Method\" attribute:value=\"0\"/>";
	String testAttrString = "<attribute:customAttribute attribute:datatype=\"http://www.w3.org/2001/XMLSchema#string\" attribute:isMultiValued=\"false\" attribute:itemId=\"_K7oVVQcAEeiZZ4s9Kw5xFw\" attribute:name=\"Updated On\" attribute:value=\"2020-06-04 12:12:12\" attribute:valueTS=\"\"/>";
	/**
	 * Seems like two schema used for attrs int and string
	 * need to pass customAttr and return pair of strings representing
	 * name, value
	 */
	@Test
	public void detectAttrSchemaInt() {
		
		Function<Element, Tuple2<String, String>> fn = DOORSExtractor.customAttrToKeyVal;
		Elements el = Jsoup.parse(testAttrInt).select("attribute|customattribute");
		Tuple2<String, String> out = fn.apply(el.first());
		assertThat(out.getT1(), equalTo("VV Method"));
		assertThat(out.getT2(), equalTo("Below"));
		
	}
	
	String intNonEnum = "<attribute:customAttribute attribute:itemId=\"_ro9VYci-EeeN5bwTRxIMGg\" attribute:name=\"Identifier\" attribute:value=\"123ABC\" attribute:isMultiValued=\"false\" attribute:datatype=\"http://www.w3.org/2001/XMLSchema#int\" attribute:valueTS=\"\"/>";
	
	@Test
	public void detectAttrSchemaIntForNonEnumeration() {
		
		Function<Element, Tuple2<String, String>> fn = DOORSExtractor.customAttrToKeyVal;
		Elements el = Jsoup.parse(intNonEnum).select("attribute|customattribute");
		Tuple2<String, String> out = fn.apply(el.first());
		assertThat(out.getT1(), equalTo("Identifier"));
		assertThat(out.getT2(), equalTo("123ABC"));
		
	}
	
	@Test
	public void detectAttrSchemaString() {
		
		Function<Element, Tuple2<String, String>> fn = DOORSExtractor.customAttrToKeyVal;
		Elements el = Jsoup.parse(testAttrString).select("attribute|customattribute");;
		Tuple2<String, String> out = fn.apply(el.first());
		
		assertThat(out.getT1(), equalTo("Updated On"));
		assertThat(out.getT2(), equalTo("2020-06-04 12:12:12"));
		
	}
	
	@Test
	public void pagesThroughWriteToFile() {
		
		
		
	}
	
}
