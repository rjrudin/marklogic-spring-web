package com.marklogic.spring.http.proxy;

import com.marklogic.appdeployer.AppConfig;
import com.marklogic.appdeployer.command.restapis.DeployRestApiServersCommand;
import com.marklogic.appdeployer.impl.SimpleAppDeployer;
import com.marklogic.client.ext.DatabaseClientConfig;
import com.marklogic.mgmt.ManageClient;
import com.marklogic.mgmt.ManageConfig;
import com.marklogic.mgmt.admin.AdminConfig;
import com.marklogic.mgmt.admin.AdminManager;
import com.marklogic.mgmt.resource.appservers.ServerManager;
import com.marklogic.spring.http.RestConfig;
import com.marklogic.spring.http.SimpleRestConfig;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class HttpProxyTest extends Assert {

	@Autowired
	private DatabaseClientConfig databaseClientConfig;

	private ManageClient manageClient;
	private AppConfig appConfig;
	private SimpleAppDeployer deployer;
	private SimpleRestConfig restConfig;

	@Before
	public void setupTemporaryRestServer() {
		manageClient = new ManageClient(new ManageConfig(
			databaseClientConfig.getHost(),
			8002,
			databaseClientConfig.getUsername(),
			databaseClientConfig.getPassword()
		));

		AdminConfig adminConfig = new AdminConfig(
			databaseClientConfig.getHost(),
			8001,
			databaseClientConfig.getUsername(),
			databaseClientConfig.getPassword()
		);

		appConfig = new AppConfig();
		appConfig.setContentForestsPerHost(1);
		appConfig.setName("marklogic-spring-web-test");
		appConfig.setRestPort(databaseClientConfig.getPort());

		AdminManager adminManager = new AdminManager(adminConfig);

		deployer = new SimpleAppDeployer(manageClient, adminManager,
			new DeployRestApiServersCommand(true));
		deployer.deploy(appConfig);

		setServerToDigestAuthentication();

		restConfig = new SimpleRestConfig(
			databaseClientConfig.getHost(),
			databaseClientConfig.getPort()
		);
	}

	@After
	public void teardown() {
		deployer.undeploy(appConfig);
	}

	/**
	 * Haven't thought of a way to verify that digest caching is occurring, so this test relies on you manually
	 * checking the ML AccessLog for the REST server.
	 * <p>
	 * When digest caching is enabled, you're looking for log entries like this:
	 * <p>
	 * 127.0.0.1 - - [08/Mar/2018:16:33:01 -0500] "GET / HTTP/1.1" 401 104 - "Apache-HttpClient/4.5.2 (Java/1.8.0_131)"
	 * 127.0.0.1 - admin [08/Mar/2018:16:33:02 -0500] "GET / HTTP/1.1" 200 2103 - "Apache-HttpClient/4.5.2 (Java/1.8.0_131)"
	 * 127.0.0.1 - admin [08/Mar/2018:16:33:02 -0500] "GET / HTTP/1.1" 200 2103 - "Apache-HttpClient/4.5.2 (Java/1.8.0_131)"
	 * <p>
	 * And when it's not enabled, you're looking for log entries like this:
	 * <p>
	 * 127.0.0.1 - - [08/Mar/2018:16:33:02 -0500] "GET / HTTP/1.1" 401 104 - "Apache-HttpClient/4.5.2 (Java/1.8.0_131)"
	 * 127.0.0.1 - admin [08/Mar/2018:16:33:02 -0500] "GET / HTTP/1.1" 200 2103 - "Apache-HttpClient/4.5.2 (Java/1.8.0_131)"
	 * 127.0.0.1 - - [08/Mar/2018:16:33:02 -0500] "GET / HTTP/1.1" 401 104 - "Apache-HttpClient/4.5.2 (Java/1.8.0_131)"
	 * 127.0.0.1 - admin [08/Mar/2018:16:33:02 -0500] "GET / HTTP/1.1" 200 2103 - "Apache-HttpClient/4.5.2 (Java/1.8.0_131)"
	 */
	@Test
	public void test() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();

		restConfig.setDigestCachingEnabled(true);
		HttpProxy proxy = newHttpProxy(restConfig);

		proxy.proxy(request, response);
		proxy.proxy(request, response);

		restConfig.setDigestCachingEnabled(false);
		proxy = newHttpProxy(restConfig);

		proxy.proxy(request, response);
		proxy.proxy(request, response);
	}

	protected void setServerToDigestAuthentication() {
		String json = String.format("{\"server-name\":\"%s\", \"authentication\":\"%s\"}",
			appConfig.getRestServerName(),
			"digest");
		new ServerManager(manageClient).save(json);
	}

	protected HttpProxy newHttpProxy(RestConfig restConfig) {
		return new HttpProxy(restConfig, new CredentialsProvider() {
			@Override
			public void setCredentials(AuthScope authscope, Credentials credentials) {
			}

			@Override
			public Credentials getCredentials(AuthScope authscope) {
				return new UsernamePasswordCredentials(databaseClientConfig.getUsername(), databaseClientConfig.getPassword());
			}

			@Override
			public void clear() {
			}
		});
	}
}

@Configuration
@PropertySource("file:gradle.properties")
class TestConfig {

	/**
	 * Ensures that placeholders are replaced with property values
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlaceHolderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public DatabaseClientConfig databaseClientConfig(
		@Value("${mlHost}") String host,
		@Value("${mlRestPort}") int port,
		@Value("${mlUsername}") String username,
		@Value("${mlPassword}") String password
	) {
		return new DatabaseClientConfig(host, port, username, password);
	}
}

