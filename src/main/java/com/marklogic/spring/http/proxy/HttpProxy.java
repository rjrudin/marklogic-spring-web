package com.marklogic.spring.http.proxy;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.marklogic.spring.http.HttpSampler;
import com.marklogic.spring.http.RestClient;
import com.marklogic.spring.http.RestConfig;
import com.marklogic.spring.http.headers.HttpSamplerImpl;
import com.marklogic.spring.http.headers.WwwAuthenticateHeader;

/**
 * Simple proxy class that uses Spring's RestOperations to proxy servlet requests to MarkLogic.
 */
public class HttpProxy extends RestClient {

	public HttpProxy(RestConfig restConfig, CredentialsProvider provider) {
        super(restConfig, provider);
    }

    public HttpProxy(RestConfig restConfig, RestOperations restOperations) {
        super(restConfig, restOperations);
    }

    @Override
    protected RestTemplate newRestTemplate(CredentialsProvider provider) {
        HttpSampler sampler = new HttpSamplerImpl();
        sampler.extractSample(getRestConfig());
        WwwAuthenticateHeader header = sampler.getHeader(WwwAuthenticateHeader.class);
        
        if (!"digest".equalsIgnoreCase(header.getValue()) || !getRestConfig().isDigestCachingEnabled()) {
            return super.newRestTemplate(provider);
        }
        return prepareDigestTemplate(provider, header.getRealm());
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
    
    protected RestTemplate prepareDigestTemplate(CredentialsProvider provider, String realm) {
        final HttpClient httpClient = newHttpClient(provider);
        final HttpHost host = new HttpHost(getRestConfig().getHost(), getRestConfig().getRestPort(), getRestConfig().getScheme());
        
        // Create AuthCache instance
        final AuthCache authCache = new BasicAuthCache();
        
        final DigestScheme digestAuth = new DigestScheme();
        digestAuth.overrideParamter("realm", realm);
        authCache.put(host, digestAuth);
        
        // create a RestTemplate wired with a custom request factory using the above AuthCache with Digest Scheme
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient){
            @Override
            protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
                // Add AuthCache to the execution context
                BasicHttpContext localcontext = new BasicHttpContext();
                localcontext.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
                return localcontext;
            }
        });

        return restTemplate;
    }
}
