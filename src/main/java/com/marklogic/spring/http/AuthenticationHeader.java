package com.marklogic.spring.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;

public class AuthenticationHeader {
    private final String type, realm;
    private AuthenticationHeader(String type, String realm) {
        super();
        this.type = type;
        this.realm = realm;
        
    }

    public String getRealm() {
        return realm;
    }

    public String getType() {
        return type;
    }

    public static AuthenticationHeader getOption(RestConfig config) {
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
            
            HttpResponse resp = client.execute(get);
            Header auth = resp.getFirstHeader("WWW-Authenticate");
            
            HeaderElement elem = auth.getElements()[0];
            String type = elem.getName().split(" ")[0];
            String realm = elem.getValue();
            
            AuthenticationHeader result = new AuthenticationHeader(type, realm);
            
            return result;
        } catch (IOException ex) {
            throw new RuntimeException("Unable to reach endpoint, cause: " + ex.getMessage(), ex);
        }
    }
}