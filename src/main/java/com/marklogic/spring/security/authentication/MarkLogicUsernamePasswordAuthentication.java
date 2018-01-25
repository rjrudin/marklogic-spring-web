package com.marklogic.spring.security.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.client.RestOperations;

import java.util.Collection;

/**
 * Doesn't erase credentials.. When upgrading Spring Boot in slush-marklogic-spring-boot from 1.3.5 to 1.4.3, some change
 * resulted in the call to AuthenticationManagerBuilder.eraseCredentials(false) to not work any longer. So this class is
 * used to prevent any erasing from occurring.
 */
public class MarkLogicUsernamePasswordAuthentication extends UsernamePasswordAuthenticationToken {
    private static final long serialVersionUID = 1L;
    private RestOperations operations;
    
    public MarkLogicUsernamePasswordAuthentication(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public MarkLogicUsernamePasswordAuthentication(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
    
    public MarkLogicUsernamePasswordAuthentication(Object principal, Object credentials, RestOperations operations) {
        super(principal, credentials);
        this.operations = operations;
    }
    
    public MarkLogicUsernamePasswordAuthentication(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities, RestOperations operations) {
        super(principal, credentials, authorities);
        this.operations = operations;
    }

    @Override
    public void eraseCredentials() {
    }

    public RestOperations getOperations() {
        return operations;
    }
    
    public void clearOperations() {
        this.operations = null;
    }
}
