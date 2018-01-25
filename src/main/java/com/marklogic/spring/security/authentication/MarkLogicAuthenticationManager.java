package com.marklogic.spring.security.authentication;

import com.marklogic.spring.http.RestClient;
import com.marklogic.spring.http.RestConfig;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Implements Spring Security's AuthenticationManager interface so that it can authenticate users by making a simple
 * request to MarkLogic and checking for a 401. Also implements AuthenticationProvider so that it can be used with
 * Spring Security's ProviderManager.
 */
public class MarkLogicAuthenticationManager implements AuthenticationProvider, AuthenticationManager {

	private RestConfig restConfig;

	private String pathToAuthenticateAgainst = "/";

	/**
	 * A RestConfig instance is needed so a request can be made to MarkLogic to see if the user can successfully
	 * authenticate.
	 *
	 * @param restConfig
	 */
	public MarkLogicAuthenticationManager(RestConfig restConfig) {
		this.restConfig = restConfig;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
			throw new IllegalArgumentException(
				getClass().getName() + " only supports " + UsernamePasswordAuthenticationToken.class.getName());
		}

		UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
		String username = token.getPrincipal().toString();
		String password = token.getCredentials().toString();

		/**
		 * For now, building a new RestTemplate each time. This should in general be okay, because we're typically not
		 * authenticating users over and over.
		 */
		RestClient client = new RestClient(restConfig
				, prepareRestTemplate(new SimpleCredentialsProvider(username, password))
			);
		URI uri = client.buildUri(pathToAuthenticateAgainst, "");
		try {
			client.getRestOperations().getForEntity(uri, String.class);
		} catch (HttpClientErrorException ex) {
			if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
				// Authenticated, but the path wasn't found - that's okay, we just needed to verify authentication
			} else if (HttpStatus.UNAUTHORIZED.equals(ex.getStatusCode())) {
				throw new BadCredentialsException("Invalid credentials");
			} else {
				throw ex;
			}
		}

		return buildAuthenticationToReturn(token, client.getRestOperations());
	}

	/**
	 * See the comments on MarkLogicUsernamePasswordAuthentication to understand why an instance of that class is
	 * returned.
	 *
	 * @param token
	 * @return
	 */
	protected Authentication buildAuthenticationToReturn(UsernamePasswordAuthenticationToken token, RestOperations operation) {
		return new MarkLogicUsernamePasswordAuthentication(token.getPrincipal(), token.getCredentials(),
			token.getAuthorities(), operation);
	}

	public void setPathToAuthenticateAgainst(String pathToAuthenticateAgainst) {
		this.pathToAuthenticateAgainst = pathToAuthenticateAgainst;
	}
	
	private RestTemplate prepareRestTemplate(SimpleCredentialsProvider provider) {
		final CloseableHttpClient httpClient =
				HttpClientBuilder
					.create()
					.setDefaultCredentialsProvider(provider)
					.useSystemProperties()
					.build();
		final HttpHost host = new HttpHost(restConfig.getHost(), restConfig.getRestPort(), restConfig.getScheme());
		// Create AuthCache instance
		final AuthCache authCache = new BasicAuthCache();
		// Generate DIGEST scheme object, initialize it and add it to the local digest cache
		final DigestScheme digestAuth = new DigestScheme();
		digestAuth.overrideParamter("realm", restConfig.getRealm());
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

/**
 * Simple implementation that is good for one-time requests.
 */
class SimpleCredentialsProvider implements CredentialsProvider {

	private String username;
	private String password;

	public SimpleCredentialsProvider(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public void setCredentials(AuthScope authscope, Credentials credentials) {
	}

	@Override
	public Credentials getCredentials(AuthScope authscope) {
		return new UsernamePasswordCredentials(username, password);
	}

	@Override
	public void clear() {
	}

}