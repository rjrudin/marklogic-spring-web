package com.marklogic.spring.http.headers;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;

import com.marklogic.spring.http.HeaderData;

public class WwwAuthenticateHeader implements HeaderData{
	private static final String NAME = "WWW-Authenticate";
	private String raw, realm, value;
	
	public WwwAuthenticateHeader() {
	}
	
	private WwwAuthenticateHeader(Header header){
        HeaderElement elem = header.getElements()[0];
        
        raw = header.toString();
        value = elem.getName().split(" ")[0];
        realm = elem.getValue();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String getRaw() {
		return raw;
	}

	public String getRealm() {
		return realm;
	}

	@Override
	public WwwAuthenticateHeader build(HttpResponse response) {
		Header header = response.getFirstHeader(NAME);
		return new WwwAuthenticateHeader(header);
	}
}
