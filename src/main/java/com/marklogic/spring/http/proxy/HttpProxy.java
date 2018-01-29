package com.marklogic.spring.http.proxy;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.marklogic.spring.http.RestClient;
import com.marklogic.spring.http.RestConfig;
import com.marklogic.spring.security.authentication.MarkLogicUsernamePasswordAuthentication;

/**
 * Simple proxy class that uses Spring's RestOperations to proxy servlet requests to MarkLogic.
 */
public class HttpProxy extends RestClient {
    public static final String RESTTEMPLATE_KEY = "com.marklogic.spring.http.proxy.RestTemplate";

    public HttpProxy(RestConfig restConfig, CredentialsProvider provider) {
        super(restConfig, provider);
    }

    public HttpProxy(RestConfig restConfig, RestOperations restOperations) {
        super(restConfig, restOperations);
    }

    protected RestTemplate newRestTemplate(CredentialsProvider provider) {
        HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));
    }

    /**
     * Proxy a request without copying any headers.
     * 
     * @param httpRequest
     * @param httpResponse
     */
    public void proxy(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        proxy(httpRequest.getServletPath(), httpRequest, httpResponse);
    }

    /**
     * Proxy a request and copy the given headers on both the request and the response.
     * 
     * @param httpRequest
     * @param httpResponse
     * @param headerNamesToCopy
     */
    public void proxy(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String... headerNamesToCopy) {
        proxy(httpRequest.getServletPath(), httpRequest, httpResponse,
                new DefaultRequestCallback(httpRequest, headerNamesToCopy),
                new DefaultResponseExtractor(httpResponse, headerNamesToCopy));
    }

    /**
     * Proxy a request, using the given path instead of the servlet path in the HttpServletRequest.
     * 
     * @param path
     * @param httpRequest
     * @param httpResponse
     * @param headerNamesToCopy
     */
    public void proxy(String path, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            String... headerNamesToCopy) {
        proxy(path, httpRequest, httpResponse, new DefaultRequestCallback(httpRequest, headerNamesToCopy),
                new DefaultResponseExtractor(httpResponse, headerNamesToCopy));
    }

    /**
     * Specify your own request callback and response extractor. This gives you the most flexibility, but does the least
     * for you.
     * 
     * @param path
     * @param httpRequest
     * @param httpResponse
     * @param requestCallback
     * @param responseExtractor
     * @return
     */
    public <T> T proxy(String path, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestCallback requestCallback, ResponseExtractor<T> responseExtractor) {
        URI uri = buildUri(path, httpRequest.getQueryString());
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Proxying to URI: %s", uri));
        }
        HttpMethod method = determineMethod(httpRequest);
        return getRestOperations().execute(uri, method, requestCallback, responseExtractor);
    }

    protected HttpMethod determineMethod(HttpServletRequest request) {
        return HttpMethod.valueOf(request.getMethod());
    }
    
    @Override
    public RestOperations getRestOperations() {
        // credentials provider....
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new CredentialsExpiredException("Missing security context.");
        }
        
        if (!(auth instanceof MarkLogicUsernamePasswordAuthentication)) {
            return super.getRestOperations();
        }
        
        MarkLogicUsernamePasswordAuthentication token = (MarkLogicUsernamePasswordAuthentication) auth;
        RestOperations template = token.getOperations();
        if (template == null) {
            throw new SessionAuthenticationException("User not logged in.");
        }
        return template;
    }
}
