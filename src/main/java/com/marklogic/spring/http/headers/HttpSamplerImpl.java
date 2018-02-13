package com.marklogic.spring.http.headers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;

import com.marklogic.spring.http.HeaderData;
import com.marklogic.spring.http.HttpSampler;
import com.marklogic.spring.http.RestConfig;

public class HttpSamplerImpl implements HttpSampler{
	private HttpResponse response;

	@Override
	public void extractSample(RestConfig config) {
        URI uri;
        try {
            uri = new URI(config.getScheme(), null, config.getHost(), config.getRestPort(), "/",
                    "", null);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Unable to build URI, cause: " + ex.getMessage(), ex);
        }
        
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpHead get = new HttpHead(uri);
            
            response = client.execute(get);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to reach endpoint, cause: " + ex.getMessage(), ex);
        }
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends HeaderData> T getHeader(Class<T> headerClass) {
		try {
			T instance = headerClass.newInstance();
			return (T) instance.build(response);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to instantiate provided implementation of HeaderData");
		}
	}
}
