package com.marklogic.spring.http;

import org.apache.http.HttpResponse;

public interface HeaderData {
	HeaderData build(HttpResponse response);
	
	String getName();
	String getValue();
	String getRaw();
}
