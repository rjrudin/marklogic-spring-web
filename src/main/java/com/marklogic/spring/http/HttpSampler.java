package com.marklogic.spring.http;

public interface HttpSampler {
	void extractSample(RestConfig config);
	<T extends HeaderData> T getHeader(Class<T> headerClass);
}
