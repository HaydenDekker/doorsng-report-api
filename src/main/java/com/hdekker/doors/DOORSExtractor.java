package com.hdekker.doors;

import java.util.function.Function;

import org.jsoup.nodes.Element;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public interface DOORSExtractor {

	public final String artifacts = "ds|artifact";
	// public final String artifactIds = "ds|artifact > rrm|identifier";
	public final String artifactCustomAttributes = "ds|artifact attribute|customAttribute";
	
	public final String attr_DATATYPE = "attribute:datatype";
	public static final String attr_Value = "attribute:value";
	public static final String attr_Name = "attribute:name";
	public static final String attr_LitName = "attribute:literalname";
	public static final String attr_IsEnum = "attribute:isEnumeration";
	
	/**
	 * if datatype int return val as literalname
	 * if datatype string return val as val
	 */
	public static Function<Element, Tuple2<String,String>> customAttrToKeyVal = (el) -> {
		
		return (el.attr(attr_DATATYPE)
			.contains("Schema#int") && el.attr(attr_IsEnum).equals("true"))? Tuples.of(el.attr(attr_Name), el.attr(attr_LitName)) :
										Tuples.of(el.attr(attr_Name), el.attr(attr_Value));
		
	};
}
